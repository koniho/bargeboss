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
    NSTimeInterval earliest=NSDate.timeIntervalSinceReferenceDate+delay;
    NSPredicate *predicate=[NSPredicate predicateWithBlock:^BOOL(id object,NSDictionary *bindings) {
        return NSDate.timeIntervalSinceReferenceDate>=earliest && [[(XCUIElement *)object value] containsString:value];
    }];
    [self expectationForPredicate:predicate evaluatedWithObject:stage handler:nil];
    [self waitForExpectationsWithTimeout:18 handler:nil];
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
    CGSize size=[self logicalSize:stage];
    double scale=fmin(size.width/688,size.height/320);
    double left=(size.width-688*scale)/2,top=(size.height-320*0.75*scale)/2;
    double worldTop=148,worldRight=460;
    NSArray<NSString *> *sections=[(NSString *)stage.value componentsSeparatedByString:@"; camera "];
    if(sections.count==2) {
        NSArray<NSString *> *values=[sections[1] componentsSeparatedByString:@","];
        if(values.count==5) { left=values[0].doubleValue;top=values[1].doubleValue;scale=values[2].doubleValue;worldTop=values[3].doubleValue;worldRight=values[4].doubleValue; }
    }
    return [self point:stage x:left+(y-worldTop)*scale y:top+(worldRight-x)*scale*0.75];
}
- (void)tapMenu:(XCUIElement *)stage x:(double)x y:(double)y {
    CGSize size=[self logicalSize:stage];
    [[self point:stage x:x+(size.width-960)/2 y:y+(size.height-600)/2] tap];
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
    [[self river:stage x:230 y:290] pressForDuration:0.15 thenDragToCoordinate:[self river:stage x:x y:y]
                                   withVelocity:300 thenHoldForDuration:0.1];
}
- (XCUIElement *)startAndDockFirstLoad:(XCUIApplication *)app {
    app.launchArguments=@[@"--reset-progress-for-ui-tests"]; [app launch];
    XCUIElement *stage=app.otherElements[@"river-stage"];
    XCTAssertTrue([stage waitForExistenceWithTimeout:10]);
    XCTAssertGreaterThan(stage.frame.size.width,stage.frame.size.height);
    [self tapMenu:stage x:770 y:446];
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
    NSArray<NSString *> *parts=[(NSString *)stage.value componentsSeparatedByString:[NSString stringWithFormat:@"; %@ ",tag]];
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
    const double xs[]={200,380,560,740,920,1100,1280,1460};
    const double ys[]={500,390,620,390,620,390,600,500};
    for(int attempt=0;attempt<150 && [(NSString *)stage.value containsString:@"lesson 2;"];attempt++) {
        CGPoint boat=[self worldPosition:stage tag:@"tug"];
        NSArray<NSString *> *parts=[(NSString *)stage.value componentsSeparatedByString:@"; gate "];
        NSInteger gate=[[parts.lastObject componentsSeparatedByString:@";"].firstObject integerValue];
        if(gate>7) break;
        XCTAssertTrue(gate>=1 && gate<=7);
        double ahead=fmin(xs[gate],boat.x+28);int segment=0;
        while(segment<6 && ahead>=xs[segment+1]) segment++;
        double t=fmax(0,fmin(1,(ahead-xs[segment])/(xs[segment+1]-xs[segment])));
        double y=ys[segment]+(ys[segment+1]-ys[segment])*t*t*(3-2*t);
        double dx=ahead-boat.x,dy=y-boat.y,length=fmax(1,hypot(dx,dy));
        [[self river:stage x:boat.x y:boat.y] pressForDuration:0.05
                thenDragToCoordinate:[self river:stage x:boat.x+55*dx/length y:boat.y+55*dy/length]
                withVelocity:500 thenHoldForDuration:0.35];
    }
    [self waitFor:@"lesson 3" on:stage];
}
- (void)pushPracticeLoad:(XCUIElement *)stage {
    for(int attempt=0;attempt<80 && [(NSString *)stage.value containsString:@"lesson 3;"];attempt++) {
        CGPoint b=[self worldPosition:stage tag:@"cargo"];
        double dx=240-b.x,dy=600-b.y,length=fmax(1,hypot(dx,dy));
        [[self river:stage x:b.x y:b.y] pressForDuration:0.05
            thenDragToCoordinate:[self river:stage x:b.x+90*dx/length y:b.y+90*dy/length]
            withVelocity:500 thenHoldForDuration:0.5];
    }
    [self waitFor:@"lesson 4" on:stage];
}
- (void)testInteractiveDragAndLassoTutorial {
    XCUIApplication *app=[[XCUIApplication alloc] init];
    app.launchArguments=@[@"--reset-progress-for-ui-tests"]; [app launch];
    XCUIElement *stage=app.otherElements[@"river-stage"];
    XCTAssertTrue([stage waitForExistenceWithTimeout:10]);
    [self tapMenu:stage x:770 y:362]; [self waitFor:@"lesson 0" on:stage];
    [self riverDrag:stage x:350 y:700 toX:350 toY:600 hold:0.6];
    [self waitFor:@"lesson 2" on:stage];
    [self driveTrainingChannel:stage];
    [self pushPracticeLoad:stage];
    CGPoint b;
    [self holdCargo:stage duration:10];
    [self waitFor:@"lesson 5" on:stage];
    [self holdCargo:stage duration:3];
    [self waitFor:@"lesson 6" on:stage];
    b=[self worldPosition:stage tag:@"cargo"];
    [self riverDrag:stage x:b.x y:b.y toX:120 toY:700 hold:0.2];
    [self waitFor:@"lesson 7" on:stage];
    [[self river:stage x:300 y:690] pressForDuration:0.05 thenDragToCoordinate:[self river:stage x:300 y:570]
                                  withVelocity:300 thenHoldForDuration:0.1];
    [self waitFor:@"lesson 8" on:stage];
    [self riverDrag:stage x:300 y:570 toX:300 toY:470 hold:2.5];
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
    [self tapMenu:stage x:770 y:513]; [self waitFor:@"Challenges;" on:stage];
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
    [self tapMenu:stage x:770 y:513]; [self waitFor:@"Challenges;" on:stage];
    XCTAttachment *image=[XCTAttachment attachmentWithScreenshot:app.screenshot];
    image.name=@"native-saved-captain-challenges"; image.lifetime=XCTAttachmentLifetimeKeepAlways; [self addAttachment:image];
    [self tapMenu:stage x:150 y:544]; [self waitFor:@"Ready;" on:stage];
    [self tapMenu:stage x:770 y:513]; [self waitFor:@"Challenges;" on:stage];
    [self tapMenu:stage x:790 y:544]; [self waitFor:@"Working;" on:stage];
    XCTAssertTrue([(NSString *)stage.value containsString:@"challenge 1;"]);
}
@end
