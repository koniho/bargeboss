#import "com/bargeboss/game/ShorelineMaterial.h"
#import "BBPainter.h"
#import "com/bargeboss/game/LightingProfile.h"
#import "com/bargeboss/game/SceneLighting.h"
#import "com/bargeboss/game/VesselShoreLight.h"
#import "IOSPrimitiveArray.h"
#import "IOSObjectArray.h"
#import "com/bargeboss/game/SpriteAtlas.h"
#import "com/bargeboss/game/SpriteGrade.h"
#import "com/bargeboss/game/StaticLayer.h"
#import "com/bargeboss/game/WaterFoam.h"
#import "com/bargeboss/game/WaterReflection.h"
#import "com/bargeboss/game/SmokePuff.h"
#import "com/bargeboss/game/EffectMaterial.h"
#import "com/bargeboss/game/RopeMaterial.h"
#import <math.h>

static void BBReleaseGradePixels(void *owner,const void *data,size_t bytes) {
    (void)data;(void)bytes;(void)CFBridgingRelease(owner);
}

// Only CPU source storage is shared. GPU uploads still use frame-sized images.
// Decode outside the lock; no drawing thread waits for background preparation.
static UIImage *BBDecodedSource(UIImage *source,NSString *key) {
    static NSMutableDictionary<NSString *,UIImage *> *images;
    static NSMutableArray<NSString *> *order;
    static NSLock *lock;static NSUInteger retainedBytes;static dispatch_once_t once;
    dispatch_once(&once, ^{images=[NSMutableDictionary dictionary];order=[NSMutableArray array];lock=[NSLock new];});
    [lock lock];UIImage *cached=images[key];
    if(cached){[order removeObject:key];[order addObject:key];}
    [lock unlock];if(cached)return cached;
    size_t width=CGImageGetWidth(source.CGImage),height=CGImageGetHeight(source.CGImage),bytes=width*height*4;
    const NSUInteger budget=64*1024*1024;
    if(bytes>budget)return source;
    CGColorSpaceRef space=CGColorSpaceCreateWithName(kCGColorSpaceSRGB);
    CGContextRef bitmap=CGBitmapContextCreate(NULL,width,height,8,width*4,space,kCGBitmapByteOrder32Little|(CGBitmapInfo)kCGImageAlphaPremultipliedFirst);
    CGColorSpaceRelease(space);if(!bitmap)return source;
    CGContextSetBlendMode(bitmap,kCGBlendModeCopy);CGContextDrawImage(bitmap,CGRectMake(0,0,width,height),source.CGImage);
    CGImageRef decoded=CGBitmapContextCreateImage(bitmap);CGContextRelease(bitmap);if(!decoded)return source;
    UIImage *image=[UIImage imageWithCGImage:decoded scale:1 orientation:UIImageOrientationUp];CGImageRelease(decoded);
    [lock lock];cached=images[key];
    if(!cached) {
        while(retainedBytes+bytes>budget && order.count) {
            NSString *oldKey=order.firstObject;CGImageRef old=images[oldKey].CGImage;
            retainedBytes-=CGImageGetBytesPerRow(old)*CGImageGetHeight(old);
            [images removeObjectForKey:oldKey];[order removeObjectAtIndex:0];
        }
        images[key]=image;[order addObject:key];retainedBytes+=bytes;cached=image;
    }
    [lock unlock];return cached;
}

@interface BBNativePainter ()
@property(nonatomic,strong) BBLightingProfile *lightingProfile;
@property(nonatomic,strong) NSMutableDictionary<NSNumber *,NSArray<UIImage *> *> *spriteAtlases;
@property(nonatomic,strong) NSMutableDictionary<NSString *,UIImage *> *spriteSources;
@property(nonatomic,strong) NSMutableDictionary<NSString *,UIImage *> *staticLayers;
@property(nonatomic,strong) NSMutableArray<NSString *> *staticLayerOrder;
@property(nonatomic,assign) NSUInteger staticLayerBytes;
@property(nonatomic,assign) jint staticLayerBuilds;
@property(nonatomic,assign) BOOL paintingStaticLayer;
@property(nonatomic,strong) NSMutableDictionary<NSString *,UIImage *> *decodedFrames;
@property(nonatomic,strong) NSMutableArray<NSString *> *decodedFrameOrder;
@property(nonatomic) NSUInteger decodedFrameBytes;
@property(nonatomic) BOOL lazySprites;
- (NSArray<UIImage *> *)atlas:(jint)atlas;
- (UIImage *)gradedImage:(UIImage *)image atlas:(jint)atlas;
@end

