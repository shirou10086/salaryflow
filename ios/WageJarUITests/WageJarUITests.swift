import XCTest

final class WageJarUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testAppLaunchesAndShowsOnboarding() {
        let app = XCUIApplication()
        app.launch()

        XCTAssertTrue(
            app.textFields.firstMatch.waitForExistence(timeout: 15),
            "The onboarding form should appear after launch."
        )
        XCTAssertEqual(app.state, .runningForeground)

        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "WageJar simulator"
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }
}
