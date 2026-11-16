#import <UIKit/UIKit.h>
#import <QuartzCore/QuartzCore.h>
#import "BBPainter.h"
#import "BBMetalPainter.h"
#if BB_IOS_PROFILE
#import "com/bargeboss/game/ProfileScenario.h"
#endif
#import "com/bargeboss/game/Game.h"
#import "com/bargeboss/game/FrameGate.h"
#import "com/bargeboss/game/Viewport.h"
#import "IOSClass.h"
#import "NSString+JavaString.h"

@interface BBGameView : UIView
@property(nonatomic,strong) BBGame *game;
@property(nonatomic,strong) BBFrameGate *frames;
@property(nonatomic,strong) BBViewport *viewport;
@property(nonatomic,strong) id<BBPainter> painter;
@property(nonatomic,strong) CADisplayLink *displayLink;
@property(nonatomic,strong) NSMutableArray<UITouch *> *touches;
@property(nonatomic,strong) NSMutableArray<NSNumber *> *touchIDs;
@property(nonatomic) jint nextTouchID;
@property(nonatomic) BOOL automation;
@property(nonatomic,strong) UISelectionFeedbackGenerator *selection;
@property(nonatomic,strong) UIImpactFeedbackGenerator *contact;
@property(nonatomic,strong) UIImpactFeedbackGenerator *impact;
@property(nonatomic,strong) UINotificationFeedbackGenerator *delivery;
@property(nonatomic) CFTimeInterval lastTime;
@property(nonatomic) jint savedProgressVersion;
@property(nonatomic) jint attemptedProgressVersion;
@property(nonatomic) BOOL profiling;
@property(nonatomic) NSInteger profileDraws;
@property(nonatomic) CFTimeInterval profileLastDraw;
@property(nonatomic) double profileLastCost;
@property(nonatomic) NSInteger profileWarmup,profileSamples;
@property(nonatomic) BOOL profileReplay,profileConfigured;
@property(nonatomic,strong) NSString *profileScene;
@property(nonatomic,strong) NSMutableData *profileIntervals;
- (void)suspend;
- (void)resume;
- (void)saveProgress:(BOOL)flush;
- (void)requestRedraw;
@end

