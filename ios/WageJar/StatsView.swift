import SwiftUI

struct StatsView: View {
    @EnvironmentObject private var settingsStore: SettingsStore
    @EnvironmentObject private var ledger: LedgerStore

    private var hidden: Bool {
        settingsStore.settings.hideAmounts
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {
                    header
                    summaryCard
                    expenseChart
                    categoryCard
                    savingsCard
                }
                .padding(.horizontal, 18)
                .padding(.bottom, 110)
            }
            .background(BrandTheme.groupedBackground.ignoresSafeArea())
            .accessibilityIdentifier("stats.screen")
            .navigationTitle("stats.title")
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text("stats.monthlyReport")
                .font(.title2.bold())
            Text(SalaryFormatting.monthYear(Date()))
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.top, 4)
    }

    private var summaryCard: some View {
        SectionCard {
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 12) {
                    SummaryTile(
                        titleKey: "ledger.monthExpense",
                        cents: ledger.expenseCents(in: Date()),
                        color: BrandTheme.expense,
                        hidden: hidden
                    )
                    SummaryTile(
                        titleKey: "ledger.monthIncome",
                        cents: ledger.incomeCents(in: Date()),
                        color: BrandTheme.savings,
                        hidden: hidden
                    )
                    SummaryTile(
                        titleKey: "stats.balance",
                        cents: ledger.incomeCents(in: Date()) - ledger.expenseCents(in: Date()),
                        color: BrandTheme.chartBlue,
                        hidden: hidden
                    )
                }
                Divider()
                Text(
                    hidden
                        ? String(localized: "stats.summaryHidden")
                        : String(
                            format: String(localized: "stats.summary"),
                            SalaryFormatting.yuan(
                                ledger.incomeCents(in: Date()) - ledger.expenseCents(in: Date())
                            ),
                            SalaryFormatting.yuan(ledger.savingsCents)
                        )
                )
                .font(.subheadline)
                .foregroundStyle(.secondary)
            }
        }
    }

    private var expenseChart: some View {
        let points = ledger.recentMonths()
        return SectionCard {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("stats.sixMonthExpense")
                        .font(.headline)
                    Text("stats.sixMonthExpenseHint")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                HStack(alignment: .bottom, spacing: 10) {
                    ForEach(points) { point in
                        VStack(spacing: 7) {
                            GeometryReader { proxy in
                                let maxValue = max(points.map(\.expenseCents).max() ?? 1, 1)
                                let ratio = CGFloat(point.expenseCents) / CGFloat(maxValue)
                                RoundedRectangle(cornerRadius: 6)
                                    .fill(
                                        LinearGradient(
                                            colors: [BrandTheme.expense, BrandTheme.expense.opacity(0.45)],
                                            startPoint: .top,
                                            endPoint: .bottom
                                        )
                                    )
                                    .frame(
                                        width: proxy.size.width,
                                        height: max(6, proxy.size.height * ratio)
                                    )
                                    .frame(maxHeight: .infinity, alignment: .bottom)
                            }
                            .frame(height: 130)
                            Text(point.month.formatted(.dateTime.month(.abbreviated)))
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                        }
                        .frame(maxWidth: .infinity)
                    }
                }
            }
        }
    }

    private var categoryCard: some View {
        let totals = ledger.categoryTotals(in: Date())
        let colors = [
            BrandTheme.savings,
            BrandTheme.expense,
            BrandTheme.warning,
            BrandTheme.chartBlue,
            Color(red: 0.5, green: 0.54, blue: 0.43),
        ]
        return SectionCard {
            VStack(alignment: .leading, spacing: 15) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("stats.expenseCategories")
                        .font(.headline)
                    Text("stats.expenseCategoriesHint")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                if totals.isEmpty {
                    Text("stats.noExpense")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(Array(totals.prefix(6).enumerated()), id: \.offset) { index, item in
                        HStack(spacing: 10) {
                            CategoryBadge(
                                category: item.category,
                                size: 34,
                                color: colors[index % colors.count]
                            )
                            Text(LocalizedStringKey(CategoryCatalog.titleKey(for: item.category)))
                                .font(.subheadline.weight(.medium))
                            Spacer()
                            Text(hidden ? "¥•••" : SalaryFormatting.yuan(item.totalCents))
                                .font(.subheadline.monospacedDigit())
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
        }
    }

    private var savingsCard: some View {
        SectionCard {
            VStack(alignment: .leading, spacing: 9) {
                Text("stats.savings")
                    .font(.headline)
                Text(hidden ? "¥••••" : SalaryFormatting.yuan(ledger.savingsCents))
                    .font(.system(size: 38, weight: .bold, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(BrandTheme.savings)
                Text("stats.savingsHint")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }
}
