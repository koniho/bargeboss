#import "BBPainter.h"
#import "IOSPrimitiveArray.h"

@implementation BBNativePainter
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
