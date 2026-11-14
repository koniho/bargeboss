#import <XCTest/XCTest.h>
#import <math.h>

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
    CGSize size=[self logicalSize:stage];
    [[self point:stage x:size.width-32 y:24] tap];
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
- (XCUIElement *)startAndDockFirstLoad:(XCUIApplication *)app {
    app.launchArguments=@[@"--reset-progress-for-ui-tests"]; [app launch];
    XCUIElement *stage=app.otherElements[@"river-stage"];
    XCTAssertTrue([stage waitForExistenceWithTimeout:10]);
    XCTAssertGreaterThan(stage.frame.size.width,stage.frame.size.height);
    [self tapTitle:stage x:770 y:446];
    [self waitFor:@"feeder drops 1;" on:stage];
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
    [self throwLine:stage toX:120 toY:290];
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
    NSArray<NSString *> *parts=[status componentsSeparatedByString:[NSString stringWithFormat:@"; %@ ",tag]];
    XCTAssertEqual(parts.count,2);
    NSArray<NSString *> *pair=[[parts.lastObject componentsSeparatedByString:@";"].firstObject componentsSeparatedByString:@","];
    XCTAssertEqual(pair.count,2);
    return CGPointMake(pair[0].doubleValue,pair[1].doubleValue);
}
- (void)holdCargo:(XCUIElement *)stage duration:(double)duration {
    CGPoint b=[self worldPosition:stage tag:@"cargo"];
    [[self river:stage x:b.x y:b.y] pressForDuration:duration];
}
- (void)driveTrainingChannel:(XCUIElement *)stage {
    const double xs[]={-1180,-1000,-820,-640,-460,-280,-100,80};
    const double ys[]={500,390,620,390,620,390,600,500};
    for(int attempt=0;attempt<150 && [(NSString *)stage.value containsString:@"lesson 2;"];attempt++) {
        CGPoint boat=[self worldPosition:stage tag:@"tug"];
        NSArray<NSString *> *parts=[(NSString *)stage.value componentsSeparatedByString:@"; gate "];
        NSInteger gate=[[parts.lastObject componentsSeparatedByString:@";"].firstObject integerValue];
        if(gate>7) break;
        XCTAssertTrue(gate>=1 && gate<=7);
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
        double dx=tx-boat.x,dy=ty-boat.y,length=fmax(1,hypot(dx,dy));
        [[self river:stage x:boat.x y:boat.y] pressForDuration:0.05
                thenDragToCoordinate:[self river:stage x:boat.x+55*dx/length y:boat.y+55*dy/length]
                withVelocity:500 thenHoldForDuration:0.35];
    }
    [self waitFor:@"lesson 3" on:stage];
}
- (void)drivePracticeTug:(XCUIElement *)stage toX:(double)x toY:(double)y {
    NSString *status=(NSString *)stage.value;
    CGPoint boat=[self worldPositionInStatus:status tag:@"tug"];
    double dx=x-boat.x,dy=y-boat.y,length=fmax(1,hypot(dx,dy));
    [[self river:stage x:boat.x y:boat.y status:status] pressForDuration:0.05
        thenDragToCoordinate:[self river:stage x:boat.x+75*dx/length y:boat.y+75*dy/length status:status]
        withVelocity:500 thenHoldForDuration:0.35];
}
- (void)pushPracticeLoad:(XCUIElement *)stage {
    for(int attempt=0;attempt<80 && [(NSString *)stage.value containsString:@"lesson 3;"];attempt++) {
        CGPoint b=[self worldPosition:stage tag:@"cargo"];
        CGPoint boat=[self worldPosition:stage tag:@"tug"];
        if(boat.x<215) { [self drivePracticeTug:stage toX:240 toY:500];continue; }
        if(hypot(boat.x-b.x,boat.y-b.y)>190) { [self drivePracticeTug:stage toX:360 toY:620];continue; }
        double dx=240-b.x,dy=600-b.y,length=fmax(1,hypot(dx,dy));
        [[self river:stage x:b.x y:b.y] pressForDuration:0.05
            thenDragToCoordinate:[self river:stage x:b.x+90*dx/length y:b.y+90*dy/length]
            withVelocity:500 thenHoldForDuration:3];
    }
    [self waitFor:@"lesson 4" on:stage];
}
- (void)testInteractiveDragAndLassoTutorial {
    XCUIApplication *app=[[XCUIApplication alloc] init];
    app.launchArguments=@[@"--reset-progress-for-ui-tests"]; [app launch];
    XCUIElement *stage=app.otherElements[@"river-stage"];
    XCTAssertTrue([stage waitForExistenceWithTimeout:10]);
    [self tapTitle:stage x:770 y:362]; [self waitFor:@"lesson 0" on:stage];
    CGPoint start=[self worldPosition:stage tag:@"tug"];
    [self riverDrag:stage x:start.x y:start.y toX:start.x+100 toY:start.y hold:0.6];
    [self waitFor:@"lesson 2" on:stage];
    [self driveTrainingChannel:stage];
    [self pushPracticeLoad:stage];
    CGPoint b;
    // HOLD FOR ASSIST now continues directly into docking on the same load.
    // Do not require a transient lesson-five snapshot after a long held touch.
    for(int attempt=0;attempt<20;attempt++) {
        NSString *status=(NSString *)stage.value;
        if(![status containsString:@"lesson 4;"] && ![status containsString:@"lesson 5;"]) break;
        [self holdCargo:stage duration:0.8];
    }
    [self waitFor:@"lesson 6" on:stage];
    b=[self worldPosition:stage tag:@"cargo"];
    // The tutorial quay is 18 world units above the water. Project its bollard
    // at that same height; both screen coordinates still use the shared camera.
    double quayRise=18/(2*0.70710678*0.57735027);
    [self riverDrag:stage x:b.x y:b.y toX:120+quayRise toY:700-quayRise hold:0.2];
    [self waitFor:@"lesson 7" on:stage];
    // Sail to the waiting second barge; neither it nor the tug is respawned.
    // Approach on its upstream side so the next lesson pulls it into open water.
    for(int attempt=0;attempt<80;attempt++) {
        CGPoint load=[self worldPosition:stage tag:@"cargo"];
        CGPoint tug=[self worldPosition:stage tag:@"tug"];
        if(hypot(tug.x-load.x,tug.y-load.y)<=150 && tug.y<=load.y-55) break;
        [self drivePracticeTug:stage toX:load.x-95 toY:load.y-85];
    }
    // Derive both endpoints and the camera from one current status snapshot.
    NSString *towStatus=(NSString *)stage.value;
    b=[self worldPositionInStatus:towStatus tag:@"cargo"];
    CGPoint boat=[self worldPositionInStatus:towStatus tag:@"tug"];
    NSLog(@"Connecting tow: %@",towStatus);
    [[self river:stage x:b.x y:b.y status:towStatus] pressForDuration:0.05
            thenDragToCoordinate:[self river:stage x:boat.x y:boat.y status:towStatus]
            withVelocity:300 thenHoldForDuration:0.1];
    [self waitFor:@"lesson 8" on:stage];
    NSString *pullStatus=(NSString *)stage.value;
    boat=[self worldPositionInStatus:pullStatus tag:@"tug"];
    [[self river:stage x:boat.x y:boat.y status:pullStatus] pressForDuration:0.35
            thenDragToCoordinate:[self river:stage x:boat.x y:boat.y-100 status:pullStatus]
            withVelocity:100 thenHoldForDuration:2.5];
    [self waitFor:@"lesson 9" on:stage];
    b=[self worldPosition:stage tag:@"cargo"];[[self river:stage x:b.x y:b.y] tap];
    [self waitFor:@"lesson 10" on:stage];
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
    // Third challenge remains locked; the board must preserve the prior selection.
    [self tapMenu:stage x:790 y:302];
    XCTAssertTrue([(NSString *)stage.value containsString:@"challenge 0;"]);
    [self tapMenu:stage x:490 y:302]; [self waitFor:@"challenge 1;" on:stage];
    // The frame publishes status only after the native version-change save has run.
    [app terminate]; app.launchArguments=@[]; [app launch];
    stage=app.otherElements[@"river-stage"]; XCTAssertTrue([stage waitForExistenceWithTimeout:10]);
    [self waitFor:@"Ready;" on:stage];
    XCTAssertTrue([(NSString *)stage.value containsString:@"challenge 1;"]);
    XCTAssertTrue([(NSString *)stage.value containsString:@"rank WAKE WRANGLER;"]);
    XCTAssertTrue([(NSString *)stage.value containsString:@"career stars 3;"]);
    XCTAssertTrue([(NSString *)stage.value containsString:@"stripes 2"]);
    [self tapTitle:stage x:770 y:513]; [self waitFor:@"Challenges;" on:stage];
    XCTAttachment *image=[XCTAttachment attachmentWithScreenshot:app.screenshot];
    image.name=@"native-saved-captain-challenges"; image.lifetime=XCTAttachmentLifetimeKeepAlways; [self addAttachment:image];
    [self tapMenu:stage x:150 y:544]; [self waitFor:@"Ready;" on:stage];
    [self tapTitle:stage x:770 y:513]; [self waitFor:@"Challenges;" on:stage];
    [self tapMenu:stage x:790 y:544]; [self waitFor:@"Working;" on:stage];
    XCTAssertTrue([(NSString *)stage.value containsString:@"challenge 1;"]);
}
@end
