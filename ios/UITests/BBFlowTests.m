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
- (void)testNativeHelmDeliveryAndLifecycle {
    XCUIApplication *app=[[XCUIApplication alloc] init]; [app launch];
    XCUIElement *stage=app.otherElements[@"river-stage"];
    XCTAssertTrue([stage waitForExistenceWithTimeout:10]);
    [self tap:stage x:300 y:688];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Working; delivered 0;"]);
    [self tap:stage x:110 y:831];
    [self waitFor:@"delivered 1;" on:stage];
    XCTAttachment *image=[XCTAttachment attachmentWithScreenshot:app.screenshot];
    image.name=@"native-helm-first-delivery"; image.lifetime=XCTAttachmentLifetimeKeepAlways; [self addAttachment:image];
    [self tap:stage x:542 y:50];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Paused; delivered 1;"]);
    [self tap:stage x:300 y:575];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Working; delivered 1;"]);
    [XCUIDevice.sharedDevice pressButton:XCUIDeviceButtonHome]; [app activate];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Paused; delivered 1;"],@"Background must pause without resetting delivery");
}
- (void)testInteractiveHelmTutorial {
    XCUIApplication *app=[[XCUIApplication alloc] init]; [app launch];
    XCUIElement *stage=app.otherElements[@"river-stage"];
    XCTAssertTrue([stage waitForExistenceWithTimeout:10]);
    [self tap:stage x:300 y:608]; [self waitFor:@"lesson 0" on:stage];
    [self tap:stage x:110 y:831]; [self waitFor:@"lesson 1" on:stage];
    [[stage coordinateWithNormalizedOffset:CGVectorMake(510.0/600,865.0/960)] pressForDuration:1.5];
    [self waitFor:@"lesson 2" on:stage];
    [self tap:stage x:290 y:908]; [self waitFor:@"lesson 3" on:stage];
    [[stage coordinateWithNormalizedOffset:CGVectorMake(110.0/600,900.0/960)] pressForDuration:1.5];
    [self tap:stage x:290 y:908]; [self waitFor:@"lesson 4" on:stage];
    [self tap:stage x:110 y:831]; [self waitFor:@"lesson 5" on:stage];
    XCTAttachment *image=[XCTAttachment attachmentWithScreenshot:app.screenshot];
    image.name=@"native-tutorial-complete"; image.lifetime=XCTAttachmentLifetimeKeepAlways; [self addAttachment:image];
    [self tap:stage x:300 y:680];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Working; delivered 0;"]);
    XCTAssertFalse([(NSString *)stage.value containsString:@"lesson"]);
}
@end
