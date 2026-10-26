import Foundation
import WageJarCore

struct CategoryOption: Identifiable, Equatable {
    let id: String
    let titleKey: String
}

enum CategoryCatalog {
    static let expenses = [
        CategoryOption(id: "food", titleKey: "category.food"),
        CategoryOption(id: "transport", titleKey: "category.transport"),
        CategoryOption(id: "shopping", titleKey: "category.shopping"),
        CategoryOption(id: "housing", titleKey: "category.housing"),
        CategoryOption(id: "fun", titleKey: "category.fun"),
        CategoryOption(id: "health", titleKey: "category.health"),
        CategoryOption(id: "study", titleKey: "category.study"),
        CategoryOption(id: "social", titleKey: "category.social"),
        CategoryOption(id: "other", titleKey: "category.other"),
    ]

    static let incomes = [
        CategoryOption(id: "salary", titleKey: "category.salary"),
        CategoryOption(id: "bonus", titleKey: "category.bonus"),
        CategoryOption(id: "invest", titleKey: "category.invest"),
        CategoryOption(id: "side", titleKey: "category.side"),
        CategoryOption(id: "other_income", titleKey: "category.otherIncome"),
    ]

    static func options(for type: TransactionKind) -> [CategoryOption] {
        switch type {
        case .expense:
            expenses
        case .income:
            incomes
        case .savingIn, .savingOut:
            [CategoryOption(id: "manual_saving", titleKey: "category.saving")]
        }
    }

    static func titleKey(for id: String) -> String {
        (expenses + incomes).first(where: { $0.id == id })?.titleKey ?? "category.other"
    }
}

@MainActor
final class LedgerStore: ObservableObject {
    @Published private(set) var transactions: [LedgerTransaction]

    private let defaults: UserDefaults
    private let storageKey = "wagejar.ledger.v1"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        if ProcessInfo.processInfo.environment["WAGEJAR_UI_TEST_RESET"] == "1" {
            defaults.removeObject(forKey: storageKey)
        }
        if
            let data = defaults.data(forKey: storageKey),
            let decoded = try? JSONDecoder().decode([LedgerTransaction].self, from: data)
        {
            transactions = decoded
        } else {
            transactions = []
        }
    }

    var savingsCents: Int {
        transactions.reduce(0) { $0 + LedgerMath.balanceDeltaCents(for: $1) }
    }

    func add(
        type: TransactionKind,
        amountCents: Int,
        category: String,
        note: String,
        occurredAt: Date = Date()
    ) {
        guard amountCents > 0 else { return }
        transactions.insert(
            LedgerTransaction(
                type: type,
                amountCents: amountCents,
                category: category,
                note: note,
                occurredAt: occurredAt
            ),
            at: 0
        )
        save()
    }

    func calibrateBalance(to actualCents: Int) {
        guard actualCents >= 0 else { return }
        let delta = actualCents - savingsCents
        guard delta != 0 else { return }
        add(
            type: delta > 0 ? .savingIn : .savingOut,
            amountCents: abs(delta),
            category: LedgerMath.balanceCalibrationCategory,
            note: delta > 0 ? "balance_up" : "balance_down"
        )
    }

    func delete(_ transaction: LedgerTransaction) {
        transactions.removeAll { $0.id == transaction.id }
        save()
    }

    func clearAll() {
        transactions = []
        save()
    }

    func transactions(in month: Date, calendar: Calendar = .current) -> [LedgerTransaction] {
        transactions.filter {
            calendar.isDate($0.occurredAt, equalTo: month, toGranularity: .month)
        }
    }

    func expenseCents(in month: Date, calendar: Calendar = .current) -> Int {
        transactions(in: month, calendar: calendar)
            .filter { $0.type == .expense }
            .reduce(0) { $0 + $1.amountCents }
    }

    func incomeCents(in month: Date, calendar: Calendar = .current) -> Int {
        transactions(in: month, calendar: calendar)
            .filter { $0.type == .income }
            .reduce(0) { $0 + $1.amountCents }
    }

    func todayExpenseCents(calendar: Calendar = .current) -> Int {
        transactions
            .filter { $0.type == .expense && calendar.isDateInToday($0.occurredAt) }
            .reduce(0) { $0 + $1.amountCents }
    }

    func categoryTotals(
        in month: Date,
        calendar: Calendar = .current
    ) -> [(category: String, totalCents: Int)] {
        let grouped = Dictionary(grouping: transactions(in: month, calendar: calendar)) {
            $0.type == .expense ? $0.category : "ignored"
        }
        return grouped
            .filter { $0.key != "ignored" }
            .map { key, values in
                (key, values.reduce(0) { $0 + $1.amountCents })
            }
            .sorted { $0.totalCents > $1.totalCents }
    }

    func recentMonths(count: Int = 6, calendar: Calendar = .current) -> [MonthTotal] {
        let now = Date()
        return (0..<count).reversed().compactMap { offset in
            guard let month = calendar.date(byAdding: .month, value: -offset, to: now) else {
                return nil
            }
            return MonthTotal(
                month: month,
                expenseCents: expenseCents(in: month, calendar: calendar),
                incomeCents: incomeCents(in: month, calendar: calendar)
            )
        }
    }

    private func save() {
        guard let data = try? JSONEncoder().encode(transactions) else { return }
        defaults.set(data, forKey: storageKey)
    }
}

struct MonthTotal: Identifiable {
    let month: Date
    let expenseCents: Int
    let incomeCents: Int

    var id: Date { month }
}
