import SwiftUI
import WageJarCore

struct RootView: View {
    @EnvironmentObject private var store: SettingsStore

    var body: some View {
        if store.settings.onboarded {
            MainTabs()
        } else {
            OnboardingView()
        }
    }
}

private enum AppTab: Hashable {
    case dashboard
    case ledger
    case stats
    case settings
}

private struct MainTabs: View {
    @EnvironmentObject private var store: SettingsStore
    @EnvironmentObject private var ledger: LedgerStore

    @State private var selection: AppTab = .dashboard
    @State private var quickAddType: TransactionKind = .expense
    @State private var showQuickAdd = false
    @State private var showBalanceCalibration = false

    var body: some View {
        TabView(selection: $selection) {
            NavigationStack {
                DashboardView(
                    onQuickAdd: presentQuickAdd,
                    onOpenLedger: { selection = .ledger },
                    onCalibrate: { showBalanceCalibration = true }
                )
            }
            .tabItem {
                Label("tab.home", systemImage: "chart.line.uptrend.xyaxis")
                    .accessibilityIdentifier("tab.home")
            }
            .tag(AppTab.dashboard)

            NavigationStack {
                LedgerView {
                    presentQuickAdd(.expense)
                }
            }
            .tabItem {
                Label("tab.ledger", systemImage: "list.bullet.rectangle")
                    .accessibilityIdentifier("tab.ledger")
            }
            .tag(AppTab.ledger)

            StatsView()
                .tabItem {
                    Label("tab.stats", systemImage: "chart.bar.xaxis")
                        .accessibilityIdentifier("tab.stats")
                }
                .tag(AppTab.stats)

            SettingsView()
                .tabItem {
                    Label("tab.settings", systemImage: "slider.horizontal.3")
                        .accessibilityIdentifier("tab.settings")
                }
                .tag(AppTab.settings)
        }
        .tint(BrandTheme.savings)
        .sheet(isPresented: $showQuickAdd) {
            QuickAddView(initialType: quickAddType) { type, amountCents, category, note in
                ledger.add(
                    type: type,
                    amountCents: amountCents,
                    category: category,
                    note: note
                )
            }
        }
        .sheet(isPresented: $showBalanceCalibration) {
            BalanceCalibrationView(
                currentCents: ledger.savingsCents,
                hidden: store.settings.hideAmounts
            ) { actualCents in
                ledger.calibrateBalance(to: actualCents)
            }
        }
    }

    private func presentQuickAdd(_ type: TransactionKind) {
        quickAddType = type
        showQuickAdd = true
    }
}
