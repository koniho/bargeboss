#import <XCTest/XCTest.h>
#import <math.h>

// One local mirror of Tutorial.java. Keep native flow code named when lessons are inserted.
typedef NS_ENUM(NSInteger, BBTutorialLesson) {
    BBLessonMove=0, BBLessonFigureEight=1, BBLessonReverse=2, BBLessonSlip=3, BBLessonMap=4, BBLessonChannel=5,
    BBLessonHold=6, BBLessonSteer=7, BBLessonDock=8, BBLessonTie=9,
    BBLessonConnect=10, BBLessonPull=11, BBLessonDouble=12, BBLessonBridge=13,
    BBLessonLink=14, BBLessonTrain=15, BBLessonRelease=16, BBLessonDone=17
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
    double dx=x-boat.x,dy=y-boat.y;
    double sx=dx+dy,sy=.577350269*(dy-dx),screenLength=fmax(.001,hypot(sx,sy));
    CGSize size=[self logicalSize:stage];
    NSInteger side=[self integerInStatus:status tag:@"joystick side"];
    double cx=side==2?size.width-113:113,cy=size.height-171;
    double power=fmax(0,fmin(1,(thrust-26)/60)),travel=power>0?7+56*power:0;
    [[self point:stage x:cx y:cy] pressForDuration:0.05
        thenDragToCoordinate:[self point:stage x:cx+travel*sx/screenLength y:cy+travel*sy/screenLength]
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
    double x=side==2?size.width-113:113,y=size.height-171;
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
    NSUInteger seen=0,helm=0;
    for(int attempt=0;attempt<650;attempt++) {
        NSString *status=(NSString *)stage.value;
        NSInteger lesson=[self integerInStatus:status tag:@"lesson"];
        if(lesson==BBLessonChannel)break;
        XCTAssertTrue(lesson>=BBLessonMove && lesson<=BBLessonMap,@"Opening lesson: %@",status);
        seen|=BBLessonMask((BBTutorialLesson)lesson);
        if([self integerInStatus:status tag:@"instructor teach"]==1){
            CGPoint radio=[self worldPositionInStatus:status tag:@"instructor radio"];
            [[self point:stage x:radio.x y:radio.y] tap];continue;
        }
        if([self integerInStatus:status tag:@"instructor call"]>0){
            if([self integerInStatus:status tag:@"dialogue"]>0){CGSize size=[self logicalSize:stage];
                [[self point:stage x:size.width*.5-fmin(440,(size.width-64)/2)*.5-8 y:size.height-69] tap];}
            [NSThread sleepForTimeInterval:0.5];continue;
        }
        NSInteger phase=[self integerInStatus:status tag:@"helm phase"];
        if(lesson==BBLessonMove)helm|=(1u<<phase);
        CGPoint tug=[self worldPositionInStatus:status tag:@"tug"],target=[self worldPositionInStatus:status tag:@"drive target"];
        if(lesson==BBLessonSlip || lesson==BBLessonMap)target=[self worldPositionInStatus:status tag:@"navigation target"];
        if(lesson==BBLessonMap && [self integerInStatus:status tag:@"map phase"]==2){
            CGSize size=[self logicalSize:stage];NSInteger side=[self integerInStatus:status tag:@"joystick side"];
            double left=side==2?18:size.width-202;
            [[self point:stage x:left+160 y:size.height-26] pressForDuration:.05 thenDragToCoordinate:[self point:stage x:left+24 y:size.height-26]];continue;
        }
        double heading=[self numberInStatus:status tag:@"heading"];
        BOOL reverse=lesson==BBLessonReverse || (lesson==BBLessonMove && phase==3) || (lesson==BBLessonSlip && [self integerInStatus:status tag:@"slip phase"]==2 && [self integerInStatus:status tag:@"slip retry"]==0);
        double base=heading+(reverse?M_PI:0),desired=atan2(target.y-tug.y,target.x-tug.x);
        double error=atan2(sin(desired-base),cos(desired-base));
        // A fresh straight gesture selects gear from the current bow/stern.
        // Turn toward distant targets without accidentally selecting the other gear.
        double angle=base+fmax(-1.0,fmin(1.0,error));
        BOOL slow=lesson==BBLessonMove && phase==0 && [self integerInStatus:status tag:@"helm pace"]==0;
        double distance=slow?45:100;
        CGPoint aim=CGPointMake(tug.x+distance*cos(angle),tug.y+distance*sin(angle));
        [self joystickDrive:stage to:aim status:status minimumTravel:slow?26:55 hold:.45];
    }
    [self waitFor:BBLessonToken(BBLessonChannel) on:stage];
    XCTAssertEqual(seen,(NSUInteger)31,@"Helm and both figure eights must be exercised");
    XCTAssertEqual(helm,(NSUInteger)31,@"Forward, left, right, reverse and rendezvous must be exercised");
    XCTAssertEqual([self integerInStatus:(NSString *)stage.value tag:@"gate"],1,@"Opening cannot skip the canal");
}
- (void)driveTrainingChannel:(XCUIElement *)stage {
    const double xs[]={-1180,-1000,-820,-640,-460,-280,-100,80};
    const double ys[]={500,390,620,390,620,390,600,500};
    for(int attempt=0;attempt<240;attempt++) {
        NSString *status=(NSString *)stage.value;
        if(![status containsString:BBLessonToken(BBLessonChannel)]) break;
        if([self integerInStatus:status tag:@"dialogue"]>0) {
            CGSize size=[self logicalSize:stage];
            [[self point:stage x:size.width*.5-fmin(440,(size.width-64)/2)*.5-8 y:size.height-69] tap];
            [NSThread sleepForTimeInterval:0.5];continue;
        }
        if([self integerInStatus:status tag:@"canal contact"]==1) {
            CGPoint house=[self worldPositionInStatus:status tag:@"bridge house"];
            [[self point:stage x:house.x y:house.y] tap];
            [NSThread sleepForTimeInterval:1.2];continue;
        }
        CGPoint boat=[self worldPositionInStatus:status tag:@"tug"];
        if([self integerInStatus:status tag:@"canal ready"]==0 && boat.x>-1500) {
            [NSThread sleepForTimeInterval:0.5];continue;
        }
        CGPoint teacher=[self worldPositionInStatus:status tag:@"teacher"];
        if([self integerInStatus:status tag:@"canal ready"]==1 && (teacher.x<-1200 || hypot(teacher.x-boat.x,teacher.y-boat.y)<60)) {
            [NSThread sleepForTimeInterval:0.5];continue;
        }
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
