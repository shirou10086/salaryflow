import SwiftUI
import WageJarCore

struct SettingsView: View {
    @EnvironmentObject private var store: SettingsStore

    var body: some View {
        NavigationStack {
            Form {
                salarySection
                workHoursSection
                breakSection
                appearanceSection
                aboutSection
            }
            .navigationTitle("settings.title")
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
                        var settings = store.settings
                        settings.salary.monthlySalaryCents = Int((value * 100).rounded())
                        store.settings = settings
                    }
                ),
                format: .number.precision(.fractionLength(2))
            )
            .decimalKeyboard()

            Stepper(
                value: Binding(
                    get: { store.settings.salary.payday },
                    set: { value in
                        var settings = store.settings
                        settings.salary.payday = value
                        store.settings = settings
                    }
                ),
                in: 1...31
            ) {
                LabeledContent("settings.payday", value: "\(store.settings.salary.payday)")
            }

            TextField(
                "settings.paidDays",
                value: Binding(
                    get: { store.settings.salary.paidDaysPerMonth },
                    set: { value in
                        var settings = store.settings
                        settings.salary.paidDaysPerMonth = max(value, 1)
                        store.settings = settings
                    }
                ),
                format: .number.precision(.fractionLength(0...2))
            )
            .decimalKeyboard()
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

    private var appearanceSection: some View {
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

    private var aboutSection: some View {
        Section("settings.about") {
            LabeledContent("settings.version", value: "0.4.4 (11)")
            Text("settings.iosNotificationNote")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
    }

    private func updateSchedule(_ transform: (inout WorkSchedule) -> Void) {
        var settings = store.settings
        transform(&settings.salary.schedule)
        store.settings = settings
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
        Calendar.current.weekdaySymbols[rawValue - 1]
    }
}
