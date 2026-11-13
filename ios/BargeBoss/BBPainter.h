#import <UIKit/UIKit.h>
#import "com/bargeboss/game/Painter.h"

@interface BBNativePainter : NSObject <BBPainter>
@property(nonatomic, assign) CGContextRef context;
@end
