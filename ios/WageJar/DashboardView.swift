import SwiftUI
import WageJarCore

struct DashboardView: View {
    @EnvironmentObject private var store: SettingsStore
    @EnvironmentObject private var ledger: LedgerStore

    let onQuickAdd: (TransactionKind) -> Void
    let onOpenLedger: () -> Void
    let onCalibrate: () -> Void

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { timeline in
            let clock = SalaryClock(config: store.settings.salary)
            let status = clock.status(at: timeline.date)
            let earned = clock.earnedToday(at: timeline.date)
            let progress = clock.dayProgress(at: timeline.date)

            ScrollView {
                VStack(spacing: 16) {
                    topBar
                    hero(
                        clock: clock,
                        status: status,
                        earned: earned,
                        progress: progress
                    )
                    quickActions
                    monthlyOverview
                    recentTransactions
                }
                .padding(.horizontal, 18)
                .padding(.bottom, 110)
            }
            .background(BrandTheme.groupedBackground.ignoresSafeArea())
            .accessibilityIdentifier("home.screen")
            .navigationTitle("tab.home")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        var settings = store.settings
                        settings.hideAmounts.toggle()
                        store.settings = settings
                    } label: {
                        Image(
                            systemName: store.settings.hideAmounts
                                ? "eye.slash"
                                : "eye"
                        )
                    }
                    .accessibilityLabel(
                        store.settings.hideAmounts
                            ? "home.showAmounts"
                            : "home.hideAmounts"
                    )
                }
            }
        }
    }

    private var topBar: some View {
        HStack {
            VStack(alignment: .leading, spacing: 3) {
                Text("app.name")
                    .font(.title2.bold())
                Text(
                    "\(SalaryFormatting.day(Date())) · \(SalaryFormatting.weekday(Date()))"
                )
                .font(.caption)
                .foregroundStyle(.secondary)
            }
            Spacer()
            Image(systemName: "banknote.fill")
                .font(.body.weight(.semibold))
                .foregroundStyle(BrandTheme.savings)
                .frame(width: 40, height: 40)
                .background(BrandTheme.card, in: Circle())
        }
    }

    private func hero(
        clock: SalaryClock,
        status: WorkStatus,
        earned: Double,
        progress: Double
    ) -> some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack {
                Text(LocalizedStringKey(status.localizedKey))
                    .font(.caption.weight(.semibold))
                    .padding(.horizontal, 11)
                    .padding(.vertical, 6)
                    .background(.white.opacity(0.12), in: Capsule())
                Spacer()
                Group {
                    if status == .restDay {
                        Text("home.dayOff")
                    } else {
                        Text(
                            String(
                                format: String(localized: "home.daysToPayday"),
                                daysToPayday()
                            )
                        )
                    }
                }
                .font(.caption)
                .foregroundStyle(BrandTheme.accent)
            }

            VStack(alignment: .leading, spacing: 3) {
                Text("home.earnedToday")
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.68))
                AmountText(
                    cents: earned,
                    hidden: store.settings.hideAmounts,
                    font: .system(size: 50, weight: .bold, design: .rounded),
                    color: .white
                )
            }

            HStack {
                Text("\(String(localized: "home.perSecond")) \(SalaryFormatting.yuan(clock.centsPerSecond, fractionDigits: 4))")
                Spacer()
                Text("\(String(localized: "home.dailyPay")) \(SalaryFormatting.yuan(clock.dailyPayCents))")
            }
            .font(.caption)
            .foregroundStyle(BrandTheme.accent)

            VStack(spacing: 7) {
                GeometryReader { proxy in
                    ZStack(alignment: .leading) {
                        Capsule().fill(.white.opacity(0.16))
                        Capsule()
                            .fill(BrandTheme.accent)
                            .frame(width: proxy.size.width * min(max(progress, 0), 1))
                    }
                }
                .frame(height: 6)
                HStack {
                    Text(SalaryFormatting.clock(minute: clock.config.schedule.startMinute))
                    Spacer()
                    Text(SalaryFormatting.clock(minute: clock.config.schedule.endMinute))
                }
                .font(.caption2)
                .foregroundStyle(.white.opacity(0.65))
            }

            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("home.dayProgress")
                        .font(.caption2)
                        .foregroundStyle(.white.opacity(0.6))
                    Text("\(Int(progress * 100))%")
                        .font(.title3.bold())
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("home.schedule")
                        .font(.caption2)
                        .foregroundStyle(.white.opacity(0.6))
                    Text(
                        "\(SalaryFormatting.clock(minute: clock.config.schedule.startMinute))–\(SalaryFormatting.clock(minute: clock.config.schedule.endMinute))"
                    )
                    .font(.subheadline.weight(.semibold))
                }
            }
        }
        .foregroundStyle(.white)
        .padding(20)
        .background(
            LinearGradient(
                colors: [BrandTheme.heroStart, BrandTheme.heroEnd],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            ),
            in: RoundedRectangle(cornerRadius: 24, style: .continuous)
        )
    }

    private var quickActions: some View {
        HStack(spacing: 12) {
            quickButton(
                titleKey: "quickAdd.expense",
                symbol: "arrow.down",
                color: BrandTheme.expense
            ) {
                onQuickAdd(.expense)
            }
            quickButton(
                titleKey: "quickAdd.income",
                symbol: "arrow.up",
                color: BrandTheme.savings
            ) {
                onQuickAdd(.income)
            }
        }
    }

    private func quickButton(
        titleKey: String,
        symbol: String,
        color: Color,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: symbol)
                    .font(.headline)
                    .frame(width: 36, height: 36)
                    .background(.white.opacity(0.15), in: RoundedRectangle(cornerRadius: 12))
                Text(LocalizedStringKey(titleKey))
                    .font(.headline)
                Spacer()
            }
            .foregroundStyle(.white)
            .padding(14)
            .background(color, in: RoundedRectangle(cornerRadius: 19))
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier(
            titleKey == "quickAdd.expense"
                ? "home.addExpense"
                : "home.addIncome"
        )
    }

    private var monthlyOverview: some View {
        let month = Date()
        let expense = ledger.expenseCents(in: month)
        let income = ledger.incomeCents(in: month)
        let goalProgress = store.settings.goalAmountCents > 0
            ? min(max(Double(ledger.savingsCents) / Double(store.settings.goalAmountCents), 0), 1)
            : 0

        return SectionCard {
            VStack(alignment: .leading, spacing: 15) {
                HStack {
                    VStack(alignment: .leading, spacing: 3) {
                        Text("home.monthlyOverview")
                            .font(.headline)
                        Text(SalaryFormatting.monthYear(month))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    Button("home.adjustSavings") {
                        onCalibrate()
                    }
                    .font(.caption.weight(.semibold))
                }

                HStack(spacing: 12) {
                    SummaryTile(
                        titleKey: "home.savings",
                        cents: ledger.savingsCents,
                        color: BrandTheme.savings,
                        hidden: store.settings.hideAmounts
                    )
                    SummaryTile(
                        titleKey: "home.todayExpense",
                        cents: ledger.todayExpenseCents(),
                        color: BrandTheme.expense,
                        hidden: store.settings.hideAmounts
                    )
                    SummaryTile(
                        titleKey: "ledger.monthBalance",
                        cents: income - expense,
                        color: BrandTheme.chartBlue,
                        hidden: store.settings.hideAmounts
                    )
                }

                VStack(alignment: .leading, spacing: 7) {
                    HStack {
                        Text(
                            store.settings.goalName.isEmpty
                                ? String(localized: "home.defaultGoal")
                                : store.settings.goalName
                        )
                        .font(.subheadline.weight(.medium))
                        Spacer()
                        Text("\(Int(goalProgress * 100))%")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    ProgressView(value: goalProgress)
                        .tint(BrandTheme.savings)
                }
            }
        }
    }

    private var recentTransactions: some View {
        SectionCard {
            VStack(alignment: .leading, spacing: 13) {
                HStack {
                    Text("home.recent")
                        .font(.headline)
                    Spacer()
                    Button("home.viewLedger") {
                        onOpenLedger()
                    }
                    .font(.caption.weight(.semibold))
                }
                if ledger.transactions.isEmpty {
                    Text("home.noTransactions")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .padding(.vertical, 12)
                } else {
                    ForEach(Array(ledger.transactions.prefix(4))) { transaction in
                        TransactionRow(
                            transaction: transaction,
                            hidden: store.settings.hideAmounts
                        )
                        if transaction.id != ledger.transactions.prefix(4).last?.id {
                            Divider().padding(.leading, 50)
                        }
                    }
                }
            }
        }
    }

    private func daysToPayday(calendar: Calendar = .current) -> Int {
        let today = calendar.startOfDay(for: Date())
        let dayCount = calendar.range(of: .day, in: .month, for: today)?.count ?? 28
        let payday = min(store.settings.salary.payday, dayCount)
        let thisMonth = calendar.date(bySetting: .day, value: payday, of: today) ?? today
        if thisMonth >= today {
            return calendar.dateComponents([.day], from: today, to: thisMonth).day ?? 0
        }
        guard let nextMonth = calendar.date(byAdding: .month, value: 1, to: today) else {
            return 0
        }
        let nextPayday = calendar.date(bySetting: .day, value: payday, of: nextMonth) ?? nextMonth
        return calendar.dateComponents([.day], from: today, to: nextPayday).day ?? 0
    }
}

private extension WorkStatus {
    var localizedKey: String {
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
