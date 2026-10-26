import SwiftUI
import WageJarCore

struct QuickAddView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var type: TransactionKind
    @State private var amountText = ""
    @State private var note = ""
    @State private var category: String

    let onSubmit: (TransactionKind, Int, String, String) -> Void

    init(
        initialType: TransactionKind,
        onSubmit: @escaping (TransactionKind, Int, String, String) -> Void
    ) {
        _type = State(initialValue: initialType)
        _category = State(initialValue: CategoryCatalog.options(for: initialType)[0].id)
        self.onSubmit = onSubmit
    }

    private var amountCents: Int? {
        SalaryFormatting.cents(from: amountText)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    HStack(spacing: 10) {
                        typeButton(.expense, titleKey: "common.expense", color: BrandTheme.expense)
                        typeButton(.income, titleKey: "common.income", color: BrandTheme.savings)
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        Text("quickAdd.amount")
                            .font(.headline)
                        HStack(alignment: .firstTextBaseline, spacing: 6) {
                            Text("¥")
                                .font(.title2.weight(.semibold))
                                .foregroundStyle(.secondary)
                            TextField("0.00", text: $amountText)
                                .keyboardType(.decimalPad)
                                .font(.system(size: 42, weight: .bold, design: .rounded))
                                .accessibilityIdentifier("quickAdd.amount")
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 18)
                        .background(BrandTheme.groupedBackground, in: RoundedRectangle(cornerRadius: 18))
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        Text("quickAdd.category")
                            .font(.headline)
                        LazyVGrid(
                            columns: [GridItem(.adaptive(minimum: 104), spacing: 8)],
                            spacing: 8
                        ) {
                            ForEach(CategoryCatalog.options(for: type)) { option in
                                categoryButton(option)
                            }
                        }
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        Text("quickAdd.note")
                            .font(.headline)
                        TextField("quickAdd.optional", text: $note)
                            .textFieldStyle(.roundedBorder)
                            .accessibilityIdentifier("quickAdd.note")
                    }
                }
                .padding(20)
            }
            .background(BrandTheme.groupedBackground.ignoresSafeArea())
            .navigationTitle(type == .expense ? "quickAdd.expenseTitle" : "quickAdd.incomeTitle")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("common.cancel") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("common.save") {
                        guard let amountCents, amountCents > 0 else { return }
                        onSubmit(type, amountCents, category, note.trimmingCharacters(in: .whitespacesAndNewlines))
                        dismiss()
                    }
                    .fontWeight(.semibold)
                    .disabled(amountCents == nil || amountCents == 0)
                    .accessibilityIdentifier("quickAdd.save")
                }
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }

    private func typeButton(
        _ candidate: TransactionKind,
        titleKey: String,
        color: Color
    ) -> some View {
        Button {
            type = candidate
            category = CategoryCatalog.options(for: candidate)[0].id
        } label: {
            Label(LocalizedStringKey(titleKey), systemImage: candidate == .expense ? "arrow.down" : "arrow.up")
                .font(.headline)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 13)
                .foregroundStyle(type == candidate ? Color.white : color)
                .background(type == candidate ? color : color.opacity(0.12), in: RoundedRectangle(cornerRadius: 15))
        }
        .buttonStyle(.plain)
    }

    private func categoryButton(_ option: CategoryOption) -> some View {
        Button {
            category = option.id
        } label: {
            HStack(spacing: 8) {
                CategoryBadge(
                    category: option.id,
                    size: 30,
                    color: type == .expense ? BrandTheme.expense : BrandTheme.savings
                )
                Text(LocalizedStringKey(option.titleKey))
                    .font(.subheadline.weight(.medium))
                    .lineLimit(1)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 8)
            .foregroundStyle(.primary)
            .background(
                category == option.id
                    ? (type == .expense ? BrandTheme.expense : BrandTheme.savings).opacity(0.13)
                    : BrandTheme.card,
                in: RoundedRectangle(cornerRadius: 13)
            )
            .overlay {
                RoundedRectangle(cornerRadius: 13)
                    .stroke(
                        category == option.id
                            ? (type == .expense ? BrandTheme.expense : BrandTheme.savings)
                            : Color.secondary.opacity(0.2),
                        lineWidth: 1
                    )
            }
        }
        .buttonStyle(.plain)
    }
}
