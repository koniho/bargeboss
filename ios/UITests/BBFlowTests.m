#import <XCTest/XCTest.h>

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
- (XCUICoordinate *)point:(XCUIElement *)stage x:(double)x y:(double)y {
    return [stage coordinateWithNormalizedOffset:CGVectorMake(x/960,y/600)];
}
- (XCUICoordinate *)river:(XCUIElement *)stage x:(double)x y:(double)y {
    // Same quarter-turn as the shared RiverPainter; physics keeps its original axes.
    return [self point:stage x:16+(y-148)*1.35 y:84+(460-x)*1.35];
}
- (void)tap:(XCUIElement *)stage x:(double)x y:(double)y { [[self point:stage x:x y:y] tap]; }
- (void)riverDrag:(XCUIElement *)stage x:(double)x y:(double)y toX:(double)xx toY:(double)yy hold:(double)hold {
    [[self river:stage x:x y:y] pressForDuration:0.35 thenDragToCoordinate:[self river:stage x:xx y:yy]
                                  withVelocity:100 thenHoldForDuration:hold];
}
- (void)testNativeLassoUnloadingAndLifecycle {
    XCUIApplication *app=[[XCUIApplication alloc] init]; [app launch];
    XCUIElement *stage=app.otherElements[@"river-stage"];
    XCTAssertTrue([stage waitForExistenceWithTimeout:10]);
    XCTAssertGreaterThan(stage.frame.size.width,stage.frame.size.height);
    [self tap:stage x:770 y:446];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Working; delivered 0;"]);
    [[self river:stage x:292 y:700] pressForDuration:4];
    [self waitFor:@"ready to tie 2" on:stage];
    XCTAssertTrue([(NSString *)stage.value containsString:@"delivered 0;"],@"Entering berth cannot score without a line");
    [self riverDrag:stage x:230 y:700 toX:310 toY:640 hold:0.2];
    // A full throw/reel cycle precedes the ready assertion, preventing stale accessibility values.
    [self waitFor:@"ready to tie 2" on:stage after:1];
    XCTAssertTrue([(NSString *)stage.value containsString:@"delivered 0;"],@"A missed throw must retract without scoring");
    [self riverDrag:stage x:230 y:700 toX:176 toY:700 hold:0.2];
    [self waitFor:@"unloading" on:stage];
    XCTAttachment *image=[XCTAttachment attachmentWithScreenshot:app.screenshot];
    image.name=@"native-landscape-crew-unloading"; image.lifetime=XCTAttachmentLifetimeKeepAlways; [self addAttachment:image];
    [self waitFor:@"delivered 1;" on:stage];
    [self tap:stage x:917 y:40];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Paused; delivered 1;"]);
    [self tap:stage x:480 y:332];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Working; delivered 1;"]);
    [XCUIDevice.sharedDevice pressButton:XCUIDeviceButtonHome]; [app activate];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Paused; delivered 1;"],@"Background pauses without resetting delivery");
}
- (void)testInteractiveDragAndLassoTutorial {
    XCUIApplication *app=[[XCUIApplication alloc] init]; [app launch];
    XCUIElement *stage=app.otherElements[@"river-stage"];
    XCTAssertTrue([stage waitForExistenceWithTimeout:10]);
    [self tap:stage x:770 y:362]; [self waitFor:@"lesson 0" on:stage];
    [self riverDrag:stage x:350 y:700 toX:350 toY:600 hold:0.6];
    [self waitFor:@"lesson 1" on:stage];
    [self tap:stage x:661 y:559]; [self waitFor:@"lesson 2" on:stage];
    [[self river:stage x:292 y:700] pressForDuration:3];
    [self waitFor:@"lesson 3" on:stage];
    [self riverDrag:stage x:230 y:700 toX:165 toY:635 hold:0.8];
    [self waitFor:@"lesson 4" on:stage];
    [[self river:stage x:292 y:700] pressForDuration:3];
    [self waitFor:@"lesson 5" on:stage];
    [self riverDrag:stage x:244 y:700 toX:176 toY:700 hold:0.2];
    [self waitFor:@"lesson 6" on:stage];
    XCTAttachment *image=[XCTAttachment attachmentWithScreenshot:app.screenshot];
    image.name=@"native-landscape-tutorial-complete"; image.lifetime=XCTAttachmentLifetimeKeepAlways; [self addAttachment:image];
    [self tap:stage x:770 y:446];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Working; delivered 0;"]);
    XCTAssertFalse([(NSString *)stage.value containsString:@"lesson"]);
}
@end
