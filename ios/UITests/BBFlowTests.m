#import <XCTest/XCTest.h>
#import <math.h>

// One local mirror of Tutorial.java. Keep native flow code named when lessons are inserted.
typedef NS_ENUM(NSInteger, BBTutorialLesson) {
    BBLessonMove=0, BBLessonCoast=1, BBLessonReverse=2, BBLessonChannel=3,
    BBLessonHold=4, BBLessonSteer=5, BBLessonDock=6, BBLessonTie=7,
    BBLessonConnect=8, BBLessonPull=9, BBLessonDouble=10, BBLessonBridge=11,
    BBLessonLink=12, BBLessonTrain=13, BBLessonRelease=14, BBLessonDone=15
};
static NSString *BBLessonToken(BBTutorialLesson lesson) {
    return [NSString stringWithFormat:@"lesson %ld;",(long)lesson];
}
static NSUInteger BBLessonMask(BBTutorialLesson lesson) {return (NSUInteger)1<<lesson;}

@interface BBFlowTests : XCTestCase
@end
@implementation BBFlowTests
- (void)setUp {
    [super setUp]; self.continueAfterFailure=NO;
    XCUIDevice.sharedDevice.orientation=UIDeviceOrientationLandscapeLeft;
}
- (void)waitFor:(NSString *)value on:(XCUIElement *)stage after:(NSTimeInterval)delay {
    NSLog(@"Barge Boss wait: expected '%@'; initial status: %@",value,stage.value);
    NSTimeInterval earliest=NSDate.timeIntervalSinceReferenceDate+delay;
    NSPredicate *predicate=[NSPredicate predicateWithBlock:^BOOL(id object,NSDictionary *bindings) {
        return NSDate.timeIntervalSinceReferenceDate>=earliest && [[(XCUIElement *)object value] containsString:value];
    }];
    XCTNSPredicateExpectation *expectation=[[XCTNSPredicateExpectation alloc] initWithPredicate:predicate object:stage];
    XCTWaiterResult result=[XCTWaiter waitForExpectations:@[expectation] timeout:18];
    NSString *actual=(NSString *)stage.value;
    NSLog(@"Barge Boss wait result %ld: expected '%@'; actual status: %@",(long)result,value,actual);
    if(result!=XCTWaiterResultCompleted) {
        XCTAttachment *image=[XCTAttachment attachmentWithScreenshot:stage.screenshot];
        image.name=[NSString stringWithFormat:@"failed-wait-%@",value];
        image.lifetime=XCTAttachmentLifetimeKeepAlways;[self addAttachment:image];
    }
    XCTAssertEqual(result,XCTWaiterResultCompleted,@"Expected '%@'; actual status: %@",value,actual);
}
- (void)waitFor:(NSString *)value on:(XCUIElement *)stage { [self waitFor:value on:stage after:0]; }
// Mirror the small shared Viewport/Layout contract, not fixed device dimensions.
- (CGSize)logicalSize:(XCUIElement *)stage {
    CGSize pixels=stage.frame.size; double scale=fmin(pixels.width/960,pixels.height/600);
    return CGSizeMake(fmax(960,pixels.width/scale),fmax(600,pixels.height/scale));
}
- (XCUICoordinate *)point:(XCUIElement *)stage x:(double)x y:(double)y {
    CGSize size=[self logicalSize:stage];
    return [stage coordinateWithNormalizedOffset:CGVectorMake(x/size.width,y/size.height)];
}
- (XCUICoordinate *)river:(XCUIElement *)stage x:(double)x y:(double)y {
    return [self river:stage x:x y:y status:(NSString *)stage.value];
}
- (XCUICoordinate *)river:(XCUIElement *)stage x:(double)x y:(double)y status:(NSString *)status {
    CGSize size=[self logicalSize:stage];
    double q=0.70710678,depth=0.57735027;
    double scale=fmin(size.width/(1008*q),(size.height-104)/(1008*q*depth));
    double left=(size.width-1008*q*scale)/2,top=(size.height-1008*q*depth*scale)/2;
    double worldTop=148,worldRight=460;
    NSArray<NSString *> *sections=[status componentsSeparatedByString:@"; camera "];
    if(sections.count==2) {
        NSArray<NSString *> *values=[sections[1] componentsSeparatedByString:@","];
        if(values.count==5) { left=values[0].doubleValue;top=values[1].doubleValue;scale=values[2].doubleValue;worldTop=values[3].doubleValue;worldRight=values[4].doubleValue; }
    }
    return [self point:stage x:left+q*((x-worldRight)+(y-worldTop)+320)*scale y:top+q*depth*((y-worldTop)-(x-worldRight))*scale];
}
- (void)tapMenu:(XCUIElement *)stage x:(double)x y:(double)y {
    CGSize size=[self logicalSize:stage];
    [[self point:stage x:x+(size.width-960)/2 y:y+(size.height-600)/2] tap];
}
- (void)tapTitle:(XCUIElement *)stage x:(double)x y:(double)y {
    CGSize size=[self logicalSize:stage];
    [[self point:stage x:x+size.width-960 y:y+(size.height-600)/2] tap];
}
- (void)tapPause:(XCUIElement *)stage {
    // river-stage excludes the safe-area inset; Back starts at the window edge.
    XCUIElement *window=[[XCUIApplication alloc] init].windows.firstMatch;
    [[window coordinateWithNormalizedOffset:CGVectorMake(0.001,0.5)] pressForDuration:0.05
        thenDragToCoordinate:[window coordinateWithNormalizedOffset:CGVectorMake(0.2,0.5)]];
}
- (void)riverDrag:(XCUIElement *)stage x:(double)x y:(double)y toX:(double)xx toY:(double)yy hold:(double)hold {
    [[self river:stage x:x y:y] pressForDuration:0.35 thenDragToCoordinate:[self river:stage x:xx y:yy]
                                  withVelocity:100 thenHoldForDuration:hold];
}
- (void)throwLine:(XCUIElement *)stage toX:(double)x toY:(double)y {
    NSString *status=(NSString *)stage.value;
    CGPoint barge=[self worldPositionInStatus:status tag:@"cargo"];
    [[self river:stage x:barge.x y:barge.y status:status] pressForDuration:0.15
            thenDragToCoordinate:[self river:stage x:x y:y status:status]
            withVelocity:300 thenHoldForDuration:0.1];
}
- (void)throwLineToBollard:(XCUIElement *)stage {
    NSString *status=(NSString *)stage.value;
    [self castFrom:[self worldPositionInStatus:status tag:@"cargo"]
                to:[self worldPositionInStatus:status tag:@"bollard"] stage:stage status:status];
}
- (XCUIElement *)startAndDockFirstLoad:(XCUIApplication *)app {
    app.launchArguments=@[@"--reset-progress-for-ui-tests",@"--practice-yard-for-ui-tests"]; [app launch];
    XCUIElement *stage=app.otherElements[@"river-stage"];
    XCTAssertTrue([stage waitForExistenceWithTimeout:10]);
    XCTAssertGreaterThan(stage.frame.size.width,stage.frame.size.height);
    [self waitFor:@"Working;" on:stage];
    // Accessibility snapshots can arrive after more than one feeder drop.
    // Wait for an available load, not a transient exact counter value.
    NSPredicate *loaded=[NSPredicate predicateWithBlock:^BOOL(id object,NSDictionary *bindings) {
        NSString *status=(NSString *)[(XCUIElement *)object value];
        return [self integerInStatus:status tag:@"feeder drops"]>=1
                && [self integerInStatus:status tag:@"cargo id"]>=0;
    }];
    XCTNSPredicateExpectation *arrival=[[XCTNSPredicateExpectation alloc] initWithPredicate:loaded object:stage];
    XCTAssertEqual([XCTWaiter waitForExpectations:@[arrival] timeout:18],XCTWaiterResultCompleted);
    [[self river:stage x:260 y:730] pressForDuration:15];
    [self waitFor:@"ready to tie 2" on:stage];
    return stage;
}
- (void)testNativeMissRetractsWithoutScoring {
    XCUIApplication *app=[[XCUIApplication alloc] init];
    XCUIElement *stage=[self startAndDockFirstLoad:app];
    [self throwLine:stage toX:310 toY:350];
    [self waitFor:@"Working; delivered 0;" on:stage after:1];
    NSString *status=(NSString *)stage.value;
    XCTAssertFalse([status containsString:@"rope "],@"Missed line finishes retracting");
    XCTAssertFalse([status containsString:@"securing load"] || [status containsString:@"drawing alongside"] || [status containsString:@"unloading"],@"A miss cannot secure or unload cargo");
}
- (void)testNativeLassoUnloadingAndLifecycle {
    XCUIApplication *app=[[XCUIApplication alloc] init];
    // Each outcome gets its own real feeder/push sequence. A previous miss must
    // not leave this test aiming at stale coordinates as unmoored cargo coasts.
    XCUIElement *stage=[self startAndDockFirstLoad:app];
    [self throwLineToBollard:stage];
    [self waitFor:@"unloading" on:stage];
    XCTAttachment *image=[XCTAttachment attachmentWithScreenshot:app.screenshot];
    image.name=@"native-landscape-crew-unloading"; image.lifetime=XCTAttachmentLifetimeKeepAlways; [self addAttachment:image];
    [self waitFor:@"delivered 1;" on:stage];
    [self tapPause:stage];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Paused; delivered 1;"]);
    [self tapMenu:stage x:480 y:332];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Working; delivered 1;"]);
    [XCUIDevice.sharedDevice pressButton:XCUIDeviceButtonHome]; [app activate];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Paused; delivered 1;"],@"Background pauses without resetting delivery");
}
- (CGPoint)worldPosition:(XCUIElement *)stage tag:(NSString *)tag {
    return [self worldPositionInStatus:(NSString *)stage.value tag:tag];
}
- (CGPoint)worldPositionInStatus:(NSString *)status tag:(NSString *)tag {
    NSString *prefix=[tag stringByAppendingString:@" "];
    for(NSString *field in [status componentsSeparatedByString:@"; "]) {
        if(![field hasPrefix:prefix]) continue;
        NSArray<NSString *> *pair=[[field substringFromIndex:prefix.length] componentsSeparatedByString:@","];
        // "cargo id" and "cargo leader" share the prefix but are not positions.
        if(pair.count==2) return CGPointMake(pair[0].doubleValue,pair[1].doubleValue);
    }
    XCTFail(@"Missing coordinate '%@' in status: %@",tag,status);return CGPointZero;
}
- (void)holdCargo:(XCUIElement *)stage duration:(double)duration {
    CGPoint b=[self worldPosition:stage tag:@"cargo"];
    [[self river:stage x:b.x y:b.y] pressForDuration:duration];
}
- (NSInteger)integerInStatus:(NSString *)status tag:(NSString *)tag {
    NSArray<NSString *> *parts=[status componentsSeparatedByString:[NSString stringWithFormat:@"; %@ ",tag]];
    XCTAssertEqual(parts.count,2,@"Missing '%@' in status: %@",tag,status);
    return [[parts.lastObject componentsSeparatedByString:@";"].firstObject integerValue];
}
- (double)numberInStatus:(NSString *)status tag:(NSString *)tag {
    NSArray<NSString *> *parts=[status componentsSeparatedByString:[NSString stringWithFormat:@"; %@ ",tag]];
    XCTAssertEqual(parts.count,2,@"Missing '%@' in status: %@",tag,status);
    return [[parts.lastObject componentsSeparatedByString:@";"].firstObject doubleValue];
}
- (void)drivePracticeTug:(XCUIElement *)stage toX:(double)x toY:(double)y status:(NSString *)status thrust:(double)thrust {
    CGPoint boat=[self worldPositionInStatus:status tag:@"tug"];
    double dx=x-boat.x,dy=y-boat.y,length=fmax(1,hypot(dx,dy));
    // Both endpoints use this same published camera and boat position. In
    // particular, do not re-read accessibility between the two coordinates.
    [[self river:stage x:boat.x y:boat.y status:status] pressForDuration:0.05
        thenDragToCoordinate:[self river:stage x:boat.x+thrust*dx/length y:boat.y+thrust*dy/length status:status]
        withVelocity:500 thenHoldForDuration:0.35];
}
// Tow-course driving also exercises the new native joystick transport. The
// fixed pad is stable while camera pan/zoom and the heavy tow keep moving.
- (void)joystickDrive:(XCUIElement *)stage to:(CGPoint)target status:(NSString *)status {
    [self joystickDrive:stage to:target status:status minimumTravel:0 hold:0];
}
- (void)joystickDrive:(XCUIElement *)stage to:(CGPoint)target status:(NSString *)status minimumTravel:(double)minimum hold:(double)hold {
    CGPoint tug=[self worldPositionInStatus:status tag:@"tug"];
    double dx=target.x-tug.x,dy=target.y-tug.y,distance=hypot(dx,dy);
    double sx=dx+dy,sy=(dy-dx)*0.57735027,length=fmax(1,hypot(sx,sy));
    CGSize size=[self logicalSize:stage];
    NSInteger side=[self integerInStatus:status tag:@"joystick side"];
    XCTAssertTrue(side==1 || side==2,@"Native joystick must be enabled on LEFT or RIGHT: %@",status);
    double x=side==2?size.width-86:86,y=size.height-118;
    double travel=fmax(minimum,distance<20?0:distance<60?30:42);
    [[self point:stage x:x y:y] pressForDuration:0.05
        thenDragToCoordinate:[self point:stage x:x+sx/length*travel y:y+sy/length*travel]
        withVelocity:500 thenHoldForDuration:hold>0?hold:(distance<80?0.6:1.3)];
}
- (void)castFrom:(CGPoint)source to:(CGPoint)target stage:(XCUIElement *)stage status:(NSString *)status {
    [[self river:stage x:source.x y:source.y status:status] pressForDuration:0.05
        thenDragToCoordinate:[self river:stage x:target.x y:target.y status:status]
        withVelocity:300 thenHoldForDuration:0.1];
}
- (void)finishTowCourse:(XCUIElement *)stage {
    NSUInteger seen=0;
    for(int attempt=0;attempt<600;attempt++) {
        NSString *status=(NSString *)stage.value;NSInteger lesson=[self integerInStatus:status tag:@"lesson"];
        if(lesson==BBLessonDone)break;
        XCTAssertTrue(lesson>=BBLessonConnect && lesson<BBLessonDone,@"Unexpected tow lesson: %@",status);seen|=BBLessonMask((BBTutorialLesson)lesson);
        if([self integerInStatus:status tag:@"cast phase"]!=0){[self waitFor:@"; cast phase 0;" on:stage];continue;}
        CGPoint tug=[self worldPositionInStatus:status tag:@"tug"],cargo=[self worldPositionInStatus:status tag:@"cargo"],spare=[self worldPositionInStatus:status tag:@"spare"];
        NSInteger cargoLeader=[self integerInStatus:status tag:@"cargo leader"],spareLeader=[self integerInStatus:status tag:@"spare leader"];
        if(lesson==BBLessonPull || lesson==BBLessonBridge || lesson==BBLessonTrain) {
            [self joystickDrive:stage to:[self worldPositionInStatus:status tag:@"tow target"] status:status];
        } else if(lesson==BBLessonConnect || lesson==BBLessonDouble) {
            CGPoint b=cargoLeader==0?spare:cargo;NSInteger leader=cargoLeader==0?spareLeader:cargoLeader;
            if(leader>=0)[[self river:stage x:b.x y:b.y status:status] tap];
            else if(hypot(tug.x-b.x,tug.y-b.y)>145)[self joystickDrive:stage to:CGPointMake(b.x-65,b.y-85) status:status];
            else [self castFrom:b to:tug stage:stage status:status];
        } else if(lesson==BBLessonLink) {
            if(spareLeader>=0)[[self river:stage x:spare.x y:spare.y status:status] tap];
            else if(cargoLeader<0)[self castFrom:cargo to:tug stage:stage status:status];
            else [self castFrom:spare to:cargo stage:stage status:status];
        } else if(lesson==BBLessonRelease) {
            CGPoint b=cargoLeader>=0?cargo:spare;
            [[self river:stage x:b.x y:b.y status:status] tap];
        }
    }
    [self waitFor:BBLessonToken(BBLessonDone) on:stage];
    NSUInteger required=BBLessonMask(BBLessonPull)|BBLessonMask(BBLessonDouble)|BBLessonMask(BBLessonBridge)
        |BBLessonMask(BBLessonLink)|BBLessonMask(BBLessonTrain)|BBLessonMask(BBLessonRelease);
    XCTAssertEqual(seen & required,required,@"Native gestures visit berth, double tow, bridge, reconfiguration, channel and release lessons");
}
- (void)driveOpenWaterPractice:(XCUIElement *)stage {
    NSInteger previousMarker=0;NSUInteger seenMarkers=0;
    for(int attempt=0;attempt<100;attempt++) {
        NSString *status=(NSString *)stage.value;
        NSInteger marker=[self integerInStatus:status tag:@"drive marker"];
        XCTAssertTrue(marker>=previousMarker && marker<=2,@"Driving markers must remain ordered: %@",status);
        previousMarker=marker;
        if(![status containsString:BBLessonToken(BBLessonMove)]) break;
        XCTAssertLessThan(marker,2,@"Both completed buoy laps should advance into the coast lesson: %@",status);
        seenMarkers|=(1u<<marker);
        CGPoint boat=[self worldPositionInStatus:status tag:@"tug"];
        CGPoint target=[self worldPositionInStatus:status tag:@"drive target"];
        double distance=hypot(target.x-boat.x,target.y-boat.y);
        // Ease thrust near a ring, then inspect real progress before deciding
        // the next gesture. No sleep assumes that a fixed drag reached a goal.
        // Each new press must cross the shared 42-unit steering-engage threshold.
        double thrust=fmax(55,fmin(85,distance*.4));
        NSLog(@"Open-water attempt %d, marker %ld, distance %.1f: %@",attempt,(long)marker,distance,status);
        [self drivePracticeTug:stage toX:target.x toY:target.y status:status thrust:thrust];
    }
    NSString *status=(NSString *)stage.value;
    XCTAssertEqual(seenMarkers,(NSUInteger)3,@"Both buoy orbits must be exercised: %@",status);
    XCTAssertEqual([self integerInStatus:status tag:@"drive marker"],2,@"Opening must circle both buoys: %@",status);
    // Every synthesized drag releases its finger. Stop sending steering once
    // the second goal is reached and wait for the actual coast completion gate.
    // A slow accessibility snapshot may already show REVERSE, so do not require
    // observing the transient COAST card before accepting that completion.
    [self waitFor:BBLessonToken(BBLessonReverse) on:stage];
    [self driveReversePractice:stage];
    XCTAssertEqual([self integerInStatus:(NSString *)stage.value tag:@"gate"],1,@"Open-water practice cannot award a canal gate");
}
- (void)driveReversePractice:(XCUIElement *)stage {
    BOOL began=NO;CGPoint start=CGPointZero,direction=CGPointZero;
    for(int attempt=0;attempt<100;attempt++) {
        NSString *status=(NSString *)stage.value;
        NSInteger lesson=[self integerInStatus:status tag:@"lesson"];
        if(lesson==BBLessonChannel)break;
        XCTAssertEqual(lesson,BBLessonReverse,@"Reverse practice must precede canal navigation: %@",status);
        CGPoint tug=[self worldPositionInStatus:status tag:@"tug"];
        CGPoint target=[self worldPositionInStatus:status tag:@"reverse target"];
        double progress=[self numberInStatus:status tag:@"reverse progress"];
        double distance=hypot(target.x-tug.x,target.y-tug.y);
        XCTAssertTrue(isfinite(progress) && progress>=0 && progress<=1,@"Published reverse progress is bounded: %@",status);
        if([self integerInStatus:status tag:@"reverse armed"]==0) {
            // No reachable stern ring was armed yet. Drive into the same open
            // basin used by the shared replay, then let gameplay choose the ring.
            [self joystickDrive:stage to:CGPointMake(-1430,500) status:status];continue;
        }
        if(!began) {
            XCTAssertLessThan(progress,1,@"Reverse cannot already be credited before its driving gestures");
            XCTAssertGreaterThan(distance,20,@"Reverse starts with a reachable ring behind the tug");
            start=tug;direction=CGPointMake((target.x-tug.x)/distance,(target.y-tug.y)/distance);began=YES;
        }
        // Keep applying astern thrust until the physical gate completes. The
        // ordinary tow driver stops within20units, too early for a short ring.
        // Extending the aim slightly avoids turning toward the bow at the ring.
        if(distance<12 && progress<1)target=CGPointMake(target.x+direction.x*12,target.y+direction.y*12);
        NSLog(@"Reverse attempt %d, progress %.3f, distance %.1f: %@",attempt,progress,distance,status);
        [self joystickDrive:stage to:target status:status minimumTravel:26 hold:0.35];
    }
    [self waitFor:BBLessonToken(BBLessonChannel) on:stage];
    NSString *status=(NSString *)stage.value;CGPoint tug=[self worldPositionInStatus:status tag:@"tug"];
    XCTAssertTrue(began,@"The native test must actually drive the reverse exercise");
    XCTAssertGreaterThanOrEqual([self numberInStatus:status tag:@"reverse progress"],1,@"Physical reverse travel must complete before the canal opens");
    XCTAssertGreaterThan(hypot(tug.x-start.x,tug.y-start.y),30,@"Reverse credit requires real displacement, not only holding a control");
}
- (void)driveTrainingChannel:(XCUIElement *)stage {
    const double xs[]={-1180,-1000,-820,-640,-460,-280,-100,80};
    const double ys[]={500,390,620,390,620,390,600,500};
    for(int attempt=0;attempt<180;attempt++) {
        NSString *status=(NSString *)stage.value;
        if(![status containsString:BBLessonToken(BBLessonChannel)]) break;
        CGPoint boat=[self worldPositionInStatus:status tag:@"tug"];
        NSInteger gate=[self integerInStatus:status tag:@"gate"];
        if(gate>7) break;
        XCTAssertTrue(gate>=1 && gate<=7);
        if(gate==1 && boat.x<xs[0]) {
            // Match the basin-entry leg of TrainingChannel.guide: line up in
            // open water before attempting the narrower mouth at x=-1260.
            double x=boat.x<-1300 && fabs(boat.y-500)>24?-1360:xs[0];
            [self drivePracticeTug:stage toX:x toY:500 status:status thrust:55];
            continue;
        }
        // Follow each horizontal / vertical / horizontal reach of the shared
        // rectilinear course rather than steering through the solid inner corner.
        double turn=(xs[gate-1]+xs[gate])/2;
        double px[]={xs[gate-1],turn,turn,xs[gate]},py[]={ys[gate-1],ys[gate-1],ys[gate],ys[gate]};
        double best=INFINITY,along=0,total=0;
        for(int i=0;i<3;i++) {
            double dx=px[i+1]-px[i],dy=py[i+1]-py[i],length=hypot(dx,dy);
            double t=fmax(0,fmin(1,((boat.x-px[i])*dx+(boat.y-py[i])*dy)/(length*length)));
            double ex=boat.x-px[i]-t*dx,ey=boat.y-py[i]-t*dy,distance=ex*ex+ey*ey;
            if(distance<=best) {best=distance;along=total+t*length;}
            total+=length;
        }
        double target=fmin(total,along+28),tx=xs[gate],ty=ys[gate];
        for(int i=0;i<3;i++) {
            double dx=px[i+1]-px[i],dy=py[i+1]-py[i],length=hypot(dx,dy);
            if(target<=length || i==2) {double t=fmin(1,target/length);tx=px[i]+t*dx;ty=py[i]+t*dy;break;}
            target-=length;
        }
        [self drivePracticeTug:stage toX:tx toY:ty status:status thrust:55];
    }
    [self waitFor:BBLessonToken(BBLessonHold) on:stage];
}
- (void)drivePracticeTug:(XCUIElement *)stage toX:(double)x toY:(double)y {
    NSString *status=(NSString *)stage.value;
    [self drivePracticeTug:stage toX:x toY:y status:status thrust:75];
}
- (void)pushPracticeLoad:(XCUIElement *)stage {
    for(int attempt=0;attempt<80 && [(NSString *)stage.value containsString:BBLessonToken(BBLessonHold)];attempt++) {
        CGPoint b=[self worldPosition:stage tag:@"cargo"];
        CGPoint boat=[self worldPosition:stage tag:@"tug"];
        if(boat.x<215) { [self drivePracticeTug:stage toX:240 toY:500];continue; }
        if(hypot(boat.x-b.x,boat.y-b.y)>190) { [self drivePracticeTug:stage toX:360 toY:620];continue; }
        double dx=240-b.x,dy=600-b.y,length=fmax(1,hypot(dx,dy));
        [[self river:stage x:b.x y:b.y] pressForDuration:0.05
            thenDragToCoordinate:[self river:stage x:b.x+90*dx/length y:b.y+90*dy/length]
            withVelocity:500 thenHoldForDuration:3];
    }
    [self waitFor:BBLessonToken(BBLessonSteer) on:stage];
}
- (void)testInteractiveDragAndLassoTutorial {
    XCUIApplication *app=[[XCUIApplication alloc] init];
    app.launchArguments=@[@"--reset-progress-for-ui-tests"]; [app launch];
    XCUIElement *stage=app.otherElements[@"river-stage"];
    XCTAssertTrue([stage waitForExistenceWithTimeout:10]);
    [self tapTitle:stage x:770 y:362]; [self waitFor:BBLessonToken(BBLessonMove) on:stage];
    [self waitFor:@"; dialogue 3;" on:stage after:0.5];
    CGSize radioSize=[self logicalSize:stage];
    double choiceWidth=fmin(440,(radioSize.width-64)/2);
    [[self point:stage x:radioSize.width/2-choiceWidth/2-8 y:radioSize.height-131] tap];
    [self waitFor:@"; arrival 0;" on:stage];
    [self driveOpenWaterPractice:stage];
    [self driveTrainingChannel:stage];
    [self pushPracticeLoad:stage];
    CGPoint b;
    // HOLD FOR ASSIST now continues directly into docking on the same load.
    // Do not require a transient DOCK snapshot after a long held touch.
    for(int attempt=0;attempt<20;attempt++) {
        NSString *status=(NSString *)stage.value;
        if(![status containsString:BBLessonToken(BBLessonSteer)] && ![status containsString:BBLessonToken(BBLessonDock)]) break;
        [self holdCargo:stage duration:0.8];
    }
    [self waitFor:BBLessonToken(BBLessonTie) on:stage];
    [self throwLineToBollard:stage];
    [self waitFor:BBLessonToken(BBLessonConnect) on:stage];
    // Sail to the waiting second barge; neither it nor the tug is respawned.
    // Approach on its upstream side so the next lesson pulls it into open water.
    for(int attempt=0;attempt<80;attempt++) {
        CGPoint load=[self worldPosition:stage tag:@"cargo"];
        CGPoint tug=[self worldPosition:stage tag:@"tug"];
        if(hypot(tug.x-load.x,tug.y-load.y)<=150 && tug.y<=load.y-55) break;
        [self drivePracticeTug:stage toX:load.x-95 toY:load.y-85];
    }
    // A released drive can still move the hull/camera before XCTest injects a
    // gesture. Retry a missed cast from a fresh shared snapshot, never teleport.
    for(int attempt=0;attempt<6;attempt++) {
        NSString *towStatus=(NSString *)stage.value;
        if([self integerInStatus:towStatus tag:@"lesson"]!=BBLessonConnect)break;
        if([self integerInStatus:towStatus tag:@"cast phase"]==0){
            b=[self worldPositionInStatus:towStatus tag:@"cargo"];
            CGPoint boat=[self worldPositionInStatus:towStatus tag:@"tug"];
            [self castFrom:b to:boat stage:stage status:towStatus];
        }
        [self waitFor:@"; cast phase 0;" on:stage after:0.5];
    }
    [self waitFor:BBLessonToken(BBLessonPull) on:stage];
    [self finishTowCourse:stage];
    XCTAttachment *image=[XCTAttachment attachmentWithScreenshot:app.screenshot];
    image.name=@"native-landscape-tutorial-complete"; image.lifetime=XCTAttachmentLifetimeKeepAlways; [self addAttachment:image];
    [self tapMenu:stage x:770 y:446];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Working; delivered 0;"]);
    XCTAssertFalse([(NSString *)stage.value containsString:@"lesson"]);
}
- (void)testChallengeBoardAndSavedCaptainProgress {
    XCUIApplication *app=[[XCUIApplication alloc] init];
    app.launchArguments=@[@"--reset-progress-for-ui-tests",@"--progress-fixture-for-ui-tests",@"BB1|0|3,1200|0,0|0,0"];
    [app launch]; XCUIElement *stage=app.otherElements[@"river-stage"];
    XCTAssertTrue([stage waitForExistenceWithTimeout:10]);
    [self waitFor:@"rank WAKE WRANGLER" on:stage];
    XCTAssertTrue([(NSString *)stage.value containsString:@"career stars 3;"]);
    XCTAssertTrue([(NSString *)stage.value containsString:@"stripes 2"]);
    [self tapTitle:stage x:770 y:513]; [self waitFor:@"Challenges;" on:stage];
    // Locations are all selectable; old job IDs remain only for career rewards.
    [self tapMenu:stage x:790 y:302];
    [self waitFor:@"challenge 5;" on:stage];
    [self tapMenu:stage x:490 y:302]; [self waitFor:@"challenge 4;" on:stage];
    // The frame publishes status only after the native version-change save has run.
    [app terminate]; app.launchArguments=@[]; [app launch];
    stage=app.otherElements[@"river-stage"]; XCTAssertTrue([stage waitForExistenceWithTimeout:10]);
    [self waitFor:@"Ready;" on:stage];
    XCTAssertTrue([(NSString *)stage.value containsString:@"challenge 4;"]);
    XCTAssertTrue([(NSString *)stage.value containsString:@"rank WAKE WRANGLER;"]);
    XCTAssertTrue([(NSString *)stage.value containsString:@"career stars 3;"]);
    XCTAssertTrue([(NSString *)stage.value containsString:@"stripes 2"]);
    [self tapTitle:stage x:770 y:513]; [self waitFor:@"Challenges;" on:stage];
    XCTAttachment *image=[XCTAttachment attachmentWithScreenshot:app.screenshot];
    image.name=@"native-saved-captain-challenges"; image.lifetime=XCTAttachmentLifetimeKeepAlways; [self addAttachment:image];
    [self tapMenu:stage x:150 y:544]; [self waitFor:@"Ready;" on:stage];
    [self tapTitle:stage x:770 y:513]; [self waitFor:@"Challenges;" on:stage];
    [self tapMenu:stage x:790 y:544]; [self waitFor:@"Working;" on:stage];
    XCTAssertTrue([(NSString *)stage.value containsString:@"challenge 4;"]);
}
@end
