#import <Foundation/Foundation.h>
@class BBGame;
@interface BBEngineRumble : NSObject
- (void)update:(BBGame *)game feedback:(BOOL)feedback;
- (void)stop;
@end
