import Foundation

public enum Weekday: Int, CaseIterable, Codable, Sendable {
    case sunday = 1
    case monday = 2
    case tuesday = 3
    case wednesday = 4
    case thursday = 5
    case friday = 6
    case saturday = 7
}

public enum WorkStatus: String, Codable, Sendable {
    case restDay
    case beforeWork
    case working
    case unpaidBreak
    case finished
}

public struct MinuteRange: Codable, Equatable, Sendable {
    public let start: Int
    public let end: Int

    public init(start: Int, end: Int) {
        self.start = start
        self.end = end
    }
}

public struct WorkSchedule: Codable, Equatable, Sendable {
    public static let defaultWorkDays = Set(Weekday.allCases.filter {
        $0 != .saturday && $0 != .sunday
    })

    public var workDays: Set<Weekday>
    public var startMinute: Int
    public var endMinute: Int
    public var lunchStartMinute: Int
    public var lunchEndMinute: Int
    public var lunchPaid: Bool
    public var dinnerEnabled: Bool
    public var dinnerStartMinute: Int
    public var dinnerEndMinute: Int
    public var dinnerPaid: Bool

    public init(
        workDays: Set<Weekday> = WorkSchedule.defaultWorkDays,
        startMinute: Int = 9 * 60,
        endMinute: Int = 18 * 60,
        lunchStartMinute: Int = 12 * 60,
        lunchEndMinute: Int = 13 * 60,
        lunchPaid: Bool = false,
        dinnerEnabled: Bool = false,
        dinnerStartMinute: Int = 18 * 60,
        dinnerEndMinute: Int = 18 * 60 + 30,
        dinnerPaid: Bool = false
    ) {
        self.workDays = workDays
        self.startMinute = startMinute
        self.endMinute = endMinute
        self.lunchStartMinute = lunchStartMinute
        self.lunchEndMinute = lunchEndMinute
        self.lunchPaid = lunchPaid
        self.dinnerEnabled = dinnerEnabled
        self.dinnerStartMinute = dinnerStartMinute
        self.dinnerEndMinute = dinnerEndMinute
        self.dinnerPaid = dinnerPaid
    }

    public var lunchActive: Bool {
        lunchEndMinute > lunchStartMinute &&
            lunchStartMinute >= startMinute &&
            lunchEndMinute <= endMinute
    }

    public var dinnerActive: Bool {
        dinnerEnabled &&
            dinnerEndMinute > dinnerStartMinute &&
            dinnerStartMinute >= startMinute &&
            dinnerEndMinute <= endMinute
    }

    public var paidSegments: [MinuteRange] {
        var unpaidBreaks: [MinuteRange] = []
        if !lunchPaid && lunchActive {
            unpaidBreaks.append(MinuteRange(start: lunchStartMinute, end: lunchEndMinute))
        }
        if !dinnerPaid && dinnerActive {
            unpaidBreaks.append(MinuteRange(start: dinnerStartMinute, end: dinnerEndMinute))
        }
        guard !unpaidBreaks.isEmpty else {
            return [MinuteRange(start: startMinute, end: endMinute)]
        }

        var mergedBreaks: [MinuteRange] = []
        for breakRange in unpaidBreaks.sorted(by: { $0.start < $1.start }) {
            guard let previous = mergedBreaks.last else {
                mergedBreaks.append(breakRange)
                continue
            }
            if breakRange.start > previous.end {
                mergedBreaks.append(breakRange)
            } else {
                mergedBreaks[mergedBreaks.count - 1] = MinuteRange(
                    start: previous.start,
                    end: max(previous.end, breakRange.end)
                )
            }
        }

        var segments: [MinuteRange] = []
        var cursor = startMinute
        for breakRange in mergedBreaks {
            if breakRange.start > cursor {
                segments.append(MinuteRange(start: cursor, end: breakRange.start))
            }
            cursor = max(cursor, breakRange.end)
        }
        if cursor < endMinute {
            segments.append(MinuteRange(start: cursor, end: endMinute))
        }
        return segments
    }

    public var paidSecondsPerDay: Int {
        paidSegments.reduce(0) { total, segment in
            total + max(0, segment.end - segment.start) * 60
        }
    }

    public func isWorkDay(_ date: Date, calendar: Calendar) -> Bool {
        guard let weekday = Weekday(rawValue: calendar.component(.weekday, from: date)) else {
            return false
        }
        return workDays.contains(weekday)
    }
}

