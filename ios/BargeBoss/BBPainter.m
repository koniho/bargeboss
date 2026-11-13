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
    int count=[BBSpriteAtlas countWithInt:atlas],columns=[BBSpriteAtlas columnsWithInt:atlas],rows=[BBSpriteAtlas rowsWithInt:atlas];
    NSMutableArray<UIImage *> *frames=[NSMutableArray arrayWithCapacity:count];
    for(int f=0;f<count;f++) {
        int col=f%columns,row=f/columns;
        float x=roundf(width*col/(float)columns),y=roundf(height*row/(float)rows);
        float right=roundf(width*(col+1)/(float)columns),bottom=roundf(height*(row+1)/(float)rows);
        CGImageRef cropped=CGImageCreateWithImageInRect(image.CGImage,CGRectMake(x,y,right-x,bottom-y));
        if(!cropped) [NSException raise:@"InvalidSpriteAtlas" format:@"Invalid sprite cell %d in %@",f,file];
        [frames addObject:[UIImage imageWithCGImage:cropped scale:1 orientation:UIImageOrientationUp]];
        CGImageRelease(cropped);
    }
    cached=[frames copy]; self.spriteAtlases[@(atlas)]=cached; return cached;
}
- (void)spriteWithInt:(jint)atlas withInt:(jint)frame withFloat:(jfloat)cx withFloat:(jfloat)cy
           withFloat:(jfloat)width withFloat:(jfloat)height withFloat:(jfloat)rotation withFloat:(jfloat)alpha {
    if(frame<0 || frame>=[BBSpriteAtlas countWithInt:atlas]) [NSException raise:@"InvalidSpriteFrame" format:@"Invalid sprite frame: %d",frame];
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
    float ax=-[BBSpriteAtlas pivotXWithInt:atlas withInt:frameA]*width;
    float ay=-[BBSpriteAtlas pivotYWithInt:atlas withInt:frameA]*height;
    float bx=-[BBSpriteAtlas pivotXWithInt:atlas withInt:frameB]*width;
    float by=-[BBSpriteAtlas pivotYWithInt:atlas withInt:frameB]*height;
    CGRect bounds=CGRectMake(fminf(ax,bx)-2,fminf(ay,by)-2,fabsf(ax-bx)+width+4,fabsf(ay-by)+height+4);
    CGContextRef ctx=self.context; CGContextSaveGState(ctx);
    CGContextTranslateCTM(ctx,cx,cy); CGContextRotateCTM(ctx,rotation);
    // Group alpha applies once when the layer is composited; inside, sum premultiplied cells.
    CGContextSetAlpha(ctx,fmaxf(0,fminf(1,alpha)));
    CGContextSetBlendMode(ctx,kCGBlendModeNormal); CGContextBeginTransparencyLayerWithRect(ctx,bounds,NULL);
    [self cell:frames[frameA] x:ax y:ay width:width height:height alpha:(255-weight)/255.0f];
    CGContextSetBlendMode(ctx,kCGBlendModePlusLighter);
    [self cell:frames[frameB] x:bx y:by width:width height:height alpha:weight/255.0f];
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
