import SwiftUI
import WageJarCore
#if os(iOS)
import UIKit
#endif

struct SettingsView: View {
    @EnvironmentObject private var store: SettingsStore
    @EnvironmentObject private var ledger: LedgerStore
    @Environment(\.openURL) private var openURL

    @State private var showBalanceCalibration = false
    @State private var showClearConfirmation = false

    var body: some View {
        NavigationStack {
            Form {
                salarySection
                workHoursSection
                breakSection
                savingsSection
                goalSection
                displaySection
                dataSection
                aboutSection
            }
            .accessibilityIdentifier("settings.screen")
            .navigationTitle("settings.title")
        }
        .sheet(isPresented: $showBalanceCalibration) {
            BalanceCalibrationView(
                currentCents: ledger.savingsCents,
                hidden: store.settings.hideAmounts
            ) { actualCents in
                ledger.calibrateBalance(to: actualCents)
            }
        }
        .confirmationDialog(
            "settings.clearTitle",
            isPresented: $showClearConfirmation,
            titleVisibility: .visible
        ) {
            Button("settings.clearData", role: .destructive) {
                ledger.clearAll()
            }
            Button("common.cancel", role: .cancel) {}
        } message: {
            Text("settings.clearHint")
        }
    }

    private var salarySection: some View {
        Section("settings.salarySection") {
            TextField(
                "settings.monthlySalary",
                value: Binding(
                    get: {
                        Double(store.settings.salary.monthlySalaryCents) / 100
                    },
                    set: { value in
                        updateSalary {
                            $0.monthlySalaryCents = max(Int((value * 100).rounded()), 0)
                        }
                    }
                ),
                format: .number.precision(.fractionLength(0...2))
            )
            .keyboardType(.decimalPad)
            .accessibilityIdentifier("settings.monthlySalary")

            TextField(
                "settings.paidDays",
                value: Binding(
                    get: { store.settings.salary.paidDaysPerMonth },
                    set: { value in
                        updateSalary {
                            $0.paidDaysPerMonth = max(value, 1)
                        }
                    }
                ),
                format: .number.precision(.fractionLength(0...2))
            )
            .keyboardType(.decimalPad)

            Stepper(
                value: Binding(
                    get: { store.settings.salary.payday },
                    set: { value in
                        updateSalary {
                            $0.payday = min(max(value, 1), 31)
                        }
                    }
                ),
                in: 1...31
            ) {
                LabeledContent("settings.payday", value: "\(store.settings.salary.payday)")
            }
        }
    }

    private var workHoursSection: some View {
        Section("settings.workHours") {
            DatePicker(
                "settings.workStart",
                selection: minuteBinding(\.startMinute),
                displayedComponents: .hourAndMinute
            )
            DatePicker(
                "settings.workEnd",
                selection: minuteBinding(\.endMinute),
                displayedComponents: .hourAndMinute
            )

            ForEach(Self.orderedWeekdays, id: \.rawValue) { weekday in
                Toggle(
                    weekday.localizedName,
                    isOn: Binding(
                        get: { store.settings.salary.schedule.workDays.contains(weekday) },
                        set: { enabled in
                            updateSchedule { schedule in
                                if enabled {
                                    schedule.workDays.insert(weekday)
                                } else {
                                    schedule.workDays.remove(weekday)
                                }
                            }
                        }
                    )
                )
            }
        }
    }