@implementation BBGameView
- (instancetype)initWithFrame:(CGRect)frame {
    if((self=[super initWithFrame:frame])) {
        self.backgroundColor=[UIColor colorWithRed:16/255.0 green:30/255.0 blue:37/255.0 alpha:1];
        self.layer.contentsFormat=kCAContentsFormatRGBA8Uint;
        self.multipleTouchEnabled=YES;
        self.isAccessibilityElement=YES; self.accessibilityIdentifier=@"river-stage";
        self.accessibilityLabel=@"Barge Boss river game";
        self.game=[[BBGame alloc] init]; self.frames=[[BBFrameGate alloc] init]; self.painter=[self createPainter];
        @try {
            NSUserDefaults *defaults=NSUserDefaults.standardUserDefaults;
            // CI isolation only; normal launches never remove saved progress.
            NSArray<NSString *> *arguments=NSProcessInfo.processInfo.arguments;
            self.profiling=[arguments containsObject:@"--ios-profile"];
            self.profileWarmup=30;self.profileSamples=120;
            self.profileReplay=[arguments containsObject:@"--ios-replay"];
            for(NSString *arg in arguments) {
                if([arg hasPrefix:@"--ios-scene="]) self.profileScene=[arg substringFromIndex:12];
                if([arg hasPrefix:@"--ios-warmup="]) self.profileWarmup=MAX(0,MIN(1800,[[arg substringFromIndex:13] integerValue]));
                if([arg hasPrefix:@"--ios-frames="]) self.profileSamples=MAX(30,MIN(3600,[[arg substringFromIndex:13] integerValue]));
            }
            if(self.profiling && [arguments containsObject:@"--ios-tutorial"]) [self.game startTutorial];
            if(self.profiling) self.profileIntervals=[NSMutableData dataWithCapacity:120*sizeof(double)];
            self.automation=[arguments containsObject:@"--reset-progress-for-ui-tests"];
            if(self.automation) {
                [defaults removeObjectForKey:@"captain-progress"]; [defaults synchronize];
            }
            if(!self.profiling) [self.game restoreProgressWithNSString:[defaults stringForKey:@"captain-progress"]];
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
- (id<BBPainter>)createPainter {return [[BBNativePainter alloc] init];}
- (void)layoutSubviews {
    [super layoutSubviews]; UIEdgeInsets insets=self.safeAreaInsets;
    self.viewport=[[BBViewport alloc] initWithFloat:(jfloat)(self.bounds.size.width-insets.left-insets.right)
                                        withFloat:(jfloat)(self.bounds.size.height-insets.top-insets.bottom)];
    [self.game configureCanvasWithFloat:[self.viewport logicalWidth] withFloat:[self.viewport logicalHeight]];
#if BB_IOS_PROFILE
    if(self.profiling && self.profileScene && !self.profileConfigured) {
        [BBProfileScenario prepareWithBBGame:self.game withNSString:self.profileScene withFloat:[self.viewport logicalWidth] withFloat:[self.viewport logicalHeight]];
        self.profileConfigured=YES;
    }
#endif
    [self.game cancelInputs]; [self.touches removeAllObjects]; [self.touchIDs removeAllObjects];
    [self requestRedraw];
}
- (void)frame:(CADisplayLink *)link {
    // Opt-in frozen-scene measurement: no saved progress, simulation or input.
    // Draw-start intervals include deferred Core Graphics playback after drawRect.
    if(self.profiling) {[self setNeedsDisplay];return;}
    if(self.lastTime>0) [self.game advanceWithDouble:link.timestamp-self.lastTime];
    switch([self.game pollHaptic]) {
        case 1: [self.selection selectionChanged]; break;
        case 2: [self.contact impactOccurred]; break;
        case 3: [self.impact impactOccurred]; break;
        case 4: [self.delivery notificationOccurred:UINotificationFeedbackTypeSuccess]; break;
        default: break;
    }
    [self saveProgress:NO]; self.lastTime=link.timestamp;
    if([self.frames needsFrameWithBBGame:self.game]) [self setNeedsDisplay];
}
- (void)requestRedraw { [self.frames markDirty]; [self setNeedsDisplay]; }
- (void)saveProgress:(BOOL)flush {
    if(self.profiling) return;
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
- (void)suspend { [self.game background]; [self saveProgress:YES]; self.accessibilityValue=self.automation?[self.game automationStatus]:[self.game status]; [self.touches removeAllObjects]; [self.touchIDs removeAllObjects]; self.lastTime=0; self.displayLink.paused=YES; [self requestRedraw]; }
- (void)resume { self.lastTime=0; self.displayLink.paused=NO; [self requestRedraw]; }
- (void)recordProfileFrame {
    if(self.profiling) {
        CFTimeInterval now=CACurrentMediaTime();
        if(self.profileDraws>self.profileWarmup && self.profileDraws<=self.profileWarmup+self.profileSamples) {
            double interval=(now-self.profileLastDraw)*1000;
            double gpu=[self.painter isKindOfClass:BBMetalPainter.class]?((BBMetalPainter *)self.painter).gpuMilliseconds:0;
            double row[]={interval,self.profileLastCost,gpu};
            [self.profileIntervals appendBytes:row length:sizeof(row)];
        }
        self.profileLastDraw=now;self.profileDraws++;
        if(self.profileDraws==self.profileWarmup+self.profileSamples+1) {
            self.displayLink.paused=YES;
            NSData *intervals=[self.profileIntervals copy];
            NSDictionary *diagnostics=[self.painter isKindOfClass:BBMetalPainter.class]?[(BBMetalPainter *)self.painter diagnostics]:@{};
            dispatch_async(dispatch_get_global_queue(QOS_CLASS_UTILITY,0), ^{
                NSMutableString *csv=[NSMutableString stringWithString:@"interval_ms,draw_ms,gpu_ms\n"];
                const double *values=intervals.bytes;
                for(NSUInteger i=0;i<intervals.length/sizeof(double);i+=3) [csv appendFormat:@"%.4f,%.4f,%.4f\n",values[i],values[i+1],values[i+2]];
                NSURL *directory=[NSFileManager.defaultManager URLsForDirectory:NSDocumentDirectory inDomains:NSUserDomainMask].firstObject;
                [[NSJSONSerialization dataWithJSONObject:diagnostics options:NSJSONWritingPrettyPrinted error:nil] writeToURL:[directory URLByAppendingPathComponent:@"ios-profile-metadata.json"] atomically:YES];
                [csv writeToURL:[directory URLByAppendingPathComponent:@"ios-profile.csv"] atomically:YES encoding:NSUTF8StringEncoding error:nil];
            });
        }
        if(self.profileReplay) {
#if BB_IOS_PROFILE
            if(self.profileScene) [BBProfileScenario replayTickWithBBGame:self.game];
            else
#endif
                [self.game advanceWithDouble:1.0/60];
        }
    }
}
- (void)drawRect:(CGRect)rect {
    CFTimeInterval renderStart=self.profiling?CACurrentMediaTime():0;
    [self recordProfileFrame];
    CGContextRef ctx=UIGraphicsGetCurrentContext(); if(!ctx) return;
    UIEdgeInsets insets=self.safeAreaInsets;
    CGContextSaveGState(ctx);
    CGContextTranslateCTM(ctx,insets.left+[self.viewport offsetX],insets.top+[self.viewport offsetY]);
    CGContextScaleCTM(ctx,[self.viewport zoomFactor],[self.viewport zoomFactor]);
    CGContextClipToRect(ctx,CGRectMake(0,0,[self.viewport logicalWidth],[self.viewport logicalHeight])); ((BBNativePainter *)self.painter).context=ctx;
    [self.game renderWithBBPainter:self.painter]; ((BBNativePainter *)self.painter).context=NULL;
    CGContextRestoreGState(ctx);
    self.accessibilityValue=self.automation?[self.game automationStatus]:[self.game status];
    [self.frames renderedWithBBGame:self.game];
    if(self.profiling) self.profileLastCost=(CACurrentMediaTime()-renderStart)*1000;
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
    if(self.profiling) return;
    [self.selection prepare]; [self.contact prepare]; [self.impact prepare]; [self.delivery prepare];
    for(UITouch *touch in touches) {
        jint identifier=self.nextTouchID++;
        [self.touches addObject:touch]; [self.touchIDs addObject:@(identifier)];
        CGPoint p=[self position:touch];
        [self.game pointerDownWithInt:identifier withFloat:(jfloat)p.x withFloat:(jfloat)p.y];
    }
    [self requestRedraw];
}
- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    for(UITouch *touch in touches) {
        NSUInteger index=[self.touches indexOfObjectIdenticalTo:touch]; if(index==NSNotFound) continue;
        CGPoint p=[self position:touch];
        [self.game pointerMoveWithInt:self.touchIDs[index].intValue withFloat:(jfloat)p.x withFloat:(jfloat)p.y];
    }
    [self requestRedraw];
}
- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    for(UITouch *touch in touches) {
        NSUInteger index=[self.touches indexOfObjectIdenticalTo:touch]; if(index==NSNotFound) continue;
        CGPoint p=[self position:touch];
        [self.game pointerUpWithInt:self.touchIDs[index].intValue withFloat:(jfloat)p.x withFloat:(jfloat)p.y];
        [self.touches removeObjectAtIndex:index]; [self.touchIDs removeObjectAtIndex:index];
    }
    [self requestRedraw];
}
- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    for(UITouch *touch in touches) {
        NSUInteger index=[self.touches indexOfObjectIdenticalTo:touch]; if(index==NSNotFound) continue;
        [self.game pointerCancelWithInt:self.touchIDs[index].intValue];
        [self.touches removeObjectAtIndex:index]; [self.touchIDs removeObjectAtIndex:index];
    }
    [self requestRedraw];
}

@end

@interface BBMetalGameView : BBGameView
@end
@implementation BBMetalGameView
+ (Class)layerClass {return CAMetalLayer.class;}
- (id<BBPainter>)createPainter {
    BBMetalPainter *painter=[[BBMetalPainter alloc] init];
    CAMetalLayer *layer=(CAMetalLayer *)self.layer;
    layer.device=painter.device;layer.pixelFormat=MTLPixelFormatBGRA8Unorm;layer.framebufferOnly=YES;
    layer.maximumDrawableCount=3;
    return painter;
}
- (void)layoutSubviews {
    [super layoutSubviews];
    ((CAMetalLayer *)self.layer).drawableSize=CGSizeMake(self.bounds.size.width*self.contentScaleFactor,self.bounds.size.height*self.contentScaleFactor);
}
- (void)displayLayer:(CALayer *)layer {
    BBMetalPainter *painter=(BBMetalPainter *)self.painter;
    UIEdgeInsets insets=self.safeAreaInsets;CGFloat density=self.contentScaleFactor;
    CGPoint offset=CGPointMake((insets.left+[self.viewport offsetX])*density,(insets.top+[self.viewport offsetY])*density);
    CFTimeInterval start=self.profiling?CACurrentMediaTime():0;
    if(![painter startLayer:(CAMetalLayer *)layer scale:[self.viewport zoomFactor]*density offset:offset
                      bounds:CGSizeMake([self.viewport logicalWidth],[self.viewport logicalHeight])]) return;
    [self recordProfileFrame];
    [self.game renderWithBBPainter:painter];[painter present];
    self.accessibilityValue=self.automation?[self.game automationStatus]:[self.game status];
    [self.frames renderedWithBBGame:self.game];
    if(self.profiling) self.profileLastCost=(CACurrentMediaTime()-start)*1000;
}
@end

@interface BBController : UIViewController
@end
@implementation BBController
- (void)loadView {
    NSArray *args=NSProcessInfo.processInfo.arguments;
    Class viewClass=![args containsObject:@"--ios-coregraphics"] && MTLCreateSystemDefaultDevice()?BBMetalGameView.class:BBGameView.class;
    self.view=[[viewClass alloc] initWithFrame:UIScreen.mainScreen.bounds];
}
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
