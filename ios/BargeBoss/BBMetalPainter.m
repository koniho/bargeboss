#import "BBMetalPainter.h"
#import "BBPainter.h"
#import "IOSPrimitiveArray.h"
#import "IOSObjectArray.h"
#import "com/bargeboss/game/SpriteAtlas.h"
#import "com/bargeboss/game/StaticLayer.h"
#import "com/bargeboss/game/WaterFoam.h"
#import <simd/simd.h>

typedef struct { vector_float2 position,uv; vector_float4 color; uint32_t kind; float phase; } BBVertex;
@interface BBMetalBatch : NSObject
@property(nonatomic,strong) id<MTLTexture> texture;
@property(nonatomic) NSUInteger start,count,mode;
@end
@implementation BBMetalBatch
@end

@interface BBMetalPainter () {
    BBVertex *_vertices;
    NSUInteger _count,_capacity,_slot;
    id<MTLBuffer> _buffers[3];
    dispatch_semaphore_t _available;
}
@property(nonatomic,strong) id<MTLDevice> device;
@property(nonatomic,strong) id<MTLCommandQueue> queue;
@property(nonatomic,strong) id<MTLRenderPipelineState> pipeline,maskPipeline;
@property(nonatomic,strong) id<MTLDepthStencilState> normalStencil,pushStencil,popStencil;
@property(nonatomic,strong) id<MTLTexture> white,stencil;
@property(nonatomic,strong) id<CAMetalDrawable> drawable;
@property(nonatomic,strong) BBNativePainter *resources,*preparationResources;
@property(nonatomic,strong) NSMutableSet<NSString *> *pending;
@property(nonatomic,strong) dispatch_queue_t preparationQueue;
@property(nonatomic) NSUInteger directLayerFallbacks;
@property(nonatomic) BOOL waterShaders;
@property(nonatomic,strong) NSMutableDictionary<NSString *,id<MTLTexture>> *textures;
@property(nonatomic,strong) NSMutableArray<NSString *> *textureOrder;
@property(nonatomic,strong) NSMutableArray<BBMetalBatch *> *batches;
@property(nonatomic,strong) NSMutableArray<NSData *> *clips;
@property(nonatomic) NSUInteger textureBytes;
@property(nonatomic) NSUInteger textureBudget,textureUploads,textureEvictions,frameTextureBytes,peakFrameTextureBytes;
@property(nonatomic,strong) NSMutableSet<NSString *> *frameTextureKeys;
@property(nonatomic) vector_float4 transform;
@property(nonatomic) CGRect visibleBounds;
@property(nonatomic) MTLScissorRect scissor;
@property(atomic) double gpuMilliseconds;
@end

