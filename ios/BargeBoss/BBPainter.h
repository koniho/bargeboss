#import <UIKit/UIKit.h>
#import "com/bargeboss/game/Painter.h"
@class BBStaticLayer;

@interface BBNativePainter : NSObject <BBPainter>
@property(nonatomic, assign) CGContextRef context;
- (UIImage *)frame:(jint)frame atlas:(jint)atlas;
- (UIImage *)imageForLayer:(BBStaticLayer *)layer scale:(float)scale;
@end
