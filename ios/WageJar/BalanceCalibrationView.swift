import SwiftUI

struct BalanceCalibrationView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var amountText = ""

    let currentCents: Int
    let hidden: Bool
    let onSubmit: (Int) -> Void

    private var actualCents: Int? {
        SalaryFormatting.cents(from: amountText)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("balance.current")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        Text(hidden ? "¥••••" : SalaryFormatting.yuan(currentCents))
                            .font(.system(size: 42, weight: .bold, design: .rounded))
                            .monospacedDigit()
                            .foregroundStyle(BrandTheme.savings)
                        Text("balance.currentHint")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    .padding(20)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(BrandTheme.savings.opacity(0.09), in: RoundedRectangle(cornerRadius: 22))

                    VStack(alignment: .leading, spacing: 8) {
                        Text("balance.updateTitle")
                            .font(.title3.bold())
                        Text("balance.updateHint")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        TextField("0.00", text: $amountText)
                            .keyboardType(.decimalPad)
                            .font(.system(size: 38, weight: .bold, design: .rounded))
                            .padding(16)
                            .background(BrandTheme.groupedBackground, in: RoundedRectangle(cornerRadius: 17))
                            .accessibilityIdentifier("balance.amount")
                    }

                    if let actualCents {
                        let delta = actualCents - currentCents
                        HStack {
                            Text(deltaLabel(delta))
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                            Spacer()
                            if delta != 0 {
                                Text(
                                    "\(delta > 0 ? "+" : "−")\(SalaryFormatting.yuan(abs(delta)))"
                                )
                                .font(.headline.monospacedDigit())
                                .foregroundStyle(delta > 0 ? BrandTheme.savings : BrandTheme.expense)
                            }
                        }
                    }
                }
                .padding(20)
            }
            .background(BrandTheme.groupedBackground.ignoresSafeArea())
            .navigationTitle("balance.title")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("common.cancel") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("common.save") {
                        guard let actualCents else { return }
                        onSubmit(actualCents)
                        dismiss()
                    }
                    .fontWeight(.semibold)
                    .disabled(actualCents == nil)
                    .accessibilityIdentifier("balance.save")
                }
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }

    private func deltaLabel(_ delta: Int) -> String {
        if delta > 0 {
            return String(localized: "balance.increase")
        }
        if delta < 0 {
            return String(localized: "balance.decrease")
        }
        return String(localized: "balance.unchanged")
    }
}
