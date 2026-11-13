#import "BBPainter.h"
#import "IOSPrimitiveArray.h"
#import "com/bargeboss/game/SpriteAtlas.h"
#import <math.h>

@interface BBNativePainter ()
@property(nonatomic,strong) NSMutableDictionary<NSNumber *,NSArray<UIImage *> *> *spriteAtlases;
- (NSArray<UIImage *> *)atlas:(jint)atlas;
@end

@implementation BBNativePainter
- (instancetype)init {
    if((self=[super init])) {
        self.spriteAtlases=[NSMutableDictionary dictionary];
        [self atlas:0]; [self atlas:1];
    }
    return self;
}
- (NSArray<UIImage *> *)atlas:(jint)atlas {
    NSArray<UIImage *> *cached=self.spriteAtlases[@(atlas)]; if(cached) return cached;
    NSString *file=[BBSpriteAtlas fileWithInt:atlas];
    NSString *path=[NSBundle.mainBundle pathForResource:file ofType:nil inDirectory:@"assets/sprites"];
    UIImage *image=path?[UIImage imageWithContentsOfFile:path]:nil;
    if(!image.CGImage) [NSException raise:@"MissingSpriteAtlas" format:@"Missing or unreadable sprite atlas: assets/sprites/%@",file];
    float width=(float)CGImageGetWidth(image.CGImage),height=(float)CGImageGetHeight(image.CGImage);
    NSMutableArray<UIImage *> *frames=[NSMutableArray arrayWithCapacity:8];
    for(int f=0;f<8;f++) {
        int col=f%4,row=f/4;
        float x=roundf(width*col/4.0f),y=roundf(height*row/2.0f);
        float right=roundf(width*(col+1)/4.0f),bottom=roundf(height*(row+1)/2.0f);
        CGImageRef cropped=CGImageCreateWithImageInRect(image.CGImage,CGRectMake(x,y,right-x,bottom-y));
        if(!cropped) [NSException raise:@"InvalidSpriteAtlas" format:@"Invalid sprite cell %d in %@",f,file];
        [frames addObject:[UIImage imageWithCGImage:cropped scale:1 orientation:UIImageOrientationUp]];
        CGImageRelease(cropped);
    }
    cached=[frames copy]; self.spriteAtlases[@(atlas)]=cached; return cached;
}
- (void)spriteWithInt:(jint)atlas withInt:(jint)frame withFloat:(jfloat)cx withFloat:(jfloat)cy
           withFloat:(jfloat)width withFloat:(jfloat)height withFloat:(jfloat)rotation withFloat:(jfloat)alpha {
    if(frame<0 || frame>=8) [NSException raise:@"InvalidSpriteFrame" format:@"Invalid sprite frame: %d",frame];
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