public struct SalaryConfig: Codable, Equatable, Sendable {
    public var monthlySalaryCents: Int
    public var paidDaysPerMonth: Double
    public var payday: Int
    public var schedule: WorkSchedule

    public init(
        monthlySalaryCents: Int = 1_000_000,
        paidDaysPerMonth: Double = 21.75,
        payday: Int = 15,
        schedule: WorkSchedule = WorkSchedule()
    ) {
        self.monthlySalaryCents = monthlySalaryCents
        self.paidDaysPerMonth = paidDaysPerMonth
        self.payday = payday
        self.schedule = schedule
    }
}

public struct SalaryClock: Sendable {
    public let config: SalaryConfig

    public init(config: SalaryConfig) {
        self.config = config
    }

    public var paidSecondsPerDay: Int {
        config.schedule.paidSecondsPerDay
    }

    public var dailyPayCents: Double {
        guard config.paidDaysPerMonth > 0 else { return 0 }
        return Double(config.monthlySalaryCents) / config.paidDaysPerMonth
    }

    public var centsPerSecond: Double {
        guard paidSecondsPerDay > 0 else { return 0 }
        return dailyPayCents / Double(paidSecondsPerDay)
    }

    public func status(at date: Date, calendar: Calendar = .current) -> WorkStatus {
        guard config.schedule.isWorkDay(date, calendar: calendar) else {
            return .restDay
        }
        let second = secondOfDay(for: date, calendar: calendar)
        let schedule = config.schedule
        if second < schedule.startMinute * 60 {
            return .beforeWork
        }
        if second >= schedule.endMinute * 60 {
            return .finished
        }
        if !schedule.lunchPaid &&
            schedule.lunchActive &&
            contains(second, rangeStart: schedule.lunchStartMinute, rangeEnd: schedule.lunchEndMinute) {
            return .unpaidBreak
        }
        if !schedule.dinnerPaid &&
            schedule.dinnerActive &&
            contains(second, rangeStart: schedule.dinnerStartMinute, rangeEnd: schedule.dinnerEndMinute) {
            return .unpaidBreak
        }
        return .working
    }

    public func earnedToday(at date: Date, calendar: Calendar = .current) -> Double {
        let dayStart = calendar.startOfDay(for: date)
        return earnedCents(from: dayStart, to: date, calendar: calendar)
    }

    public func earnedCents(
        from: Date,
        to: Date,
        calendar: Calendar = .current
    ) -> Double {
        guard to > from else { return 0 }
        var totalSeconds = 0.0
        var day = calendar.startOfDay(for: from)
        let lastDay = calendar.startOfDay(for: to)

        while day <= lastDay {
            if config.schedule.isWorkDay(day, calendar: calendar) {
                for segment in config.schedule.paidSegments {
                    guard
                        let segmentStart = calendar.date(
                            byAdding: .minute,
                            value: segment.start,
                            to: day
                        ),
                        let segmentEnd = calendar.date(
                            byAdding: .minute,
                            value: segment.end,
                            to: day
                        )
                    else {
                        continue
                    }
                    let overlapStart = max(from, segmentStart)
                    let overlapEnd = min(to, segmentEnd)
                    if overlapEnd > overlapStart {
                        totalSeconds += overlapEnd.timeIntervalSince(overlapStart)
                    }
                }
            }
            guard let nextDay = calendar.date(byAdding: .day, value: 1, to: day) else {
                break
            }
            day = nextDay
        }

        return totalSeconds * centsPerSecond
    }

    public func dayProgress(at date: Date, calendar: Calendar = .current) -> Double {
        let duration = Double(config.schedule.endMinute - config.schedule.startMinute) * 60
        guard duration > 0 else { return 0 }
        let elapsed = Double(secondOfDay(for: date, calendar: calendar)) -
            Double(config.schedule.startMinute * 60)
        return min(max(elapsed / duration, 0), 1)
    }

    private func secondOfDay(for date: Date, calendar: Calendar) -> Int {
        calendar.component(.hour, from: date) * 3600 +
            calendar.component(.minute, from: date) * 60 +
            calendar.component(.second, from: date)
    }

    private func contains(_ second: Int, rangeStart: Int, rangeEnd: Int) -> Bool {
        second >= rangeStart * 60 && second < rangeEnd * 60
    }
}
