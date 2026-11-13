#import <XCTest/XCTest.h>

@interface BBFlowTests : XCTestCase
@end
@implementation BBFlowTests
- (void)waitFor:(NSString *)value on:(XCUIElement *)stage {
    [self expectationForPredicate:[NSPredicate predicateWithFormat:@"value CONTAINS %@",value] evaluatedWithObject:stage handler:nil];
    [self waitForExpectationsWithTimeout:18 handler:nil];
}
- (void)tap:(XCUIElement *)stage x:(double)x y:(double)y {
    [[stage coordinateWithNormalizedOffset:CGVectorMake(x/600,y/960)] tap];
}
- (void)drag:(XCUIElement *)stage x:(double)x y:(double)y toX:(double)xx toY:(double)yy hold:(double)hold {
    XCUICoordinate *start=[stage coordinateWithNormalizedOffset:CGVectorMake(x/600,y/960)];
    XCUICoordinate *end=[stage coordinateWithNormalizedOffset:CGVectorMake(xx/600,yy/960)];
    [start pressForDuration:0.35 thenDragToCoordinate:end withVelocity:100 thenHoldForDuration:hold];
}
- (void)testNativeLassoUnloadingAndLifecycle {
    XCUIApplication *app=[[XCUIApplication alloc] init]; [app launch];
    XCUIElement *stage=app.otherElements[@"river-stage"];
    XCTAssertTrue([stage waitForExistenceWithTimeout:10]);
    [self tap:stage x:300 y:688];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Working; delivered 0;"]);
    [[stage coordinateWithNormalizedOffset:CGVectorMake(293.44/600,600.64/960)] pressForDuration:4];
    [self waitFor:@"ready to tie 2" on:stage];
    XCTAssertTrue([(NSString *)stage.value containsString:@"delivered 0;"],@"Entering berth cannot score without a line");
    [self drag:stage x:200 y:601 toX:280 toY:530 hold:0.2];
    [self waitFor:@"ready to tie 2" on:stage];
    XCTAssertTrue([(NSString *)stage.value containsString:@"delivered 0;"],@"A missed throw must retract without scoring");
    [self drag:stage x:200 y:601 toX:149 toY:601 hold:0.2];
    [self waitFor:@"unloading" on:stage];
    XCTAttachment *image=[XCTAttachment attachmentWithScreenshot:app.screenshot];
    image.name=@"native-crew-unloading"; image.lifetime=XCTAttachmentLifetimeKeepAlways; [self addAttachment:image];
    [self waitFor:@"delivered 1;" on:stage];
    [self tap:stage x:542 y:50];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Paused; delivered 1;"]);
    [self tap:stage x:300 y:575];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Working; delivered 1;"]);
    [XCUIDevice.sharedDevice pressButton:XCUIDeviceButtonHome]; [app activate];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Paused; delivered 1;"],@"Background pauses without resetting delivery");
}
- (void)testInteractiveDragAndLassoTutorial {
    XCUIApplication *app=[[XCUIApplication alloc] init]; [app launch];
    XCUIElement *stage=app.otherElements[@"river-stage"];
    XCTAssertTrue([stage waitForExistenceWithTimeout:10]);
    [self tap:stage x:300 y:608]; [self waitFor:@"lesson 0" on:stage];
    [self drag:stage x:341 y:601 toX:341 toY:519 hold:0.6];
    [self waitFor:@"lesson 1" on:stage];
    [self tap:stage x:110 y:910]; [self waitFor:@"lesson 2" on:stage];
    [[stage coordinateWithNormalizedOffset:CGVectorMake(293.44/600,600.64/960)] pressForDuration:3];
    [self waitFor:@"lesson 3" on:stage];
    [self drag:stage x:200 y:601 toX:147 toY:548 hold:0.8];
    [self waitFor:@"lesson 4" on:stage];
    [[stage coordinateWithNormalizedOffset:CGVectorMake(293.44/600,600.64/960)] pressForDuration:3];
    [self waitFor:@"lesson 5" on:stage];
    [self drag:stage x:220 y:601 toX:149 toY:601 hold:0.2];
    [self waitFor:@"lesson 6" on:stage];
    XCTAttachment *image=[XCTAttachment attachmentWithScreenshot:app.screenshot];
    image.name=@"native-drag-tutorial-complete"; image.lifetime=XCTAttachmentLifetimeKeepAlways; [self addAttachment:image];
    [self tap:stage x:300 y:680];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Working; delivered 0;"]);
    XCTAssertFalse([(NSString *)stage.value containsString:@"lesson"]);
}
@end
