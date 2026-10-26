import SwiftUI

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

private struct MainTabs: View {
    var body: some View {
        TabView {
            DashboardView()
                .tabItem {
                    Label("tab.home", systemImage: "chart.line.uptrend.xyaxis")
                }

            SettingsView()
                .tabItem {
                    Label("tab.settings", systemImage: "slider.horizontal.3")
                }
        }
        .tint(BrandTheme.savings)
    }
}
