import SwiftUI
import WageJarCore

struct DashboardView: View {
    @EnvironmentObject private var store: SettingsStore

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { timeline in
            let clock = SalaryClock(config: store.settings.salary)
            let status = clock.status(at: timeline.date)
            let earned = clock.earnedToday(at: timeline.date)
            let progress = clock.dayProgress(at: timeline.date)

            ZStack {
                LinearGradient(
                    colors: [BrandTheme.heroStart, BrandTheme.heroEnd],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("app.name")
                                    .font(.title2.bold())
                                Text("home.todayEarned")
                                    .font(.subheadline)
                                    .foregroundStyle(.white.opacity(0.68))
                            }
                            Spacer()
                            Text(status.localizedKey)
                                .font(.caption.weight(.semibold))
                                .padding(.horizontal, 12)
                                .padding(.vertical, 7)
                                .background(.white.opacity(0.12), in: Capsule())
                        }

                        Text(store.settings.hideAmounts ? "¥••••" : SalaryFormatting.yuan(earned))
                            .font(.system(size: 50, weight: .bold, design: .rounded))
                            .monospacedDigit()
                            .contentTransition(.numericText(value: earned))
                            .minimumScaleFactor(0.65)
                            .lineLimit(1)

                        ProgressView(value: progress)
                            .tint(BrandTheme.accent)

                        HStack(spacing: 12) {
                            MetricCard(
                                title: "home.perSecond",
                                value: SalaryFormatting.yuan(clock.centsPerSecond, fractionDigits: 4)
                            )
                            MetricCard(
                                title: "home.dailyPay",
                                value: SalaryFormatting.yuan(clock.dailyPayCents)
                            )
                        }

                        VStack(alignment: .leading, spacing: 14) {
                            Text("home.schedule")
                                .font(.headline)
                            ScheduleRow(
                                title: "settings.workHours",
                                value: "\(SalaryFormatting.clock(minute: store.settings.salary.schedule.startMinute)) - \(SalaryFormatting.clock(minute: store.settings.salary.schedule.endMinute))"
                            )
                            ScheduleRow(
                                title: "settings.lunch",
                                value: "\(SalaryFormatting.clock(minute: store.settings.salary.schedule.lunchStartMinute)) - \(SalaryFormatting.clock(minute: store.settings.salary.schedule.lunchEndMinute))"
                            )
                            if store.settings.salary.schedule.dinnerEnabled {
                                ScheduleRow(
                                    title: "settings.dinner",
                                    value: "\(SalaryFormatting.clock(minute: store.settings.salary.schedule.dinnerStartMinute)) - \(SalaryFormatting.clock(minute: store.settings.salary.schedule.dinnerEndMinute))"
                                )
                            }
                        }
                        .padding(18)
                        .background(.white.opacity(0.1), in: RoundedRectangle(cornerRadius: 22))
                    }
                    .padding(22)
                    .foregroundStyle(.white)
                }
            }
        }
    }
}

private struct MetricCard: View {
    let title: LocalizedStringKey
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.caption)
                .foregroundStyle(.white.opacity(0.68))
            Text(value)
                .font(.headline.monospacedDigit())
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(.white.opacity(0.1), in: RoundedRectangle(cornerRadius: 18))
    }
}

private struct ScheduleRow: View {
    let title: LocalizedStringKey
    let value: String

    var body: some View {
        HStack {
            Text(title)
                .foregroundStyle(.white.opacity(0.72))
            Spacer()
            Text(value)
                .fontWeight(.semibold)
                .monospacedDigit()
        }
    }
}

private extension WorkStatus {
    var localizedKey: LocalizedStringKey {
        switch self {
        case .restDay:
            "status.restDay"
        case .beforeWork:
            "status.beforeWork"
        case .working:
            "status.working"
        case .unpaidBreak:
            "status.break"
        case .finished:
            "status.finished"
        }
    }
}