@implementation BBMetalPainter
- (instancetype)init {
    if((self=[super init])) {
        self.device=MTLCreateSystemDefaultDevice();
        if(!self.device) return nil;
        self.queue=[self.device newCommandQueue];
        self.waterShaders=![NSProcessInfo.processInfo.arguments containsObject:@"--ios-water-textures"];
        self.resources=[[BBNativePainter alloc] init];
        self.preparationResources=[[BBNativePainter alloc] init];
        self.preparationQueue=dispatch_queue_create("com.bargeboss.metal.prepare",dispatch_queue_attr_make_with_qos_class(DISPATCH_QUEUE_SERIAL,QOS_CLASS_USER_INITIATED,0));
        self.pending=[NSMutableSet set];
        self.textures=[NSMutableDictionary dictionary];self.textureOrder=[NSMutableArray array];
        self.textureBudget=160*1024*1024;self.frameTextureKeys=[NSMutableSet set];
        for(NSString *arg in NSProcessInfo.processInfo.arguments) if([arg hasPrefix:@"--ios-texture-mib="])
            self.textureBudget=MAX(16,MIN(192,[[arg substringFromIndex:18] integerValue]))*1024*1024;
        self.batches=[NSMutableArray array];self.clips=[NSMutableArray array];
        _available=dispatch_semaphore_create(3);_slot=2;
        NSError *error=nil;
        id<MTLLibrary> library=[self.device newDefaultLibrary];
        if(!library) [NSException raise:@"MetalLibrary" format:@"%@",error];
        MTLRenderPipelineDescriptor *descriptor=[MTLRenderPipelineDescriptor new];
        descriptor.vertexFunction=[library newFunctionWithName:@"bbVertex"];
        descriptor.fragmentFunction=[library newFunctionWithName:@"bbFragment"];
        descriptor.colorAttachments[0].pixelFormat=MTLPixelFormatBGRA8Unorm;
        descriptor.stencilAttachmentPixelFormat=MTLPixelFormatStencil8;
        descriptor.colorAttachments[0].blendingEnabled=YES;
        descriptor.colorAttachments[0].sourceRGBBlendFactor=MTLBlendFactorOne;
        descriptor.colorAttachments[0].sourceAlphaBlendFactor=MTLBlendFactorOne;
        descriptor.colorAttachments[0].destinationRGBBlendFactor=MTLBlendFactorOneMinusSourceAlpha;
        descriptor.colorAttachments[0].destinationAlphaBlendFactor=MTLBlendFactorOneMinusSourceAlpha;
        self.pipeline=[self.device newRenderPipelineStateWithDescriptor:descriptor error:&error];
        descriptor.colorAttachments[0].writeMask=MTLColorWriteMaskNone;
        self.maskPipeline=[self.device newRenderPipelineStateWithDescriptor:descriptor error:&error];
        if(!self.pipeline || !self.maskPipeline) [NSException raise:@"MetalPipeline" format:@"%@",error];
        self.normalStencil=[self stencilState:MTLStencilOperationKeep compare:MTLCompareFunctionEqual];
        self.pushStencil=[self stencilState:MTLStencilOperationIncrementClamp compare:MTLCompareFunctionAlways];
        self.popStencil=[self stencilState:MTLStencilOperationDecrementClamp compare:MTLCompareFunctionAlways];
        MTLTextureDescriptor *white=[MTLTextureDescriptor texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm width:1 height:1 mipmapped:NO];
        self.white=[self.device newTextureWithDescriptor:white];uint32_t pixel=0xffffffff;
        [self.white replaceRegion:MTLRegionMake2D(0,0,1,1) mipmapLevel:0 withBytes:&pixel bytesPerRow:4];
    }
    return self;
}
- (void)dealloc {free(_vertices);}
- (id<MTLDepthStencilState>)stencilState:(MTLStencilOperation)operation compare:(MTLCompareFunction)compare {
    MTLStencilDescriptor *stencil=[MTLStencilDescriptor new];stencil.stencilCompareFunction=compare;
    stencil.depthStencilPassOperation=operation;stencil.readMask=255;stencil.writeMask=operation==MTLStencilOperationKeep?0:255;
    MTLDepthStencilDescriptor *descriptor=[MTLDepthStencilDescriptor new];descriptor.frontFaceStencil=stencil;descriptor.backFaceStencil=stencil;
    return [self.device newDepthStencilStateWithDescriptor:descriptor];
}
- (BOOL)startLayer:(CAMetalLayer *)layer scale:(float)scale offset:(CGPoint)offset bounds:(CGSize)bounds {
    if(dispatch_semaphore_wait(_available,DISPATCH_TIME_NOW)) return NO;
    self.drawable=[layer nextDrawable];
    if(!self.drawable) {dispatch_semaphore_signal(_available);return NO;}
    _slot=(_slot+1)%3;_count=0;[self.batches removeAllObjects];[self.clips removeAllObjects];
    NSUInteger width=self.drawable.texture.width,height=self.drawable.texture.height;
    if(!self.stencil || self.stencil.width!=width || self.stencil.height!=height) {
        MTLTextureDescriptor *d=[MTLTextureDescriptor texture2DDescriptorWithPixelFormat:MTLPixelFormatStencil8 width:width height:height mipmapped:NO];
        d.storageMode=MTLStorageModePrivate;d.usage=MTLTextureUsageRenderTarget;
        self.stencil=[self.device newTextureWithDescriptor:d];
    }
    self.transform=(vector_float4){2*scale/width,-2*scale/height,2*offset.x/width-1,1-2*offset.y/height};
    self.visibleBounds=CGRectMake(0,0,bounds.width,bounds.height);
    NSUInteger x=MIN(width,(NSUInteger)MAX(0,ceil(offset.x))),y=MIN(height,(NSUInteger)MAX(0,ceil(offset.y)));
    self.scissor=(MTLScissorRect){x,y,MIN(width-x,(NSUInteger)MAX(0,floor(bounds.width*scale))),MIN(height-y,(NSUInteger)MAX(0,floor(bounds.height*scale)))};
    return YES;
}
- (void)present {
    NSUInteger bytes=_count*sizeof(BBVertex);
    if(!_buffers[_slot] || _buffers[_slot].length<bytes) _buffers[_slot]=[self.device newBufferWithLength:MAX(bytes,256*1024) options:MTLResourceStorageModeShared];
    memcpy(_buffers[_slot].contents,_vertices,bytes);
    id<MTLCommandBuffer> command=[self.queue commandBuffer];
    MTLRenderPassDescriptor *pass=[MTLRenderPassDescriptor renderPassDescriptor];
    pass.colorAttachments[0].texture=self.drawable.texture;pass.colorAttachments[0].loadAction=MTLLoadActionClear;
    pass.colorAttachments[0].storeAction=MTLStoreActionStore;pass.colorAttachments[0].clearColor=MTLClearColorMake(16/255.0,30/255.0,37/255.0,1);
    pass.stencilAttachment.texture=self.stencil;pass.stencilAttachment.loadAction=MTLLoadActionClear;pass.stencilAttachment.storeAction=MTLStoreActionDontCare;
    id<MTLRenderCommandEncoder> encoder=[command renderCommandEncoderWithDescriptor:pass];
    [encoder setVertexBuffer:_buffers[_slot] offset:0 atIndex:0];vector_float4 transform=self.transform;
    [encoder setVertexBytes:&transform length:sizeof(transform) atIndex:1];
    [encoder setStencilReferenceValue:0];[encoder setScissorRect:self.scissor];
    NSUInteger mode=NSUIntegerMax;id<MTLTexture> texture=nil;
    for(BBMetalBatch *batch in self.batches) {
        if(mode!=batch.mode) {
            mode=batch.mode;[encoder setRenderPipelineState:mode?self.maskPipeline:self.pipeline];
            [encoder setDepthStencilState:mode==1?self.pushStencil:mode==2?self.popStencil:self.normalStencil];
        }
        if(texture!=batch.texture) {texture=batch.texture;[encoder setFragmentTexture:texture atIndex:0];}
        [encoder drawPrimitives:MTLPrimitiveTypeTriangle vertexStart:batch.start vertexCount:batch.count];
    }
    [encoder endEncoding];[command presentDrawable:self.drawable];
    dispatch_semaphore_t available=_available;__weak BBMetalPainter *owner=self;
    [command addCompletedHandler:^(id<MTLCommandBuffer> completed) {
        owner.gpuMilliseconds=(completed.GPUEndTime-completed.GPUStartTime)*1000;
        dispatch_semaphore_signal(available);
    }];
    [command commit];self.drawable=nil;
}
- (void)append:(const BBVertex *)vertices count:(NSUInteger)count texture:(id<MTLTexture>)texture mode:(NSUInteger)mode {
    if(_count+count>_capacity) {_capacity=MAX(_count+count,MAX(8192,_capacity*2));_vertices=realloc(_vertices,_capacity*sizeof(BBVertex));}
    memcpy(_vertices+_count,vertices,count*sizeof(BBVertex));
    BBMetalBatch *batch=self.batches.lastObject;
    if(!batch || batch.texture!=texture || batch.mode!=mode) {
        batch=[BBMetalBatch new];batch.texture=texture;batch.start=_count;batch.mode=mode;[self.batches addObject:batch];
    }
    batch.count+=count;_count+=count;
}
static vector_float4 BBColor(jint argb) {
    uint32_t c=(uint32_t)argb;float a=(c>>24)/255.f;
    return (vector_float4){((c>>16)&255)/255.f*a,((c>>8)&255)/255.f*a,(c&255)/255.f*a,a};
}
- (void)triangle:(vector_float2)a b:(vector_float2)b c:(vector_float2)c color:(vector_float4)color mode:(NSUInteger)mode {
    BBVertex v[]={{a,{0,0},color},{b,{0,0},color},{c,{0,0},color}};[self append:v count:3 texture:self.white mode:mode];
}
- (id<MTLTexture>)cached:(NSString *)key {
    id<MTLTexture> result=self.textures[key];
    if(result) {[self.textureOrder removeObject:key];[self.textureOrder addObject:key];[self reference:key texture:result];}return result;
}
- (void)reference:(NSString *)key texture:(id<MTLTexture>)texture {
    if(![self.frameTextureKeys containsObject:key]) {
        [self.frameTextureKeys addObject:key];self.frameTextureBytes+=texture.width*texture.height*4;
        self.peakFrameTextureBytes=MAX(self.peakFrameTextureBytes,self.frameTextureBytes);
    }
}
- (NSDictionary *)diagnostics {return @{@"water_shaders":@(self.waterShaders),@"texture_bytes":@(self.textureBytes),@"texture_budget":@(self.textureBudget),@"uploads":@(self.textureUploads),@"evictions":@(self.textureEvictions),@"peak_frame_texture_bytes":@(self.peakFrameTextureBytes),@"vertex_capacity_bytes":@(_capacity*sizeof(BBVertex)),@"direct_layer_fallbacks":@(self.directLayerFallbacks),@"pending_preparations":@(self.pending.count)};}
- (id<MTLTexture>)makeTexture:(UIImage *)image {
    if(!image) return self.white;
    size_t width=CGImageGetWidth(image.CGImage),height=CGImageGetHeight(image.CGImage);
    CGColorSpaceRef space=CGColorSpaceCreateWithName(kCGColorSpaceSRGB);
    CGContextRef ctx=CGBitmapContextCreate(NULL,width,height,8,width*4,space,kCGBitmapByteOrder32Little|kCGImageAlphaPremultipliedFirst);CGColorSpaceRelease(space);
    if(!ctx) return self.white;
    CGContextSetBlendMode(ctx,kCGBlendModeCopy);CGContextDrawImage(ctx,CGRectMake(0,0,width,height),image.CGImage);
    MTLTextureDescriptor *d=[MTLTextureDescriptor texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm width:width height:height mipmapped:NO];
    d.usage=MTLTextureUsageShaderRead;
    id<MTLTexture> texture=[self.device newTextureWithDescriptor:d];
    [texture replaceRegion:MTLRegionMake2D(0,0,width,height) mipmapLevel:0 withBytes:CGBitmapContextGetData(ctx) bytesPerRow:width*4];CGContextRelease(ctx);
    return texture;
}
- (void)storeTexture:(id<MTLTexture>)texture key:(NSString *)key mayEvict:(BOOL)evict {
    if(self.textures[key] || !texture) return;
    NSUInteger bytes=texture.width*texture.height*4;
    const NSUInteger budget=self.textureBudget;
    if(!evict && self.textureBytes+bytes>budget)return;
    self.textureUploads++;
    while(self.textureBytes+bytes>budget && self.textureOrder.count) {
        NSString *oldKey=self.textureOrder.firstObject;id<MTLTexture> old=self.textures[oldKey];self.textureBytes-=old.width*old.height*4;
        [self.textures removeObjectForKey:oldKey];[self.textureOrder removeObjectAtIndex:0];
        self.textureEvictions++;
    }
    if(bytes<=budget) {self.textures[key]=texture;[self.textureOrder addObject:key];self.textureBytes+=bytes;}
 }
