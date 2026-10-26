import SwiftUI

enum BrandTheme {
    static let heroStart = Color(red: 16 / 255, green: 62 / 255, blue: 52 / 255)
    static let heroEnd = Color(red: 26 / 255, green: 94 / 255, blue: 76 / 255)
    static let savings = Color(red: 38 / 255, green: 119 / 255, blue: 95 / 255)
    static let accent = Color(red: 169 / 255, green: 224 / 255, blue: 200 / 255)
    static let paper = Color(red: 250 / 255, green: 248 / 255, blue: 242 / 255)
}

enum SalaryFormatting {
    static func yuan(_ cents: Double, fractionDigits: Int = 2) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.minimumFractionDigits = fractionDigits
        formatter.maximumFractionDigits = fractionDigits
        let value = formatter.string(from: NSNumber(value: cents / 100)) ?? "0.00"
        return "¥\(value)"
    }

    static func clock(minute: Int) -> String {
        String(format: "%02d:%02d", minute / 60, minute % 60)
    }
}

extension View {
    func decimalKeyboard() -> some View {
        #if os(iOS)
        keyboardType(.decimalPad)
        #else
        self
        #endif
    }

    func numberKeyboard() -> some View {
        #if os(iOS)
        keyboardType(.numberPad)
        #else
        self
        #endif
    }
}
