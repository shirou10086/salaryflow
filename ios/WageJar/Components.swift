import SwiftUI
import WageJarCore

struct SectionCard<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        content
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                BrandTheme.card,
                in: RoundedRectangle(cornerRadius: 22, style: .continuous)
            )
    }
}

struct AmountText: View {
    let cents: Double
    let hidden: Bool
    var font: Font = .system(size: 48, weight: .bold, design: .rounded)
    var color: Color = .primary

    var body: some View {
        Text(hidden ? "¥••••" : SalaryFormatting.yuan(cents))
            .font(font)
            .monospacedDigit()
            .contentTransition(.numericText(value: cents))
            .foregroundStyle(color)
            .minimumScaleFactor(0.62)
            .lineLimit(1)
    }
}

struct StatusPill: View {
    let key: String
    var color: Color = BrandTheme.savings

    var body: some View {
        Text(LocalizedStringKey(key))
            .font(.caption.weight(.semibold))
            .padding(.horizontal, 11)
            .padding(.vertical, 6)
            .foregroundStyle(color)
            .background(color.opacity(0.12), in: Capsule())
    }
}

struct SummaryTile: View {
    let titleKey: String
    let cents: Int
    let color: Color
    let hidden: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(LocalizedStringKey(titleKey))
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(hidden ? "¥•••" : SalaryFormatting.yuan(cents))
                .font(.headline.monospacedDigit())
                .foregroundStyle(color)
                .lineLimit(1)
                .minimumScaleFactor(0.67)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct CategoryBadge: View {
    let category: String
    var size: CGFloat = 38
    var color: Color = BrandTheme.savings

    var body: some View {
        Image(systemName: symbol)
            .font(.system(size: size * 0.4, weight: .semibold))
            .foregroundStyle(color)
            .frame(width: size, height: size)
            .background(color.opacity(0.12), in: RoundedRectangle(cornerRadius: size * 0.32))
    }

    private var symbol: String {
        switch category {
        case "food":
            "fork.knife"
        case "transport":
            "bus.fill"
        case "shopping":
            "bag.fill"
        case "housing":
            "house.fill"
        case "fun":
            "gamecontroller.fill"
        case "health":
            "cross.case.fill"
        case "study":
            "book.fill"
        case "social":
            "person.2.fill"
        case "salary":
            "banknote.fill"
        case "bonus":
            "gift.fill"
        case "invest":
            "chart.line.uptrend.xyaxis"
        case "side":
            "briefcase.fill"
        case "manual_saving":
            "banknote"
        default:
            "ellipsis"
        }
    }
}

struct TransactionRow: View {
    let transaction: LedgerTransaction
    let hidden: Bool

    var body: some View {
        HStack(spacing: 12) {
            CategoryBadge(category: transaction.category, color: accent)
            VStack(alignment: .leading, spacing: 3) {
                Text(LocalizedStringKey(CategoryCatalog.titleKey(for: transaction.category)))
                    .font(.body.weight(.semibold))
                Text(detailText)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 8)
            Text(hidden ? "¥•••" : amountText)
                .font(.body.weight(.semibold))
                .foregroundStyle(accent)
                .monospacedDigit()
        }
        .padding(.vertical, 3)
    }

    private var amountText: String {
        let prefix: String
        switch transaction.type {
        case .expense, .savingOut:
            prefix = "−"
        case .income, .savingIn:
            prefix = "+"
        }
        return "\(prefix)\(SalaryFormatting.yuan(transaction.amountCents))"
    }

    private var accent: Color {
        switch transaction.type {
        case .expense:
            BrandTheme.expense
        case .income, .savingIn:
            BrandTheme.savings
        case .savingOut:
            BrandTheme.warning
        }
    }

    private var detailText: String {
        let timestamp = SalaryFormatting.transactionTimestamp(transaction.occurredAt)
        return transaction.note.isEmpty ? timestamp : "\(transaction.note) · \(timestamp)"
    }
}

struct FilterPill: View {
    let titleKey: String
    let selected: Bool
    var color: Color = BrandTheme.savings
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(LocalizedStringKey(titleKey))
                .font(.subheadline.weight(.semibold))
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .foregroundStyle(selected ? Color.white : Color.primary)
                .background(
                    selected ? color : BrandTheme.card,
                    in: Capsule()
                )
        }
        .buttonStyle(.plain)
    }
}

struct EmptyState: View {
    let symbol: String
    let titleKey: String
    let subtitleKey: String

    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: symbol)
                .font(.system(size: 32, weight: .medium))
                .foregroundStyle(BrandTheme.savings)
            Text(LocalizedStringKey(titleKey))
                .font(.headline)
            Text(LocalizedStringKey(subtitleKey))
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 34)
    }
}
