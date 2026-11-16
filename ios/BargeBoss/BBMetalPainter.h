#import <Metal/Metal.h>
#import <QuartzCore/CAMetalLayer.h>
#import "com/bargeboss/game/Painter.h"
@interface BBMetalPainter : NSObject <BBPainter>
@property(nonatomic,readonly) id<MTLDevice> device;
@property(atomic,readonly) double gpuMilliseconds;
- (BOOL)startLayer:(CAMetalLayer *)layer scale:(float)scale offset:(CGPoint)offset bounds:(CGSize)bounds;
- (void)present;
- (NSDictionary *)diagnostics;
@end
