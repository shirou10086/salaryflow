import Foundation

public enum TransactionKind: String, Codable, CaseIterable, Sendable {
    case expense
    case income
    case savingIn
    case savingOut
}

public struct LedgerTransaction: Codable, Equatable, Identifiable, Sendable {
    public let id: UUID
    public var type: TransactionKind
    public var amountCents: Int
    public var category: String
    public var note: String
    public var occurredAt: Date

    public init(
        id: UUID = UUID(),
        type: TransactionKind,
        amountCents: Int,
        category: String,
        note: String = "",
        occurredAt: Date = Date()
    ) {
        self.id = id
        self.type = type
        self.amountCents = amountCents
        self.category = category
        self.note = note
        self.occurredAt = occurredAt
    }
}

public enum LedgerMath {
    public static let balanceCalibrationCategory = "balance_calibration"

    public static func balanceDeltaCents(for transaction: LedgerTransaction) -> Int {
        switch transaction.type {
        case .income:
            transaction.amountCents
        case .expense:
            -transaction.amountCents
        case .savingIn where transaction.category == balanceCalibrationCategory:
            transaction.amountCents
        case .savingOut where transaction.category == balanceCalibrationCategory:
            -transaction.amountCents
        case .savingIn, .savingOut:
            0
        }
    }

    public static func savingsCents(transactions: [LedgerTransaction]) -> Int {
        transactions.reduce(0) { $0 + balanceDeltaCents(for: $1) }
    }
}
