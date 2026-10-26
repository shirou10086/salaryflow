import SwiftUI

@main
@MainActor
struct WageJarApp: App {
    @StateObject private var store = SettingsStore()
    @StateObject private var ledger = LedgerStore()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(store)
                .environmentObject(ledger)
        }
    }
}
