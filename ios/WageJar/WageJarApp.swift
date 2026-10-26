import SwiftUI

@main
@MainActor
struct WageJarApp: App {
    @StateObject private var store = SettingsStore()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(store)
        }
    }
}
