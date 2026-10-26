import Foundation
import WageJarCore

struct AppSettings: Codable, Equatable {
    var salary: SalaryConfig
    var onboarded: Bool
    var hideAmounts: Bool

    static let `default` = AppSettings(
        salary: SalaryConfig(),
        onboarded: false,
        hideAmounts: false
    )
}

@MainActor
final class SettingsStore: ObservableObject {
    @Published var settings: AppSettings {
        didSet {
            save()
        }
    }

    private let defaults: UserDefaults
    private let storageKey = "wagejar.settings.v1"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        if
            let data = defaults.data(forKey: storageKey),
            let decoded = try? JSONDecoder().decode(AppSettings.self, from: data)
        {
            settings = decoded
        } else {
            settings = .default
        }
    }

    private func save() {
        guard let data = try? JSONEncoder().encode(settings) else { return }
        defaults.set(data, forKey: storageKey)
    }
}
