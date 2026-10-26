import Foundation
import WageJarCore

struct AppSettings: Codable, Equatable {
    var salary: SalaryConfig
    var onboarded: Bool
    var hideAmounts: Bool
    var goalName: String
    var goalAmountCents: Int

    static let `default` = AppSettings(
        salary: SalaryConfig(),
        onboarded: false,
        hideAmounts: false,
        goalName: "",
        goalAmountCents: 1_000_000
    )

    init(
        salary: SalaryConfig,
        onboarded: Bool,
        hideAmounts: Bool,
        goalName: String = "",
        goalAmountCents: Int = 1_000_000
    ) {
        self.salary = salary
        self.onboarded = onboarded
        self.hideAmounts = hideAmounts
        self.goalName = goalName
        self.goalAmountCents = goalAmountCents
    }

    private enum CodingKeys: String, CodingKey {
        case salary
        case onboarded
        case hideAmounts
        case goalName
        case goalAmountCents
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        salary = try container.decodeIfPresent(SalaryConfig.self, forKey: .salary) ?? SalaryConfig()
        onboarded = try container.decodeIfPresent(Bool.self, forKey: .onboarded) ?? false
        hideAmounts = try container.decodeIfPresent(Bool.self, forKey: .hideAmounts) ?? false
        goalName = try container.decodeIfPresent(String.self, forKey: .goalName) ?? ""
        goalAmountCents = try container.decodeIfPresent(Int.self, forKey: .goalAmountCents)
            ?? 1_000_000
    }
}

@MainActor
final class SettingsStore: ObservableObject {
    @Published var settings: AppSettings {
        didSet {
            save()
        }
    }

    private let defaults: UserDefaults
    private let storageKey = "wagejar.settings.v2"
    private let legacyStorageKey = "wagejar.settings.v1"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        if ProcessInfo.processInfo.environment["WAGEJAR_UI_TEST_RESET"] == "1" {
            defaults.removeObject(forKey: storageKey)
            defaults.removeObject(forKey: legacyStorageKey)
        }
        if
            let data = defaults.data(forKey: storageKey),
            let decoded = try? JSONDecoder().decode(AppSettings.self, from: data)
        {
            settings = decoded
        } else if
            let data = defaults.data(forKey: legacyStorageKey),
            let decoded = try? JSONDecoder().decode(AppSettings.self, from: data)
        {
            settings = decoded
        } else {
            settings = .default
        }
        save()
    }

    private func save() {
        guard let data = try? JSONEncoder().encode(settings) else { return }
        defaults.set(data, forKey: storageKey)
    }
}
