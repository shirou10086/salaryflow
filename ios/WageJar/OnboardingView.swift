import SwiftUI
import WageJarCore

struct OnboardingView: View {
    @EnvironmentObject private var store: SettingsStore
    @State private var salaryText = "10000"
    @State private var paydayText = "15"

    private var salaryCents: Int? {
        guard let yuan = Double(salaryText.replacingOccurrences(of: ",", with: "")) else {
            return nil
        }
        return Int((yuan * 100).rounded())
    }

    private var payday: Int? {
        guard let day = Int(paydayText), (1...31).contains(day) else { return nil }
        return day
    }

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [BrandTheme.heroStart, BrandTheme.heroEnd],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    Text("app.name")
                        .font(.system(size: 38, weight: .bold, design: .rounded))
                        .foregroundStyle(.white)

                    Text("onboarding.subtitle")
                        .font(.headline)
                        .foregroundStyle(.white.opacity(0.78))

                    VStack(spacing: 16) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("settings.monthlySalary")
                                .font(.subheadline.weight(.semibold))
                            TextField("onboarding.salaryPlaceholder", text: $salaryText)
                                .decimalKeyboard()
                                .textFieldStyle(.roundedBorder)
                                .accessibilityIdentifier("onboarding.salary")
                        }

                        VStack(alignment: .leading, spacing: 8) {
                            Text("settings.payday")
                                .font(.subheadline.weight(.semibold))
                            TextField("15", text: $paydayText)
                                .numberKeyboard()
                                .textFieldStyle(.roundedBorder)
                                .accessibilityIdentifier("onboarding.payday")
                        }
                    }
                    .padding(20)
                    .background(.white, in: RoundedRectangle(cornerRadius: 24))

                    Text("onboarding.workHint")
                        .font(.footnote)
                        .foregroundStyle(.white.opacity(0.72))

                    Button {
                        guard let salaryCents, let payday else { return }
                        var settings = store.settings
                        settings.salary.monthlySalaryCents = salaryCents
                        settings.salary.payday = payday
                        settings.onboarded = true
                        store.settings = settings
                    } label: {
                        Text("onboarding.finish")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 15)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(BrandTheme.accent)
                    .foregroundStyle(BrandTheme.heroStart)
                    .disabled(salaryCents == nil || payday == nil)
                    .accessibilityIdentifier("onboarding.finish")
                }
                .padding(24)
            }
        }
    }
}
