import XCTest

final class WageJarUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testOnboardingAddExpenseAndNavigateTabs() {
        let app = XCUIApplication()
        app.launchEnvironment["WAGEJAR_UI_TEST_RESET"] = "1"
        app.launchArguments += ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN"]
        app.launch()

        let salaryField = app.textFields["onboarding.salary"]
        XCTAssertTrue(salaryField.waitForExistence(timeout: 15))
        XCTAssertTrue(app.textFields["onboarding.payday"].exists)

        let finishButton = app.buttons["onboarding.finish"]
        XCTAssertTrue(finishButton.exists)
        finishButton.tap()

        let addExpense = app.buttons["home.addExpense"]
        XCTAssertTrue(addExpense.waitForExistence(timeout: 10))
        addExpense.tap()

        let amountField = app.textFields["quickAdd.amount"]
        XCTAssertTrue(amountField.waitForExistence(timeout: 5))
        amountField.tap()
        amountField.typeText("12.50")

        let saveButton = app.buttons["quickAdd.save"]
        XCTAssertTrue(saveButton.isEnabled)
        saveButton.tap()

        let savedAmount = app.staticTexts
            .matching(NSPredicate(format: "label CONTAINS %@", "12.50"))
            .firstMatch
        XCTAssertTrue(savedAmount.waitForExistence(timeout: 10))

        let dashboardScreenshot = XCTAttachment(screenshot: app.screenshot())
        dashboardScreenshot.name = "WageJar dashboard after adding an expense"
        dashboardScreenshot.lifetime = .keepAlways
        add(dashboardScreenshot)

        selectTab(identifier: "tab.ledger", index: 1, in: app)
        XCTAssertTrue(screen("ledger.screen", in: app).waitForExistence(timeout: 5))
        XCTAssertTrue(
            app.staticTexts
                .matching(NSPredicate(format: "label CONTAINS %@", "12.50"))
                .firstMatch
                .exists
        )

        let ledgerScreenshot = XCTAttachment(screenshot: app.screenshot())
        ledgerScreenshot.name = "WageJar ledger"
        ledgerScreenshot.lifetime = .keepAlways
        add(ledgerScreenshot)

        selectTab(identifier: "tab.stats", index: 2, in: app)
        XCTAssertTrue(screen("stats.screen", in: app).waitForExistence(timeout: 5))

        selectTab(identifier: "tab.settings", index: 3, in: app)
        XCTAssertTrue(screen("settings.screen", in: app).waitForExistence(timeout: 5))
        XCTAssertEqual(app.state, .runningForeground)
    }

    private func selectTab(
        identifier: String,
        index: Int,
        in app: XCUIApplication
    ) {
        let identifiedTab = app.tabBars.buttons[identifier]
        if identifiedTab.waitForExistence(timeout: 2) {
            identifiedTab.tap()
        } else {
            app.tabBars.buttons.element(boundBy: index).tap()
        }
    }

    private func screen(_ identifier: String, in app: XCUIApplication) -> XCUIElement {
        app.descendants(matching: .any)
            .matching(identifier: identifier)
            .firstMatch
    }
}