    private var breakSection: some View {
        Section("settings.breaks") {
            DatePicker(
                "settings.lunchStart",
                selection: minuteBinding(\.lunchStartMinute),
                displayedComponents: .hourAndMinute
            )
            DatePicker(
                "settings.lunchEnd",
                selection: minuteBinding(\.lunchEndMinute),
                displayedComponents: .hourAndMinute
            )
            Toggle(
                "settings.lunchPaid",
                isOn: Binding(
                    get: { store.settings.salary.schedule.lunchPaid },
                    set: { paid in
                        updateSchedule { $0.lunchPaid = paid }
                    }
                )
            )

            Toggle(
                "settings.dinner",
                isOn: Binding(
                    get: { store.settings.salary.schedule.dinnerEnabled },
                    set: { enabled in
                        updateSchedule { $0.dinnerEnabled = enabled }
                    }
                )
            )
            .accessibilityIdentifier("settings.dinner")

            if store.settings.salary.schedule.dinnerEnabled {
                DatePicker(
                    "settings.dinnerStart",
                    selection: minuteBinding(\.dinnerStartMinute),
                    displayedComponents: .hourAndMinute
                )
                DatePicker(
                    "settings.dinnerEnd",
                    selection: minuteBinding(\.dinnerEndMinute),
                    displayedComponents: .hourAndMinute
                )
                Toggle(
                    "settings.dinnerPaid",
                    isOn: Binding(
                        get: { store.settings.salary.schedule.dinnerPaid },
                        set: { paid in
                            updateSchedule { $0.dinnerPaid = paid }
                        }
                    )
                )
                Text("settings.dinnerHint")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private var savingsSection: some View {
        Section("settings.savingsSection") {
            LabeledContent(
                "settings.currentSavings",
                value: store.settings.hideAmounts
                    ? "¥••••"
                    : SalaryFormatting.yuan(ledger.savingsCents)
            )
            Button("settings.updateSavings") {
                showBalanceCalibration = true
            }
        }
    }

    private var goalSection: some View {
        Section("settings.goalSection") {
            TextField(
                "settings.goalName",
                text: Binding(
                    get: { store.settings.goalName },
                    set: { value in
                        var settings = store.settings
                        settings.goalName = value
                        store.settings = settings
                    }
                )
            )
            TextField(
                "settings.goalAmount",
                value: Binding(
                    get: { Double(store.settings.goalAmountCents) / 100 },
                    set: { value in
                        var settings = store.settings
                        settings.goalAmountCents = max(Int((value * 100).rounded()), 0)
                        store.settings = settings
                    }
                ),
                format: .number.precision(.fractionLength(0...2))
            )
            .keyboardType(.decimalPad)
        }
    }

    private var displaySection: some View {
        Section("settings.display") {
            Toggle(
                "settings.hideAmounts",
                isOn: Binding(
                    get: { store.settings.hideAmounts },
                    set: { hidden in
                        var settings = store.settings
                        settings.hideAmounts = hidden
                        store.settings = settings
                    }
                )
            )
        }
    }

    private var dataSection: some View {
        Section("settings.data") {
            Button("settings.clearData", role: .destructive) {
                showClearConfirmation = true
            }
        }
    }

    private var aboutSection: some View {
        Section("settings.about") {
            LabeledContent("settings.version", value: "0.4.5 (12)")
            Button {
                #if os(iOS)
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    openURL(url)
                }
                #endif
            } label: {
                LabeledContent(
                    "settings.language",
                    value: String(localized: "settings.languageValue")
                )
            }
            Text("settings.iosNotificationNote")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
    }

    private func updateSalary(_ transform: (inout SalaryConfig) -> Void) {
        var settings = store.settings
        transform(&settings.salary)
        store.settings = settings
    }

    private func updateSchedule(_ transform: (inout WorkSchedule) -> Void) {
        updateSalary { transform(&$0.schedule) }
    }

    private func minuteBinding(
        _ keyPath: WritableKeyPath<WorkSchedule, Int>
    ) -> Binding<Date> {
        Binding(
            get: {
                let minute = store.settings.salary.schedule[keyPath: keyPath]
                return Calendar.current.date(
                    bySettingHour: minute / 60,
                    minute: minute % 60,
                    second: 0,
                    of: Date()
                ) ?? Date()
            },
            set: { date in
                let components = Calendar.current.dateComponents([.hour, .minute], from: date)
                let minute = (components.hour ?? 0) * 60 + (components.minute ?? 0)
                updateSchedule { $0[keyPath: keyPath] = minute }
            }
        )
    }

    private static let orderedWeekdays: [Weekday] = [
        .monday,
        .tuesday,
        .wednesday,
        .thursday,
        .friday,
        .saturday,
        .sunday,
    ]
}

private extension Weekday {
    var localizedName: String {
        let symbols = Calendar.current.weekdaySymbols
        guard symbols.indices.contains(rawValue - 1) else { return "\(rawValue)" }
        return symbols[rawValue - 1]
    }
}
