import XCTest
@testable import WageJarCore

final class SalaryClockTests: XCTestCase {
    private var calendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 0)!
        return calendar
    }

    private var defaultConfig: SalaryConfig {
        SalaryConfig(
            monthlySalaryCents: 1_000_000,
            paidDaysPerMonth: 21.75,
            payday: 15
        )
    }

    func testDefaultScheduleExcludesUnpaidLunch() {
        let clock = SalaryClock(config: defaultConfig)
        XCTAssertEqual(clock.paidSecondsPerDay, 28_800)
    }

    func testUnpaidDinnerIsExcluded() {
        var schedule = WorkSchedule()
        schedule.endMinute = 20 * 60
        schedule.dinnerEnabled = true
        schedule.dinnerStartMinute = 18 * 60
        schedule.dinnerEndMinute = 19 * 60
        let clock = SalaryClock(
            config: SalaryConfig(schedule: schedule)
        )

        XCTAssertEqual(clock.paidSecondsPerDay, 32_400)
        XCTAssertEqual(
            clock.status(at: date(day: 19, hour: 18, minute: 30)),
            .unpaidBreak
        )
    }

    func testPaidDinnerKeepsCounting() {
        var schedule = WorkSchedule()
        schedule.endMinute = 20 * 60
        schedule.dinnerEnabled = true
        schedule.dinnerStartMinute = 18 * 60
        schedule.dinnerEndMinute = 19 * 60
        schedule.dinnerPaid = true
        let clock = SalaryClock(
            config: SalaryConfig(schedule: schedule)
        )

        XCTAssertEqual(clock.paidSecondsPerDay, 36_000)
        XCTAssertEqual(
            clock.status(at: date(day: 19, hour: 18, minute: 30)),
            .working
        )
    }

    func testOverlappingBreaksAreDeductedOnce() {
        var schedule = WorkSchedule()
        schedule.endMinute = 20 * 60
        schedule.dinnerEnabled = true
        schedule.dinnerStartMinute = 12 * 60 + 30
        schedule.dinnerEndMinute = 13 * 60 + 30
        let clock = SalaryClock(
            config: SalaryConfig(schedule: schedule)
        )

        XCTAssertEqual(clock.paidSecondsPerDay, 34_200)
        XCTAssertEqual(
            schedule.paidSegments,
            [
                MinuteRange(start: 9 * 60, end: 12 * 60),
                MinuteRange(start: 13 * 60 + 30, end: 20 * 60),
            ]
        )
    }

    func testDisabledDinnerDoesNotChangePaidTime() {
        var schedule = WorkSchedule()
        schedule.dinnerEnabled = false
        let clock = SalaryClock(
            config: SalaryConfig(schedule: schedule)
        )

        XCTAssertEqual(clock.paidSecondsPerDay, 28_800)
    }

    func testLedgerBalanceIncludesIncomeExpenseAndCalibration() {
        let transactions = [
            LedgerTransaction(type: .income, amountCents: 10_000, category: "salary"),
            LedgerTransaction(type: .expense, amountCents: 3_000, category: "food"),
            LedgerTransaction(
                type: .savingIn,
                amountCents: 500,
                category: LedgerMath.balanceCalibrationCategory
            ),
            LedgerTransaction(
                type: .savingOut,
                amountCents: 200,
                category: LedgerMath.balanceCalibrationCategory
            ),
            LedgerTransaction(type: .savingIn, amountCents: 9_999, category: "manual_saving"),
        ]

        XCTAssertEqual(LedgerMath.savingsCents(transactions: transactions), 7_300)
    }

    private func date(day: Int, hour: Int, minute: Int) -> Date {
        calendar.date(
            from: DateComponents(
                year: 2026,
                month: 10,
                day: day,
                hour: hour,
                minute: minute
            )
        )!
    }
}
