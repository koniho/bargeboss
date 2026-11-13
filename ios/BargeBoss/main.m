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
@property(nonatomic,strong) NSMutableArray<UITouch *> *touches;
@property(nonatomic,strong) NSMutableArray<NSNumber *> *touchIDs;
@property(nonatomic) jint nextTouchID;
@property(nonatomic,strong) UISelectionFeedbackGenerator *selection;
@property(nonatomic,strong) UIImpactFeedbackGenerator *contact;
@property(nonatomic,strong) UIImpactFeedbackGenerator *impact;
@property(nonatomic,strong) UINotificationFeedbackGenerator *delivery;
@property(nonatomic) CFTimeInterval lastTime;
@property(nonatomic) jint savedProgressVersion;
@property(nonatomic) jint attemptedProgressVersion;
- (void)suspend;
- (void)resume;
- (void)saveProgress:(BOOL)flush;
@end

@implementation BBGameView
- (instancetype)initWithFrame:(CGRect)frame {
    if((self=[super initWithFrame:frame])) {
        self.backgroundColor=[UIColor colorWithRed:16/255.0 green:30/255.0 blue:37/255.0 alpha:1];
        self.multipleTouchEnabled=YES;
        self.isAccessibilityElement=YES; self.accessibilityIdentifier=@"river-stage";
        self.accessibilityLabel=@"Barge Boss river game";
        self.game=[[BBGame alloc] init]; self.painter=[[BBNativePainter alloc] init];
        @try {
            NSUserDefaults *defaults=NSUserDefaults.standardUserDefaults;
            // CI isolation only; normal launches never remove saved progress.
            NSArray<NSString *> *arguments=NSProcessInfo.processInfo.arguments;
            if([arguments containsObject:@"--reset-progress-for-ui-tests"]) {
                [defaults removeObjectForKey:@"captain-progress"]; [defaults synchronize];
            }
            [self.game restoreProgressWithNSString:[defaults stringForKey:@"captain-progress"]];
            NSUInteger fixture=[arguments indexOfObject:@"--progress-fixture-for-ui-tests"];
            if(fixture!=NSNotFound && fixture+1<arguments.count)
                [self.game restoreProgressWithNSString:arguments[fixture+1]];
        } @catch(NSException *exception) { NSLog(@"Could not read captain progress: %@",exception); }
        self.savedProgressVersion=self.attemptedProgressVersion=[self.game progressVersion];
        self.touches=[NSMutableArray array]; self.touchIDs=[NSMutableArray array];
        self.selection=[[UISelectionFeedbackGenerator alloc] init];
        self.contact=[[UIImpactFeedbackGenerator alloc] initWithStyle:UIImpactFeedbackStyleLight];
        self.impact=[[UIImpactFeedbackGenerator alloc] initWithStyle:UIImpactFeedbackStyleMedium];
        self.delivery=[[UINotificationFeedbackGenerator alloc] init];
        self.viewport=[[BBViewport alloc] initWithFloat:960 withFloat:600];
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
    [self.game configureCanvasWithFloat:[self.viewport logicalWidth] withFloat:[self.viewport logicalHeight]];
    [self.game cancelInputs]; [self.touches removeAllObjects]; [self.touchIDs removeAllObjects];
}
- (void)frame:(CADisplayLink *)link {
    if(self.lastTime>0) [self.game advanceWithDouble:link.timestamp-self.lastTime];
    switch([self.game pollHaptic]) {
        case 1: [self.selection selectionChanged]; break;
        case 2: [self.contact impactOccurred]; break;
        case 3: [self.impact impactOccurred]; break;
        case 4: [self.delivery notificationOccurred:UINotificationFeedbackTypeSuccess]; break;
        default: break;
    }
    [self saveProgress:NO]; self.lastTime=link.timestamp; self.accessibilityValue=[self.game status]; [self setNeedsDisplay];
}
- (void)saveProgress:(BOOL)flush {
    jint version=[self.game progressVersion];
    if(version==self.savedProgressVersion || (!flush && version==self.attemptedProgressVersion)) return;
    self.attemptedProgressVersion=version;
    @try {
        NSUserDefaults *defaults=NSUserDefaults.standardUserDefaults;
        [defaults setObject:[self.game saveProgress] forKey:@"captain-progress"];
        if([defaults synchronize]) self.savedProgressVersion=version;
        else NSLog(@"Could not save captain progress");
    } @catch(NSException *exception) { NSLog(@"Could not save captain progress: %@",exception); }
}
- (void)suspend { [self.game background]; [self saveProgress:YES]; self.accessibilityValue=[self.game status]; [self.touches removeAllObjects]; [self.touchIDs removeAllObjects]; self.lastTime=0; self.displayLink.paused=YES; [self setNeedsDisplay]; }
- (void)resume { self.lastTime=0; self.displayLink.paused=NO; }
- (void)drawRect:(CGRect)rect {
    CGContextRef ctx=UIGraphicsGetCurrentContext(); if(!ctx) return;
    UIEdgeInsets insets=self.safeAreaInsets;
    CGContextSaveGState(ctx);
    CGContextTranslateCTM(ctx,insets.left+[self.viewport offsetX],insets.top+[self.viewport offsetY]);
    CGContextScaleCTM(ctx,[self.viewport zoomFactor],[self.viewport zoomFactor]);
    CGContextClipToRect(ctx,CGRectMake(0,0,[self.viewport logicalWidth],[self.viewport logicalHeight])); self.painter.context=ctx;
    [self.game renderWithBBPainter:self.painter]; self.painter.context=NULL;
    CGContextRestoreGState(ctx);
}
- (CGPoint)position:(UITouch *)touch {
    CGPoint point=[touch locationInView:self]; UIEdgeInsets insets=self.safeAreaInsets;
    return CGPointMake([self.viewport worldXWithFloat:(jfloat)(point.x-insets.left)],
                       [self.viewport worldYWithFloat:(jfloat)(point.y-insets.top)]);
}
- (CGRect)accessibilityFrame {
    UIEdgeInsets insets=self.safeAreaInsets; CGFloat scale=[self.viewport zoomFactor];
    CGRect stage=CGRectMake(insets.left+[self.viewport offsetX],insets.top+[self.viewport offsetY],[self.viewport logicalWidth]*scale,[self.viewport logicalHeight]*scale);
    return UIAccessibilityConvertFrameToScreenCoordinates(stage,self);
}
- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.selection prepare]; [self.contact prepare]; [self.impact prepare]; [self.delivery prepare];
    for(UITouch *touch in touches) {
        jint identifier=self.nextTouchID++;
        [self.touches addObject:touch]; [self.touchIDs addObject:@(identifier)];
        CGPoint p=[self position:touch];
        [self.game pointerDownWithInt:identifier withFloat:(jfloat)p.x withFloat:(jfloat)p.y];
    }
}
- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    for(UITouch *touch in touches) {
        NSUInteger index=[self.touches indexOfObjectIdenticalTo:touch]; if(index==NSNotFound) continue;
        CGPoint p=[self position:touch];
        [self.game pointerMoveWithInt:self.touchIDs[index].intValue withFloat:(jfloat)p.x withFloat:(jfloat)p.y];
    }
}
- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    for(UITouch *touch in touches) {
        NSUInteger index=[self.touches indexOfObjectIdenticalTo:touch]; if(index==NSNotFound) continue;
        CGPoint p=[self position:touch];
        [self.game pointerUpWithInt:self.touchIDs[index].intValue withFloat:(jfloat)p.x withFloat:(jfloat)p.y];
        [self.touches removeObjectAtIndex:index]; [self.touchIDs removeObjectAtIndex:index];
    }
}
- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    for(UITouch *touch in touches) {
        NSUInteger index=[self.touches indexOfObjectIdenticalTo:touch]; if(index==NSNotFound) continue;
        [self.game pointerCancelWithInt:self.touchIDs[index].intValue];
        [self.touches removeObjectAtIndex:index]; [self.touchIDs removeObjectAtIndex:index];
    }
}

@end

@interface BBController : UIViewController
@end
@implementation BBController
- (void)loadView { self.view=[[BBGameView alloc] initWithFrame:UIScreen.mainScreen.bounds]; }
- (BOOL)prefersStatusBarHidden { return YES; }
- (UIInterfaceOrientationMask)supportedInterfaceOrientations { return UIInterfaceOrientationMaskLandscape; }
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
