#import "BBPainter.h"
#import "IOSPrimitiveArray.h"
#import "com/bargeboss/game/SpriteAtlas.h"
#import "com/bargeboss/game/SpriteGrade.h"
#import <math.h>

static void BBReleaseGradePixels(void *owner,const void *data,size_t bytes) {
    (void)data;(void)bytes;(void)CFBridgingRelease(owner);
}

@interface BBNativePainter ()
@property(nonatomic,strong) NSMutableDictionary<NSNumber *,NSArray<UIImage *> *> *spriteAtlases;
@property(nonatomic,strong) NSMutableDictionary<NSString *,UIImage *> *spriteSources;
- (NSArray<UIImage *> *)atlas:(jint)atlas;
- (UIImage *)gradedImage:(UIImage *)image atlas:(jint)atlas;
@end

@implementation BBNativePainter
- (instancetype)init {
    if((self=[super init])) {
        self.spriteAtlases=[NSMutableDictionary dictionary]; self.spriteSources=[NSMutableDictionary dictionary];
        [self atlas:0]; [self atlas:1];
    }
    return self;
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
    [BBSpriteGrade applyPremultipliedRgbaWithInt:atlas withByteArray:pixels];
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
        NSString *key=[BBSpriteGrade cacheKeyWithInt:atlas withNSString:file];
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
- (void)spriteWithInt:(jint)atlas withInt:(jint)frame withFloat:(jfloat)cx withFloat:(jfloat)cy
           withFloat:(jfloat)width withFloat:(jfloat)height withFloat:(jfloat)rotation withFloat:(jfloat)alpha {
    if(frame<0 || frame>=[BBSpriteAtlas countWithInt:atlas]) [NSException raise:@"InvalidSpriteFrame" format:@"Invalid sprite frame: %d",frame];
    width*=[BBSpriteAtlas widthScaleWithInt:atlas withInt:frame];
    height*=[BBSpriteAtlas heightScaleWithInt:atlas withInt:frame];
    UIImage *image=[self atlas:atlas][frame];
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
    NSArray<UIImage *> *frames=[self atlas:atlas];
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
    [self cell:frames[frameA] x:ax y:ay width:aw height:ah alpha:(255-weight)/255.0f];
    CGContextSetBlendMode(ctx,kCGBlendModePlusLighter);
    [self cell:frames[frameB] x:bx y:by width:bw height:bh alpha:weight/255.0f];
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
- (void)polyWithFloatArray:(IOSFloatArray *)xy withInt:(jint)c {
    if(xy->size_<6) return;
    [self color:c]; CGContextBeginPath(self.context);
    CGContextMoveToPoint(self.context,xy->buffer_[0],xy->buffer_[1]);
    for(jint i=2;i<xy->size_;i+=2) CGContextAddLineToPoint(self.context,xy->buffer_[i],xy->buffer_[i+1]);
    CGContextClosePath(self.context); CGContextFillPath(self.context);
}
- (void)lineWithFloat:(jfloat)x withFloat:(jfloat)y withFloat:(jfloat)xx withFloat:(jfloat)yy withFloat:(jfloat)width withInt:(jint)c {
    [self color:c]; CGContextSetLineWidth(self.context,width); CGContextSetLineCap(self.context,kCGLineCapRound);
    CGContextBeginPath(self.context); CGContextMoveToPoint(self.context,x,y); CGContextAddLineToPoint(self.context,xx,yy); CGContextStrokePath(self.context);
}
@end
