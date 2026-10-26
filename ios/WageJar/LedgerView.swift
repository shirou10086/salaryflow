import SwiftUI
import WageJarCore

private enum LedgerFilter: String, CaseIterable {
    case all
    case expense
    case income
    case saving

    var titleKey: String {
        switch self {
        case .all:
            "ledger.all"
        case .expense:
            "common.expense"
        case .income:
            "common.income"
        case .saving:
            "ledger.saving"
        }
    }

    func matches(_ transaction: LedgerTransaction) -> Bool {
        switch self {
        case .all:
            true
        case .expense:
            transaction.type == .expense
        case .income:
            transaction.type == .income
        case .saving:
            transaction.type == .savingIn || transaction.type == .savingOut
        }
    }
}

struct LedgerView: View {
    @EnvironmentObject private var settingsStore: SettingsStore
    @EnvironmentObject private var ledger: LedgerStore

    @State private var month = Date()
    @State private var filter: LedgerFilter = .all
    @State private var pendingDelete: LedgerTransaction?

    let onAdd: () -> Void

    private var monthTransactions: [LedgerTransaction] {
        ledger.transactions(in: month).filter(filter.matches)
    }

    private var groupedTransactions: [(date: Date, items: [LedgerTransaction])] {
        let grouped = Dictionary(grouping: monthTransactions) {
            Calendar.current.startOfDay(for: $0.occurredAt)
        }
        return grouped
            .map { (date: $0.key, items: $0.value.sorted { $0.occurredAt > $1.occurredAt }) }
            .sorted { $0.date > $1.date }
    }

    private var hidden: Bool {
        settingsStore.settings.hideAmounts
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 14) {
                    monthSwitcher

                    SectionCard {
                        HStack(spacing: 12) {
                            SummaryTile(
                                titleKey: "ledger.monthExpense",
                                cents: ledger.expenseCents(in: month),
                                color: BrandTheme.expense,
                                hidden: hidden
                            )
                            SummaryTile(
                                titleKey: "ledger.monthIncome",
                                cents: ledger.incomeCents(in: month),
                                color: BrandTheme.savings,
                                hidden: hidden
                            )
                            SummaryTile(
                                titleKey: "ledger.monthBalance",
                                cents: ledger.incomeCents(in: month) - ledger.expenseCents(in: month),
                                color: BrandTheme.chartBlue,
                                hidden: hidden
                            )
                        }
                    }

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(LedgerFilter.allCases, id: \.rawValue) { item in
                                FilterPill(
                                    titleKey: item.titleKey,
                                    selected: filter == item
                                ) {
                                    filter = item
                                }
                            }
                        }
                        .padding(.vertical, 1)
                    }

                    if groupedTransactions.isEmpty {
                        SectionCard {
                            EmptyState(
                                symbol: "receipt",
                                titleKey: "ledger.emptyTitle",
                                subtitleKey: "ledger.emptySubtitle"
                            )
                        }
                    } else {
                        ForEach(groupedTransactions, id: \.date) { group in
                            VStack(alignment: .leading, spacing: 9) {
                                HStack {
                                    Text(
                                        "\(SalaryFormatting.day(group.date)) · \(SalaryFormatting.weekday(group.date))"
                                    )
                                    .font(.headline)
                                    Spacer()
                                    let expense = group.items
                                        .filter { $0.type == .expense }
                                        .reduce(0) { $0 + $1.amountCents }
                                    if expense > 0 {
                                        Text(
                                            hidden
                                                ? "¥•••"
                                                : String(
                                                    format: String(localized: "ledger.dayExpense"),
                                                    SalaryFormatting.yuan(expense)
                                                )
                                        )
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                    }
                                }
                                .padding(.horizontal, 4)

                                SectionCard {
                                    VStack(spacing: 10) {
                                        ForEach(group.items) { transaction in
                                            HStack(spacing: 8) {
                                                TransactionRow(transaction: transaction, hidden: hidden)
                                                Button {
                                                    pendingDelete = transaction
                                                } label: {
                                                    Image(systemName: "trash")
                                                        .font(.caption)
                                                        .foregroundStyle(.secondary)
                                                }
                                                .buttonStyle(.plain)
                                                .accessibilityLabel("common.delete")
                                            }
                                            if transaction.id != group.items.last?.id {
                                                Divider().padding(.leading, 50)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .padding(.horizontal, 18)
                .padding(.bottom, 110)
            }
            .background(BrandTheme.groupedBackground.ignoresSafeArea())
            .accessibilityIdentifier("ledger.screen")
            .navigationTitle("tab.ledger")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: onAdd) {
                        Image(systemName: "plus")
                    }
                    .accessibilityLabel("quickAdd.title")
                    .accessibilityIdentifier("ledger.add")
                }
            }
            .alert(
                "ledger.deleteTitle",
                isPresented: Binding(
                    get: { pendingDelete != nil },
                    set: { if !$0 { pendingDelete = nil } }
                )
            ) {
                Button("common.cancel", role: .cancel) {
                    pendingDelete = nil
                }
                Button("common.delete", role: .destructive) {
                    if let pendingDelete {
                        ledger.delete(pendingDelete)
                    }
                    pendingDelete = nil
                }
            } message: {
                Text("ledger.deleteHint")
            }
        }
    }

    private var monthSwitcher: some View {
        HStack {
            Button {
                shiftMonth(-1)
            } label: {
                Image(systemName: "chevron.left")
                    .frame(width: 40, height: 40)
            }
            .accessibilityLabel("ledger.previousMonth")

            Spacer()
            VStack(spacing: 2) {
                Text(SalaryFormatting.monthYear(month))
                    .font(.title3.bold())
                if !Calendar.current.isDate(month, equalTo: Date(), toGranularity: .month) {
                    Button("ledger.currentMonth") {
                        month = Date()
                    }
                    .font(.caption)
                }
            }
            Spacer()

            Button {
                shiftMonth(1)
            } label: {
                Image(systemName: "chevron.right")
                    .frame(width: 40, height: 40)
            }
            .accessibilityLabel("ledger.nextMonth")
        }
    }

    private func shiftMonth(_ value: Int) {
        if let newMonth = Calendar.current.date(byAdding: .month, value: value, to: month) {
            month = newMonth
        }
    }
}
