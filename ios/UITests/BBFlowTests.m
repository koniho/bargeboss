#import <XCTest/XCTest.h>

@interface BBFlowTests : XCTestCase
@end
@implementation BBFlowTests
- (void)testNativeTouchDeliveryAndLifecycle {
    XCUIApplication *app=[[XCUIApplication alloc] init]; [app launch];
    XCUIElement *stage=app.otherElements[@"river-stage"];
    XCTAssertTrue([stage waitForExistenceWithTimeout:10]);
    [[stage coordinateWithNormalizedOffset:CGVectorMake(.5,688.0/960)] tap];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Working; delivered 0;"]);
    [[stage coordinateWithNormalizedOffset:CGVectorMake(292.0/600,700.0/960)] pressForDuration:12];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Working; delivered 1;"],@"Native hold must deliver the first load: %@",stage.value);
    XCTAttachment *image=[XCTAttachment attachmentWithScreenshot:app.screenshot];
    image.name=@"native-first-delivery"; image.lifetime=XCTAttachmentLifetimeKeepAlways; [self addAttachment:image];
    [[stage coordinateWithNormalizedOffset:CGVectorMake(300.0/600,900.0/960)] tap];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Paused; delivered 1;"]);
    [[stage coordinateWithNormalizedOffset:CGVectorMake(.5,575.0/960)] tap];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Working; delivered 1;"]);
    [XCUIDevice.sharedDevice pressButton:XCUIDeviceButtonHome]; [app activate];
    XCTAssertTrue([(NSString *)stage.value hasPrefix:@"Paused; delivered 1;"],@"Background must pause without resetting delivery");
}
@end