@implementation BBNativePainter
- (BBLightingProfile *)lighting {return self.lightingProfile;}
- (void)lightingWithBBLightingProfile:(BBLightingProfile *)profile {
    if(self.lightingProfile==profile)return;
    self.lightingProfile=profile;
    [self.spriteAtlases removeAllObjects];[self.spriteSources removeAllObjects];
    [self.decodedFrames removeAllObjects];[self.decodedFrameOrder removeAllObjects];self.decodedFrameBytes=0;
    [self.staticLayers removeAllObjects];[self.staticLayerOrder removeAllObjects];self.staticLayerBytes=0;
}
- (void)prepareSceneWithBBLightingProfile:(BBLightingProfile *)profile withIntArray:(IOSIntArray *)atlases {
    [self lightingWithBBLightingProfile:profile];
    for(int i=0;i<atlases->size_;i++)for(int f=0;f<[BBSpriteAtlas countWithInt:IOSIntArray_Get(atlases,i)];f++)[self frame:f atlas:IOSIntArray_Get(atlases,i)];
}

- (void)prepareResidentLayersWithBBStaticLayerArray:(IOSObjectArray *)layers {
    for(int i=0;i<layers->size_;i++)for(int scale=1;scale<=2;scale++)
        [self imageForLayer:IOSObjectArray_Get(layers,i) scale:scale];
}