- (id<MTLTexture>)upload:(UIImage *)image key:(NSString *)key {
    id<MTLTexture> texture=[self makeTexture:image];[self storeTexture:texture key:key mayEvict:YES];
    [self reference:key texture:texture];return texture;
}
- (void)prefetchAtlas:(jint)atlas frame:(jint)frame {
    NSString *key=[NSString stringWithFormat:@"sprite:%d:%d",atlas,frame];
    if(self.textures[key] || [self.pending containsObject:key] || self.pending.count>=8
            || self.textureBytes+4*1024*1024>self.textureBudget)return;
    [self.pending addObject:key];
    dispatch_async(self.preparationQueue, ^{ @autoreleasepool {
        id<MTLTexture> texture=[self makeTexture:[self.preparationResources frame:frame atlas:atlas]];
        dispatch_async(dispatch_get_main_queue(), ^{[self.pending removeObject:key];[self storeTexture:texture key:key mayEvict:NO];});
    }});
}
- (id<MTLTexture>)texture:(jint)atlas frame:(jint)frame {
    NSString *key=[NSString stringWithFormat:@"sprite:%d:%d",atlas,frame];
    id<MTLTexture> texture=[self cached:key]?:[self upload:[self.resources frame:frame atlas:atlas] key:key];
    int count=[BBSpriteAtlas countWithInt:atlas];
    if(count==32) {[self prefetchAtlas:atlas frame:(frame+1)%count];[self prefetchAtlas:atlas frame:(frame+count-1)%count];}
    return texture;
}
- (void)quad:(vector_float2)p a:(vector_float2)a b:(vector_float2)b texture:(id<MTLTexture>)texture alpha:(float)alpha {
    vector_float4 color={alpha,alpha,alpha,alpha};
    BBVertex v[]={{p,{0,0},color},{p+a,{1,0},color},{p+b,{0,1},color},
        {p+a,{1,0},color},{p+a+b,{1,1},color},{p+b,{0,1},color}};
    [self append:v count:6 texture:texture mode:0];
}
- (void)waterFoamWithFloat:(jfloat)x withFloat:(jfloat)y withFloat:(jfloat)ax withFloat:(jfloat)ay withFloat:(jfloat)bx withFloat:(jfloat)by withFloat:(jfloat)phase withInt:(jint)kind withFloat:(jfloat)alpha {
    if(alpha<=.005f)return;
    if(!self.waterShaders) {
        if(kind<4)[self spriteAffineWithInt:BBSpriteAtlas_FOAM withInt:kind withFloat:x withFloat:y withFloat:ax withFloat:ay withFloat:bx withFloat:by withFloat:alpha];
        else [BBWaterFoam fallbackWithBBPainter:self withFloat:x withFloat:y withFloat:ax withFloat:ay withFloat:bx withFloat:by withFloat:phase withInt:kind withFloat:alpha];
        return;
    }
    vector_float2 a={ax,ay},b={bx,by},p=(vector_float2){x,y}-(a+b)*.5f;
    vector_float4 color={.76f*alpha,.94f*alpha,.90f*alpha,alpha};uint32_t mode=2+kind;
    BBVertex v[]={{p,{0,0},color,mode,phase},{p+a,{1,0},color,mode,phase},{p+b,{0,1},color,mode,phase},
        {p+a,{1,0},color,mode,phase},{p+a+b,{1,1},color,mode,phase},{p+b,{0,1},color,mode,phase}};
    [self append:v count:6 texture:self.white mode:0];
}
- (jboolean)prefersPolygonBatches {return YES;}
- (jboolean)preparesLayers {return YES;}
- (void)prepareLayerWithBBStaticLayer:(BBStaticLayer *)layer withFloat:(jfloat)scale {
    [self prepareLayer:layer scale:scale visible:NO];
}
- (void)prepareLayer:(BBStaticLayer *)layer scale:(float)scale visible:(BOOL)visible {
    if(![layer backgroundSafe])return;
    NSString *key=[NSString stringWithFormat:@"layer:%d:%g",[layer key],[BBStaticLayer rasterScaleWithFloat:scale]];
    if(self.textures[key] || [self.pending containsObject:key] || self.pending.count>=(visible?12:4))return;
    [self.pending addObject:key];
    dispatch_async(self.preparationQueue, ^{ @autoreleasepool {
        id<MTLTexture> texture=[self makeTexture:[self.preparationResources imageForLayer:layer scale:scale]];
        dispatch_async(dispatch_get_main_queue(), ^{[self.pending removeObject:key];[self storeTexture:texture key:key mayEvict:visible];});
    }});
}
- (void)beginFrame {[self.resources beginFrame];[self.frameTextureKeys removeAllObjects];self.frameTextureBytes=0;}
- (void)layerWithBBStaticLayer:(BBStaticLayer *)layer withFloat:(jfloat)x withFloat:(jfloat)y withFloat:(jfloat)scale withFloat:(jfloat)alpha {
    float w=[layer width]*scale,h=[layer height]*scale;
    if(alpha<=0 || !CGRectIntersectsRect(self.visibleBounds,CGRectMake(x,y,w,h)))return;
    NSString *key=[NSString stringWithFormat:@"layer:%d:%g",[layer key],[BBStaticLayer rasterScaleWithFloat:scale]];
    id<MTLTexture> texture=[self cached:key];
    if(!texture && [layer backgroundSafe]) {
        [self prepareLayer:layer scale:scale visible:YES];self.directLayerFallbacks++;
        float width=self.visibleBounds.size.width,height=self.visibleBounds.size.height;
        float left=fmaxf(0,x),right=fminf(width,x+w),top=fmaxf(0,y),bottom=fminf(height,y+h);
        float rectangles[][4]={{0,0,left,height},{right,0,width,height},{left,0,right,top},{left,bottom,right,height}};
        IOSFloatArray *outside=[IOSFloatArray arrayWithLength:48];
        for(int i=0;i<4;i++){float *r=rectangles[i],*v=outside->buffer_+i*12;
            float points[]={r[0],r[1],r[2],r[1],r[0],r[3],r[2],r[1],r[2],r[3],r[0],r[3]};memcpy(v,points,sizeof(points));}
        [self pushClipOutsideWithFloatArray:outside];
        @try {[layer drawWithBBPainter:self withFloat:x withFloat:y withFloat:scale withFloat:alpha];}
        @finally {[self popClip];}
        return;
    }
    if(!texture) texture=[self upload:[self.resources imageForLayer:layer scale:scale] key:key];
    [self quad:(vector_float2){x,y} a:(vector_float2){w,0} b:(vector_float2){0,h} texture:texture alpha:alpha];
}
- (void)spriteWithInt:(jint)atlas withInt:(jint)frame withFloat:(jfloat)x withFloat:(jfloat)y withFloat:(jfloat)width withFloat:(jfloat)height withFloat:(jfloat)rotation withFloat:(jfloat)alpha {
    if(alpha<=0)return;
    width*=[BBSpriteAtlas widthScaleWithInt:atlas withInt:frame];height*=[BBSpriteAtlas heightScaleWithInt:atlas withInt:frame];
    float c=cosf(rotation),s=sinf(rotation),px=[BBSpriteAtlas pivotXWithInt:atlas withInt:frame],py=[BBSpriteAtlas pivotYWithInt:atlas withInt:frame];
    vector_float2 a={width*c,width*s},b={-height*s,height*c},p=(vector_float2){x,y}-a*px-b*py;
    float left=fminf(fminf(p.x,p.x+a.x),fminf(p.x+b.x,p.x+a.x+b.x));
    float top=fminf(fminf(p.y,p.y+a.y),fminf(p.y+b.y,p.y+a.y+b.y));
    if(!CGRectIntersectsRect(self.visibleBounds,CGRectMake(left,top,fabsf(a.x)+fabsf(b.x),fabsf(a.y)+fabsf(b.y)))) return;
    [self quad:p a:a b:b texture:[self texture:atlas frame:frame] alpha:alpha];
}
- (void)spriteAffineWithInt:(jint)atlas withInt:(jint)frame withFloat:(jfloat)x withFloat:(jfloat)y withFloat:(jfloat)ax withFloat:(jfloat)ay withFloat:(jfloat)bx withFloat:(jfloat)by withFloat:(jfloat)alpha {
    if(alpha<=0)return;
    vector_float2 a={ax,ay},b={bx,by},p=(vector_float2){x,y}-a*[BBSpriteAtlas pivotXWithInt:atlas withInt:frame]-b*[BBSpriteAtlas pivotYWithInt:atlas withInt:frame];
    [self quad:p a:a b:b texture:[self texture:atlas frame:frame] alpha:alpha];
}
- (void)spriteBlendWithInt:(jint)atlas withInt:(jint)a withInt:(jint)b withFloat:(jfloat)x withFloat:(jfloat)y withFloat:(jfloat)w withFloat:(jfloat)h withFloat:(jfloat)rotation withFloat:(jfloat)blend withFloat:(jfloat)alpha {
    blend=fmaxf(0,fminf(1,blend));
    if(a==b || blend==0 || blend==1) {[self spriteWithInt:atlas withInt:blend==1?b:a withFloat:x withFloat:y withFloat:w withFloat:h withFloat:rotation withFloat:alpha];return;}
    // Initial fast path: source-over crossfade. Exact premultiplied group blending
    // remains on the CG reference path until a dedicated two-texture shader lands.
    [self spriteWithInt:atlas withInt:a withFloat:x withFloat:y withFloat:w withFloat:h withFloat:rotation withFloat:alpha*(1-blend)];
    [self spriteWithInt:atlas withInt:b withFloat:x withFloat:y withFloat:w withFloat:h withFloat:rotation withFloat:alpha*blend];
}
- (void)rectWithFloat:(jfloat)x withFloat:(jfloat)y withFloat:(jfloat)w withFloat:(jfloat)h withInt:(jint)c {
    vector_float4 color=BBColor(c);if(color.w<=0)return;
    [self triangle:(vector_float2){x,y} b:(vector_float2){x+w,y} c:(vector_float2){x,y+h} color:color mode:0];
    [self triangle:(vector_float2){x+w,y} b:(vector_float2){x+w,y+h} c:(vector_float2){x,y+h} color:color mode:0];
}
- (void)circleWithFloat:(jfloat)x withFloat:(jfloat)y withFloat:(jfloat)r withInt:(jint)c {
    vector_float4 color=BBColor(c);if(color.w<=0 || r<=0)return;
    BBVertex v[]={{{x-r,y-r},{0,0},color,1},{{x+r,y-r},{1,0},color,1},{{x-r,y+r},{0,1},color,1},
        {{x+r,y-r},{1,0},color,1},{{x+r,y+r},{1,1},color,1},{{x-r,y+r},{0,1},color,1}};
    [self append:v count:6 texture:self.white mode:0];
}
- (void)lineWithFloat:(jfloat)x withFloat:(jfloat)y withFloat:(jfloat)xx withFloat:(jfloat)yy withFloat:(jfloat)width withInt:(jint)c {
    float dx=xx-x,dy=yy-y,length=hypotf(dx,dy);if(length<=0)return;
    float nx=-dy/length*width/2,ny=dx/length*width/2;vector_float4 color=BBColor(c);if(color.w<=0)return;
    [self triangle:(vector_float2){x+nx,y+ny} b:(vector_float2){xx+nx,yy+ny} c:(vector_float2){x-nx,y-ny} color:color mode:0];
    [self triangle:(vector_float2){xx+nx,yy+ny} b:(vector_float2){xx-nx,yy-ny} c:(vector_float2){x-nx,y-ny} color:color mode:0];
    [self circleWithFloat:x withFloat:y withFloat:width/2 withInt:c];[self circleWithFloat:xx withFloat:yy withFloat:width/2 withInt:c];
}
- (void)polyWithFloatArray:(IOSFloatArray *)xy withInt:(jint)c {
    vector_float4 color=BBColor(c);if(color.w<=0)return;
    // Shared painter polygons are emitted as simple contours; ear clipping also
    // handles the concave machinery/shore shapes without changing their order.
    int n=xy->size_/2;if(n<3)return;
    int indices[n];float area=0;for(int i=0;i<n;i++){indices[i]=i;int j=(i+1)%n;area+=xy->buffer_[i*2]*xy->buffer_[j*2+1]-xy->buffer_[j*2]*xy->buffer_[i*2+1];}
    float sign=area<0?-1:1;
    int remaining=n,guard=n*n;
    while(remaining>=3 && guard-->0) {
        BOOL found=NO;
        for(int i=0;i<remaining;i++) {
            int ia=indices[(i+remaining-1)%remaining],ib=indices[i],ic=indices[(i+1)%remaining];
            vector_float2 a={xy->buffer_[ia*2],xy->buffer_[ia*2+1]},b={xy->buffer_[ib*2],xy->buffer_[ib*2+1]},d={xy->buffer_[ic*2],xy->buffer_[ic*2+1]};
            float cross=(b.x-a.x)*(d.y-a.y)-(b.y-a.y)*(d.x-a.x);if(cross*sign<=0)continue;
            BOOL inside=NO;
            for(int j=0;j<remaining;j++) {int k=indices[j];if(k==ia || k==ib || k==ic)continue;float x=xy->buffer_[k*2],y=xy->buffer_[k*2+1];
                if(((b.x-a.x)*(y-a.y)-(b.y-a.y)*(x-a.x))*sign>=0 && ((d.x-b.x)*(y-b.y)-(d.y-b.y)*(x-b.x))*sign>=0 && ((a.x-d.x)*(y-d.y)-(a.y-d.y)*(x-d.x))*sign>=0){inside=YES;break;}}
            if(inside)continue;
            [self triangle:a b:b c:d color:color mode:0];memmove(indices+i,indices+i+1,(remaining-i-1)*sizeof(int));remaining--;found=YES;break;
        }
        if(!found)break;
    }
}
- (void)polygonsWithFloatArray2:(IOSObjectArray *)xy withInt:(jint)count withInt:(jint)c {
    for(jint i=0;i<count;i++) [self polyWithFloatArray:(IOSFloatArray *)IOSObjectArray_Get(xy,i) withInt:c];
}
- (void)clipData:(NSData *)data mode:(NSUInteger)mode {
    const float *xy=data.bytes;
    for(NSUInteger i=0;i+5<data.length/sizeof(float);i+=6) [self triangle:(vector_float2){xy[i],xy[i+1]} b:(vector_float2){xy[i+2],xy[i+3]} c:(vector_float2){xy[i+4],xy[i+5]} color:(vector_float4){1,1,1,1} mode:mode];
}
- (void)pushClipOutsideWithFloatArray:(IOSFloatArray *)triangles {
    NSData *data=[NSData dataWithBytes:triangles->buffer_ length:triangles->size_*sizeof(float)];[self.clips addObject:data];[self clipData:data mode:1];
}
- (void)popClip {NSData *data=self.clips.lastObject;if(data){[self clipData:data mode:2];[self.clips removeLastObject];}}
@end
