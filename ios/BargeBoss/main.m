#import <UIKit/UIKit.h>
#import <QuartzCore/QuartzCore.h>
#import "BBPainter.h"
#import "com/bargeboss/game/Game.h"
#import "com/bargeboss/game/Viewport.h"
#import "IOSClass.h"
#import "NSString+JavaString.h"

@interface BBGameView : UIView
@property(nonatomic,strong) BBGame *game;
@property(nonatomic,strong) BBViewport *viewport;
@property(nonatomic,strong) BBNativePainter *painter;
@property(nonatomic,strong) CADisplayLink *displayLink;
@property(nonatomic,strong) UITouch *primary;
@property(nonatomic) CFTimeInterval lastTime;
- (void)suspend;
- (void)resume;
@end

@implementation BBGameView
- (instancetype)initWithFrame:(CGRect)frame {
    if((self=[super initWithFrame:frame])) {
        self.backgroundColor=[UIColor colorWithRed:16/255.0 green:30/255.0 blue:37/255.0 alpha:1];
        self.multipleTouchEnabled=YES;
        self.game=[[BBGame alloc] init]; self.painter=[[BBNativePainter alloc] init];
        self.viewport=[[BBViewport alloc] initWithFloat:600 withFloat:960];
        self.displayLink=[CADisplayLink displayLinkWithTarget:self selector:@selector(frame:)];
        self.displayLink.preferredFramesPerSecond=60;
        [self.displayLink addToRunLoop:NSRunLoop.mainRunLoop forMode:NSRunLoopCommonModes];
    }
    return self;
}
- (void)layoutSubviews {
    [super layoutSubviews]; UIEdgeInsets insets=self.safeAreaInsets;
    self.viewport=[[BBViewport alloc] initWithFloat:(jfloat)(self.bounds.size.width-insets.left-insets.right)
                                        withFloat:(jfloat)(self.bounds.size.height-insets.top-insets.bottom)];
    [self.game cancel]; self.primary=nil;
}
- (void)frame:(CADisplayLink *)link {
    if(self.lastTime>0) [self.game advanceWithDouble:link.timestamp-self.lastTime];
    self.lastTime=link.timestamp; [self setNeedsDisplay];
}
- (void)suspend { [self.game background]; self.primary=nil; self.lastTime=0; self.displayLink.paused=YES; [self setNeedsDisplay]; }
- (void)resume { self.lastTime=0; self.displayLink.paused=NO; }
- (void)drawRect:(CGRect)rect {
    CGContextRef ctx=UIGraphicsGetCurrentContext(); if(!ctx) return;
    UIEdgeInsets insets=self.safeAreaInsets;
    CGContextSaveGState(ctx);
    CGContextTranslateCTM(ctx,insets.left+[self.viewport offsetX],insets.top+[self.viewport offsetY]);
    CGContextScaleCTM(ctx,[self.viewport scale],[self.viewport scale]);
    CGContextClipToRect(ctx,CGRectMake(0,0,600,960)); self.painter.context=ctx;
    [self.game renderWithBBPainter:self.painter]; self.painter.context=NULL;
    CGContextRestoreGState(ctx);
}
- (CGPoint)position:(UITouch *)touch {
    CGPoint point=[touch locationInView:self]; UIEdgeInsets insets=self.safeAreaInsets;
    return CGPointMake([self.viewport worldXWithFloat:(jfloat)(point.x-insets.left)],
                       [self.viewport worldYWithFloat:(jfloat)(point.y-insets.top)]);
}
- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    if(self.primary) return; self.primary=touches.anyObject; CGPoint p=[self position:self.primary];
    [self.game downWithFloat:(jfloat)p.x withFloat:(jfloat)p.y];
}
- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    if(!self.primary || ![touches containsObject:self.primary]) return; CGPoint p=[self position:self.primary];
    [self.game moveWithFloat:(jfloat)p.x withFloat:(jfloat)p.y];
}
- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    if(!self.primary || ![touches containsObject:self.primary]) return; CGPoint p=[self position:self.primary];
    [self.game upWithFloat:(jfloat)p.x withFloat:(jfloat)p.y]; self.primary=nil;
}
- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    if(self.primary && [touches containsObject:self.primary]) { [self.game cancel]; self.primary=nil; }
}
@end

@interface BBController : UIViewController
@end
@implementation BBController
- (void)loadView { self.view=[[BBGameView alloc] initWithFrame:UIScreen.mainScreen.bounds]; }
- (BOOL)prefersStatusBarHidden { return YES; }
- (UIInterfaceOrientationMask)supportedInterfaceOrientations { return UIInterfaceOrientationMaskPortrait; }
@end

@interface BBAppDelegate : UIResponder <UIApplicationDelegate>
@property(nonatomic,strong) UIWindow *window;
@end
@implementation BBAppDelegate
- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)options {
    self.window=[[UIWindow alloc] initWithFrame:UIScreen.mainScreen.bounds];
    self.window.rootViewController=[[BBController alloc] init]; [self.window makeKeyAndVisible]; return YES;
}
- (void)applicationWillResignActive:(UIApplication *)application { [(BBGameView *)self.window.rootViewController.view suspend]; }
- (void)applicationDidBecomeActive:(UIApplication *)application { [(BBGameView *)self.window.rootViewController.view resume]; }
@end

int main(int argc,char *argv[]) {
    @autoreleasepool { IOSClass_initialize(); NSString_class_(); return UIApplicationMain(argc,argv,nil,NSStringFromClass(BBAppDelegate.class)); }
}