- (instancetype)init {
    if((self=[super init])) {
        self.lightingProfile=[BBLightingProfile presetWithInt:2];
        self.spriteAtlases=[NSMutableDictionary dictionary]; self.spriteSources=[NSMutableDictionary dictionary];
        self.staticLayers=[NSMutableDictionary dictionary];self.staticLayerOrder=[NSMutableArray array];
        self.decodedFrames=[NSMutableDictionary dictionary];self.decodedFrameOrder=[NSMutableArray array];
        self.lazySprites=[NSProcessInfo.processInfo.arguments containsObject:@"--ios-lazy-sprites"];
        [self atlas:0]; [self atlas:1];
    }
    return self;
}
- (jboolean)supportsUnionClips {return NO;}
- (void)beginSurfaceBatch {}
- (void)endSurfaceBatch {}
- (void)beginWaterFoamBatch {}
- (void)endWaterFoamBatch {}
- (void)pushClipOutsideWithFloatArray:(IOSFloatArray *)triangles {
    CGContextRef c=self.context;CGContextSaveGState(c);CGContextBeginPath(c);
    CGContextAddRect(c,CGContextGetClipBoundingBox(c));
    for(jint i=0;i<triangles->size_;i+=6) {
        CGContextMoveToPoint(c,triangles->buffer_[i],triangles->buffer_[i+1]);
        CGContextAddLineToPoint(c,triangles->buffer_[i+2],triangles->buffer_[i+3]);
        CGContextAddLineToPoint(c,triangles->buffer_[i+4],triangles->buffer_[i+5]);CGContextClosePath(c);
    }
    CGContextEOClip(c);
}
- (void)popClip {CGContextRestoreGState(self.context);}
- (jboolean)prefersPolygonBatches {return YES;}
// Java interface defaults are not inherited by handwritten Objective-C hosts.
- (jboolean)retainedSurfacesWithBBStaticLayer:(BBStaticLayer *)layer withFloat:(jfloat)x withFloat:(jfloat)y withFloat:(jfloat)scale {return NO;}
- (jboolean)preparesLayers {return NO;}
- (void)prepareLayerWithBBStaticLayer:(BBStaticLayer *)layer withFloat:(jfloat)scale {(void)layer;(void)scale;}
- (void)beginFrame {
    if(!self.paintingStaticLayer) self.staticLayerBuilds=0;
}
- (void)layerWithBBStaticLayer:(BBStaticLayer *)layer withFloat:(jfloat)x withFloat:(jfloat)y
                    withFloat:(jfloat)scale withFloat:(jfloat)alpha {
    float width=[layer width]*scale,height=[layer height]*scale;
    if(!(width>0 && height>0 && alpha>0) || !isfinite(width) || !isfinite(height)) return;
    CGRect bounds=CGRectMake(x,y,width,height);CGContextRef target=self.context;
    if(!CGRectIntersectsRect(bounds,CGContextGetClipBoundingBox(target))) return;
    if(self.paintingStaticLayer) {[self directLayer:layer x:x y:y scale:scale alpha:alpha];return;}
    NSString *cacheKey=[NSString stringWithFormat:@"%d:%g",[layer key],[BBStaticLayer rasterScaleWithFloat:scale]];
    if(!self.staticLayers[cacheKey] && self.staticLayerBuilds>=[BBStaticLayer maxBuildsPerFrame]) {
        [self directLayer:layer x:x y:y scale:scale alpha:alpha];return;
    }
    UIImage *image=[self imageForLayer:layer scale:scale];
    if(!image) {[self directLayer:layer x:x y:y scale:scale alpha:alpha];return;}
    float density=[BBStaticLayer rasterScaleWithFloat:scale];
    CGContextSaveGState(target);CGContextClipToRect(target,bounds);
    // Preserve the exact logical scale even when raster dimensions round up.
    // The clip trims the subpixel padding instead of squeezing the painted art.
    float drawWidth=CGImageGetWidth(image.CGImage)*scale/density,drawHeight=CGImageGetHeight(image.CGImage)*scale/density;
    CGContextTranslateCTM(target,x,y+drawHeight);CGContextScaleCTM(target,1,-1);
    CGContextSetInterpolationQuality(target,kCGInterpolationLow);CGContextSetAlpha(target,fmaxf(0,fminf(1,alpha)));
    CGContextDrawImage(target,CGRectMake(0,0,drawWidth,drawHeight),image.CGImage);
    CGContextRestoreGState(target);
}
- (UIImage *)imageForLayer:(BBStaticLayer *)layer scale:(float)scale {
    CGContextRef target=self.context;
    float density=[BBStaticLayer rasterScaleWithFloat:scale];
    double pw=ceil([layer width]*density),ph=ceil([layer height]*density);
    NSString *key=[NSString stringWithFormat:@"%d:%g",[layer key],density];
    UIImage *image=self.staticLayers[key];
    if(image) {
        [self.staticLayerOrder removeObject:key];[self.staticLayerOrder addObject:key];
    } else {
        if(pw<=0 || ph<=0 || pw*ph*4>[BBStaticLayer cacheBudgetBytes]) {
            return nil;
        }
        size_t pixelsW=(size_t)pw,pixelsH=(size_t)ph,bytes=pixelsW*pixelsH*4;
        self.staticLayerBuilds++;
        while(self.staticLayerBytes+bytes>(NSUInteger)[BBStaticLayer cacheBudgetBytes] && self.staticLayerOrder.count>0) {
            NSString *oldKey=self.staticLayerOrder.firstObject;UIImage *old=self.staticLayers[oldKey];
            self.staticLayerBytes-=CGImageGetBytesPerRow(old.CGImage)*CGImageGetHeight(old.CGImage);
            [self.staticLayerOrder removeObjectAtIndex:0];[self.staticLayers removeObjectForKey:oldKey];
        }
        CGColorSpaceRef space=CGColorSpaceCreateWithName(kCGColorSpaceSRGB);
        CGContextRef offscreen=CGBitmapContextCreate(NULL,pixelsW,pixelsH,8,pixelsW*4,space,
                kCGBitmapByteOrder32Little|kCGImageAlphaPremultipliedFirst);
        CGColorSpaceRelease(space);
        if(!offscreen) {return nil;}
        CGContextClearRect(offscreen,CGRectMake(0,0,pixelsW,pixelsH));
        CGContextTranslateCTM(offscreen,0,pixelsH);CGContextScaleCTM(offscreen,density,-density);
        CGContextClipToRect(offscreen,CGRectMake(0,0,[layer width],[layer height]));
        CGContextSetShouldAntialias(offscreen,true);
        self.paintingStaticLayer=YES;self.context=offscreen;
        @try {
            [layer paintWithBBPainter:self];
            CGImageRef bitmap=CGBitmapContextCreateImage(offscreen);
            if(bitmap) {image=[UIImage imageWithCGImage:bitmap scale:1 orientation:UIImageOrientationUp];CGImageRelease(bitmap);}
        } @finally {
            self.context=target;self.paintingStaticLayer=NO;CGContextRelease(offscreen);
        }
        if(!image) {return nil;}
        self.staticLayers[key]=image;[self.staticLayerOrder addObject:key];
        self.staticLayerBytes+=CGImageGetBytesPerRow(image.CGImage)*CGImageGetHeight(image.CGImage);
    }
    return image;
}
- (void)directLayer:(BBStaticLayer *)layer x:(float)x y:(float)y scale:(float)scale alpha:(float)alpha {
    CGContextRef ctx=self.context;BOOL previous=self.paintingStaticLayer;self.paintingStaticLayer=YES;
    CGContextSaveGState(ctx);CGRect bounds=CGRectMake(x,y,[layer width]*scale,[layer height]*scale);
    CGContextClipToRect(ctx,bounds);
    BOOL group=alpha<1;
    if(group) {CGContextSetAlpha(ctx,fmaxf(0,alpha));CGContextBeginTransparencyLayerWithRect(ctx,bounds,NULL);}
    @try {
        CGContextTranslateCTM(ctx,x,y);CGContextScaleCTM(ctx,scale,scale);[layer paintWithBBPainter:self];
    } @finally {
        if(group) CGContextEndTransparencyLayer(ctx);
        CGContextRestoreGState(ctx);self.paintingStaticLayer=previous;
    }
}
- (UIImage *)gradedImage:(UIImage *)image atlas:(jint)atlas {
    if(![BBSpriteGrade appliesWithInt:atlas]) return image;
    size_t width=CGImageGetWidth(image.CGImage),height=CGImageGetHeight(image.CGImage),bytes=width*height*4;
    if(bytes>INT32_MAX) [NSException raise:@"SpriteGradeSize" format:@"Scenery image is too large"];
    IOSByteArray *pixels=[IOSByteArray arrayWithLength:(jint)bytes];
    CGColorSpaceRef space=CGColorSpaceCreateWithName(kCGColorSpaceSRGB);
    CGBitmapInfo format=kCGBitmapByteOrder32Big|kCGImageAlphaPremultipliedLast;
    CGContextRef decode=CGBitmapContextCreate(pixels->buffer_,width,height,8,width*4,space,format);
    if(!decode) {CGColorSpaceRelease(space);[NSException raise:@"SpriteGradeDecode" format:@"Cannot decode scenery RGBA buffer"];}
    CGContextSetBlendMode(decode,kCGBlendModeCopy);CGContextDrawImage(decode,CGRectMake(0,0,width,height),image.CGImage);
    CGContextRelease(decode);
    [BBSpriteGrade applySourcePremultipliedRgbaWithInt:atlas withByteArray:pixels withBBLightingProfile:self.lightingProfile];
    // Retain this one graded buffer for CGImage; do not copy a second full-size raster.
    void *owner=(__bridge_retained void *)pixels;
    CGDataProviderRef provider=CGDataProviderCreateWithData(owner,pixels->buffer_,bytes,BBReleaseGradePixels);
    if(!provider) {BBReleaseGradePixels(owner,NULL,0);CGColorSpaceRelease(space);[NSException raise:@"SpriteGradeDecode" format:@"Cannot retain scenery pixels"];}
    CGImageRef result=CGImageCreate(width,height,8,32,width*4,space,format,provider,NULL,false,kCGRenderingIntentDefault);
    UIImage *graded=result?[UIImage imageWithCGImage:result scale:1 orientation:UIImageOrientationUp]:nil;
    if(result) CGImageRelease(result);CGDataProviderRelease(provider);CGColorSpaceRelease(space);
    if(!graded) [NSException raise:@"SpriteGradeDecode" format:@"Cannot create graded scenery image"];
    return graded;
}
- (NSArray<UIImage *> *)atlas:(jint)atlas {
    NSArray<UIImage *> *cached=self.spriteAtlases[@(atlas)]; if(cached) return cached;
    int count=[BBSpriteAtlas countWithInt:atlas];
    NSMutableArray<UIImage *> *frames=[NSMutableArray arrayWithCapacity:count];
    NSMutableDictionary<NSString *,UIImage *> *sources=self.spriteSources;
    for(int f=0;f<count;f++) {
        NSString *file=[BBSpriteAtlas fileWithInt:atlas withInt:f];
        NSString *key=[BBSpriteGrade cacheKeyWithInt:atlas withNSString:file withBBLightingProfile:self.lightingProfile];
        UIImage *image=sources[key];
        if(!image) {
            @autoreleasepool {
                NSString *path=[NSBundle.mainBundle pathForResource:file ofType:nil inDirectory:@"assets/sprites"];
                image=path?[UIImage imageWithContentsOfFile:path]:nil;
                if(!image.CGImage) [NSException raise:@"MissingSpriteAtlas" format:@"Missing or unreadable sprite atlas: assets/sprites/%@",file];
                image=[self gradedImage:image atlas:atlas];sources[key]=image;
            }
        }
        float width=(float)CGImageGetWidth(image.CGImage),height=(float)CGImageGetHeight(image.CGImage);
        int sourceFrame=[BBSpriteAtlas sourceFrameWithInt:atlas withInt:f];
        float x=roundf(width*[BBSpriteAtlas sourceLeftWithInt:atlas withInt:f]),y=roundf(height*[BBSpriteAtlas sourceTopWithInt:atlas withInt:f]);
        float right=roundf(width*[BBSpriteAtlas sourceRightWithInt:atlas withInt:f]),bottom=roundf(height*[BBSpriteAtlas sourceBottomWithInt:atlas withInt:f]);
        CGImageRef cropped=CGImageCreateWithImageInRect(image.CGImage,CGRectMake(x,y,right-x,bottom-y));
        if(!cropped) [NSException raise:@"InvalidSpriteAtlas" format:@"Invalid sprite cell %d in %@",sourceFrame,file];
        [frames addObject:[UIImage imageWithCGImage:cropped scale:1 orientation:UIImageOrientationUp]];
        CGImageRelease(cropped);
    }
    cached=[frames copy]; self.spriteAtlases[@(atlas)]=cached; return cached;
}
// A cropped CGImage still retains its PNG provider. Materialize only the used
// cell so repeated Core Graphics display-list playback never decodes that PNG.
// Eviction releases our reference; queued drawing commands retain their images.
- (UIImage *)frame:(jint)frame atlas:(jint)atlas {
    UIImage *source=[self atlas:atlas][frame];
    if(self.lazySprites) return source;
    NSString *key=[NSString stringWithFormat:@"%d:%d",atlas,frame];
    UIImage *image=self.decodedFrames[key];
    if(image) {
        [self.decodedFrameOrder removeObject:key];[self.decodedFrameOrder addObject:key];return image;
    }
    if(![BBSpriteGrade appliesWithInt:atlas]) {
        NSString *file=[BBSpriteAtlas fileWithInt:atlas withInt:frame];
        NSString *sourceKey=[BBSpriteGrade cacheKeyWithInt:atlas withNSString:file withBBLightingProfile:self.lightingProfile];
        UIImage *sheet=BBDecodedSource(self.spriteSources[sourceKey],sourceKey);
        float sw=CGImageGetWidth(sheet.CGImage),sh=CGImageGetHeight(sheet.CGImage);
        float x=roundf(sw*[BBSpriteAtlas sourceLeftWithInt:atlas withInt:frame]),y=roundf(sh*[BBSpriteAtlas sourceTopWithInt:atlas withInt:frame]);
        float right=roundf(sw*[BBSpriteAtlas sourceRightWithInt:atlas withInt:frame]),bottom=roundf(sh*[BBSpriteAtlas sourceBottomWithInt:atlas withInt:frame]);
        CGImageRef crop=CGImageCreateWithImageInRect(sheet.CGImage,CGRectMake(x,y,right-x,bottom-y));
        if(crop){source=[UIImage imageWithCGImage:crop scale:1 orientation:UIImageOrientationUp];CGImageRelease(crop);}
    }
    CGImageRef original=source.CGImage;
    size_t width=CGImageGetWidth(original),height=CGImageGetHeight(original);
    const NSUInteger budget=32*1024*1024;
    if(!width || !height || width>budget/4/height) return source;
    size_t bytes=width*height*4;
    while(self.decodedFrameBytes+bytes>budget && self.decodedFrameOrder.count) {
        NSString *oldKey=self.decodedFrameOrder.firstObject;
        CGImageRef old=self.decodedFrames[oldKey].CGImage;
        self.decodedFrameBytes-=CGImageGetBytesPerRow(old)*CGImageGetHeight(old);
        [self.decodedFrames removeObjectForKey:oldKey];[self.decodedFrameOrder removeObjectAtIndex:0];
    }
    CGColorSpaceRef space=CGColorSpaceCreateWithName(kCGColorSpaceSRGB);
    CGContextRef bitmap=CGBitmapContextCreate(NULL,width,height,8,width*4,space,
            kCGBitmapByteOrder32Little|kCGImageAlphaPremultipliedFirst);
    CGColorSpaceRelease(space);
    if(!bitmap) return source;
    CGContextSetBlendMode(bitmap,kCGBlendModeCopy);
    CGContextSetInterpolationQuality(bitmap,kCGInterpolationNone);
    CGContextDrawImage(bitmap,CGRectMake(0,0,width,height),original);
    CGImageRef decoded=CGBitmapContextCreateImage(bitmap);CGContextRelease(bitmap);
    if(!decoded) return source;
    image=[UIImage imageWithCGImage:decoded scale:1 orientation:UIImageOrientationUp];
    self.decodedFrameBytes+=CGImageGetBytesPerRow(decoded)*CGImageGetHeight(decoded);
    CGImageRelease(decoded);
    self.decodedFrames[key]=image;[self.decodedFrameOrder addObject:key];return image;
}
- (void)spriteWithInt:(jint)atlas withInt:(jint)frame withFloat:(jfloat)cx withFloat:(jfloat)cy
           withFloat:(jfloat)width withFloat:(jfloat)height withFloat:(jfloat)rotation withFloat:(jfloat)alpha {
    if(frame<0 || frame>=[BBSpriteAtlas countWithInt:atlas]) [NSException raise:@"InvalidSpriteFrame" format:@"Invalid sprite frame: %d",frame];
    width*=[BBSpriteAtlas widthScaleWithInt:atlas withInt:frame];
    height*=[BBSpriteAtlas heightScaleWithInt:atlas withInt:frame];
    UIImage *image=[self frame:frame atlas:atlas];
    CGContextRef ctx=self.context;
    CGContextSaveGState(ctx);
    CGContextTranslateCTM(ctx,cx,cy); CGContextRotateCTM(ctx,rotation);
    CGContextTranslateCTM(ctx,-[BBSpriteAtlas pivotXWithInt:atlas withInt:frame]*width,
                            -[BBSpriteAtlas pivotYWithInt:atlas withInt:frame]*height);
    // CGImage's raster coordinates run upward; UIKit's stage coordinates run down.
    CGContextTranslateCTM(ctx,0,height); CGContextScaleCTM(ctx,1,-1);
    CGContextSetInterpolationQuality(ctx,kCGInterpolationNone);
    CGContextSetAlpha(ctx,fmaxf(0,fminf(1,alpha)));
    CGContextDrawImage(ctx,CGRectMake(0,0,width,height),image.CGImage);
    CGContextRestoreGState(ctx);
}
// Supplied axes span a complete source cell and already include all caller scaling.
- (void)waterReflectionWithFloat:(jfloat)x withFloat:(jfloat)y withFloat:(jfloat)ax withFloat:(jfloat)ay withFloat:(jfloat)bx withFloat:(jfloat)by withFloat:(jfloat)phase withInt:(jint)color {
    [BBWaterReflection fallbackWithBBPainter:self withFloat:x withFloat:y withFloat:ax withFloat:ay withFloat:bx withFloat:by withFloat:phase withInt:color];
}
- (void)ropeSegmentWithFloat:(jfloat)x withFloat:(jfloat)y withFloat:(jfloat)xx withFloat:(jfloat)yy withFloat:(jfloat)width withFloat:(jfloat)from withFloat:(jfloat)to withInt:(jint)argb {
    [BBRopeMaterial fallbackWithBBPainter:self withFloat:x withFloat:y withFloat:xx withFloat:yy withFloat:width withFloat:from withFloat:to withInt:argb];
}
- (void)shorelineWithFloatArray:(IOSFloatArray *)xy withFloatArray:(IOSFloatArray *)uv withFloat:(jfloat)phase withFloat:(jfloat)energy withInt:(jint)kind withInt:(jint)argb {
    [BBShorelineMaterial fallbackWithBBPainter:self withFloatArray:xy withFloatArray:uv withFloat:phase withFloat:energy withInt:kind withInt:argb];
}
- (void)surfacePolygonWithFloatArray:(IOSFloatArray *)xy withFloatArray:(IOSFloatArray *)uv withInt:(jint)material withInt:(jint)argb withFloat:(jfloat)wetness {
    [self polyWithFloatArray:xy withInt:[self.lightingProfile surfaceWithInt:argb withInt:material]];
}
- (void)materialWithFloat:(jfloat)x withFloat:(jfloat)y withFloat:(jfloat)ax withFloat:(jfloat)ay withFloat:(jfloat)bx withFloat:(jfloat)by withInt:(jint)kind withFloat:(jfloat)detail withFloat:(jfloat)amount withInt:(jint)argb {
    [BBEffectMaterial fallbackWithBBPainter:self withFloat:x withFloat:y withFloat:ax withFloat:ay withFloat:bx withFloat:by withInt:kind withFloat:detail withFloat:amount withInt:argb];
}
- (void)normalSpriteWithInt:(jint)frame withFloat:(jfloat)x withFloat:(jfloat)y withFloat:(jfloat)w withFloat:(jfloat)h withFloat:(jfloat)nx withFloat:(jfloat)ny withFloat:(jfloat)nz withFloat:(jfloat)radius withInt:(jint)argb withFloat:(jfloat)alpha {
    [BBVesselShoreLight normalSpriteFallbackWithBBPainter:self withInt:frame withFloat:x withFloat:y withFloat:w withFloat:h withFloat:nx withFloat:ny withFloat:nz withFloat:radius withInt:argb withFloat:alpha];
}
- (void)smokePuffWithFloat:(jfloat)x withFloat:(jfloat)y withFloat:(jfloat)radius withFloat:(jfloat)phase withInt:(jint)argb {
    [BBSmokePuff fallbackWithBBPainter:self withFloat:x withFloat:y withFloat:radius withInt:argb];
}
- (void)waterFoamWithFloat:(jfloat)x withFloat:(jfloat)y withFloat:(jfloat)ax withFloat:(jfloat)ay withFloat:(jfloat)bx withFloat:(jfloat)by withFloat:(jfloat)phase withInt:(jint)kind withFloat:(jfloat)alpha {
    if(kind<4) [self spriteAffineWithInt:BBSpriteAtlas_FOAM withInt:kind withFloat:x withFloat:y withFloat:ax withFloat:ay withFloat:bx withFloat:by withFloat:alpha];
    else [BBWaterFoam fallbackWithBBPainter:self withFloat:x withFloat:y withFloat:ax withFloat:ay withFloat:bx withFloat:by withFloat:phase withInt:kind withFloat:alpha];
}
- (void)spriteAffineWithInt:(jint)atlas withInt:(jint)frame withFloat:(jfloat)x withFloat:(jfloat)y
                 withFloat:(jfloat)ax withFloat:(jfloat)ay withFloat:(jfloat)bx withFloat:(jfloat)by withFloat:(jfloat)alpha {
    if(frame<0 || frame>=[BBSpriteAtlas countWithInt:atlas]) [NSException raise:@"InvalidSpriteFrame" format:@"Invalid affine sprite frame: %d",frame];
    if(!(alpha>0) || ax*by-ay*bx==0) return;
    UIImage *image=[self frame:frame atlas:atlas];float px=[BBSpriteAtlas pivotXWithInt:atlas withInt:frame],py=[BBSpriteAtlas pivotYWithInt:atlas withInt:frame];
    CGContextRef ctx=self.context;CGContextSaveGState(ctx);
    CGContextConcatCTM(ctx,CGAffineTransformMake(ax,ay,bx,by,x-ax*px-bx*py,y-ay*px-by*py));
    CGContextTranslateCTM(ctx,0,1);CGContextScaleCTM(ctx,1,-1);
    CGContextSetInterpolationQuality(ctx,kCGInterpolationLow);CGContextSetBlendMode(ctx,kCGBlendModeNormal);
    CGContextSetAlpha(ctx,fmaxf(0,fminf(1,alpha)));CGContextDrawImage(ctx,CGRectMake(0,0,1,1),image.CGImage);
    CGContextRestoreGState(ctx);
}
// Draw a registered cell inside an existing sprite-local transparency layer.
- (void)cell:(UIImage *)image x:(float)x y:(float)y width:(float)width height:(float)height alpha:(float)alpha {
    CGContextRef ctx=self.context; CGContextSaveGState(ctx);
    CGContextTranslateCTM(ctx,x,y+height); CGContextScaleCTM(ctx,1,-1);
    CGContextSetInterpolationQuality(ctx,kCGInterpolationNone); CGContextSetAlpha(ctx,alpha);
    CGContextDrawImage(ctx,CGRectMake(0,0,width,height),image.CGImage); CGContextRestoreGState(ctx);
}
- (void)spriteBlendWithInt:(jint)atlas withInt:(jint)frameA withInt:(jint)frameB
                withFloat:(jfloat)cx withFloat:(jfloat)cy withFloat:(jfloat)width withFloat:(jfloat)height
                withFloat:(jfloat)rotation withFloat:(jfloat)blend withFloat:(jfloat)alpha {
    int count=[BBSpriteAtlas countWithInt:atlas];
    if(frameA<0 || frameA>=count || frameB<0 || frameB>=count)
        [NSException raise:@"InvalidSpriteFrame" format:@"Invalid sprite blend frames: %d, %d",frameA,frameB];
    int weight=(int)roundf(fmaxf(0,fminf(1,blend))*255);
    if(weight==0 || frameA==frameB || weight==255) {
        [self spriteWithInt:atlas withInt:weight==255?frameB:frameA withFloat:cx withFloat:cy
                  withFloat:width withFloat:height withFloat:rotation withFloat:alpha]; return;
    }
    UIImage *imageA=[self frame:frameA atlas:atlas],*imageB=[self frame:frameB atlas:atlas];
    float aw=width*[BBSpriteAtlas widthScaleWithInt:atlas withInt:frameA];
    float ah=height*[BBSpriteAtlas heightScaleWithInt:atlas withInt:frameA];
    float bw=width*[BBSpriteAtlas widthScaleWithInt:atlas withInt:frameB];
    float bh=height*[BBSpriteAtlas heightScaleWithInt:atlas withInt:frameB];
    float ax=-[BBSpriteAtlas pivotXWithInt:atlas withInt:frameA]*aw;
    float ay=-[BBSpriteAtlas pivotYWithInt:atlas withInt:frameA]*ah;
    float bx=-[BBSpriteAtlas pivotXWithInt:atlas withInt:frameB]*bw;
    float by=-[BBSpriteAtlas pivotYWithInt:atlas withInt:frameB]*bh;
    CGRect bounds=CGRectMake(fminf(ax,bx)-2,fminf(ay,by)-2,
            fmaxf(ax+aw,bx+bw)-fminf(ax,bx)+4,fmaxf(ay+ah,by+bh)-fminf(ay,by)+4);
    CGContextRef ctx=self.context; CGContextSaveGState(ctx);
    CGContextTranslateCTM(ctx,cx,cy); CGContextRotateCTM(ctx,rotation);
    // Group alpha applies once when the layer is composited; inside, sum premultiplied cells.
    CGContextSetAlpha(ctx,fmaxf(0,fminf(1,alpha)));
    CGContextSetBlendMode(ctx,kCGBlendModeNormal); CGContextBeginTransparencyLayerWithRect(ctx,bounds,NULL);
    [self cell:imageA x:ax y:ay width:aw height:ah alpha:(255-weight)/255.0f];
    CGContextSetBlendMode(ctx,kCGBlendModePlusLighter);
    [self cell:imageB x:bx y:by width:bw height:bh alpha:weight/255.0f];
    CGContextEndTransparencyLayer(ctx); CGContextRestoreGState(ctx);
}
- (void)color:(jint)argb {
    uint32_t c=(uint32_t)argb;
    CGContextSetRGBFillColor(self.context,((c>>16)&255)/255.0,((c>>8)&255)/255.0,(c&255)/255.0,(c>>24)/255.0);
    CGContextSetRGBStrokeColor(self.context,((c>>16)&255)/255.0,((c>>8)&255)/255.0,(c&255)/255.0,(c>>24)/255.0);
}
- (void)rectWithFloat:(jfloat)x withFloat:(jfloat)y withFloat:(jfloat)w withFloat:(jfloat)h withInt:(jint)c {
    [self color:c]; CGContextFillRect(self.context,CGRectMake(x,y,w,h));
}
- (void)circleWithFloat:(jfloat)x withFloat:(jfloat)y withFloat:(jfloat)radius withInt:(jint)c {
    [self color:c]; CGContextFillEllipseInRect(self.context,CGRectMake(x-radius,y-radius,radius*2,radius*2));
}
- (void)convexQuadWithFloat:(jfloat)ax withFloat:(jfloat)ay withFloat:(jfloat)bx withFloat:(jfloat)by withFloat:(jfloat)cx withFloat:(jfloat)cy withFloat:(jfloat)dx withFloat:(jfloat)dy withInt:(jint)c {
    [self color:c];CGContextBeginPath(self.context);CGContextMoveToPoint(self.context,ax,ay);
    CGContextAddLineToPoint(self.context,bx,by);CGContextAddLineToPoint(self.context,cx,cy);
    CGContextAddLineToPoint(self.context,dx,dy);CGContextClosePath(self.context);CGContextFillPath(self.context);
}
- (void)polyWithFloatArray:(IOSFloatArray *)xy withInt:(jint)c {
    if(xy->size_<6) return;
    [self color:c]; CGContextBeginPath(self.context);
    CGContextMoveToPoint(self.context,xy->buffer_[0],xy->buffer_[1]);
    for(jint i=2;i<xy->size_;i+=2) CGContextAddLineToPoint(self.context,xy->buffer_[i],xy->buffer_[i+1]);
    CGContextClosePath(self.context); CGContextFillPath(self.context);
}
- (void)polygonsWithFloatArray2:(IOSObjectArray *)xy withInt:(jint)count withInt:(jint)c {
    [self color:c];CGContextBeginPath(self.context);
    for(jint n=0;n<count;n++) {
        IOSFloatArray *q=(IOSFloatArray *)IOSObjectArray_Get(xy,n);
        CGContextMoveToPoint(self.context,q->buffer_[0],q->buffer_[1]);
        for(jint i=2;i<q->size_;i+=2)CGContextAddLineToPoint(self.context,q->buffer_[i],q->buffer_[i+1]);
        CGContextClosePath(self.context);
    }
    CGContextFillPath(self.context);
}
- (void)lineWithFloat:(jfloat)x withFloat:(jfloat)y withFloat:(jfloat)xx withFloat:(jfloat)yy withFloat:(jfloat)width withInt:(jint)c {
    [self color:c]; CGContextSetLineWidth(self.context,width); CGContextSetLineCap(self.context,kCGLineCapRound);
    CGContextBeginPath(self.context); CGContextMoveToPoint(self.context,x,y); CGContextAddLineToPoint(self.context,xx,yy); CGContextStrokePath(self.context);
}
@end
