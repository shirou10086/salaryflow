import SwiftUI

enum BrandTheme {
    static let heroStart = Color(red: 16 / 255, green: 62 / 255, blue: 52 / 255)
    static let heroEnd = Color(red: 26 / 255, green: 94 / 255, blue: 76 / 255)
    static let savings = Color(red: 38 / 255, green: 119 / 255, blue: 95 / 255)
    static let accent = Color(red: 169 / 255, green: 224 / 255, blue: 200 / 255)
    static let paper = Color(red: 250 / 255, green: 248 / 255, blue: 242 / 255)
    static let expense = Color(red: 179 / 255, green: 74 / 255, blue: 52 / 255)
    static let warning = Color(red: 176 / 255, green: 124 / 255, blue: 47 / 255)
    static let chartBlue = Color(red: 74 / 255, green: 112 / 255, blue: 132 / 255)
    static let groupedBackground = Color(red: 245 / 255, green: 244 / 255, blue: 240 / 255)
    static let card = Color.white
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

    static func yuan(_ cents: Int, fractionDigits: Int = 2) -> String {
        yuan(Double(cents), fractionDigits: fractionDigits)
    }

    static func compactYuan(_ cents: Int) -> String {
        let value = Double(cents) / 100
        if abs(value) >= 10_000 {
            return String(format: "%.1f万", value / 10_000)
        }
        return String(format: "%.2f", value)
    }

    static func clock(minute: Int) -> String {
        String(format: "%02d:%02d", minute / 60, minute % 60)
    }

    static func time(_ date: Date) -> String {
        date.formatted(date: .omitted, time: .shortened)
    }

    static func day(_ date: Date) -> String {
        date.formatted(.dateTime.month().day())
    }

    static func monthYear(_ date: Date) -> String {
        date.formatted(.dateTime.year().month(.wide))
    }

    static func weekday(_ date: Date) -> String {
        date.formatted(.dateTime.weekday(.wide))
    }

    static func cents(from text: String) -> Int? {
        let cleaned = text
            .replacingOccurrences(of: ",", with: "")
            .replacingOccurrences(of: "¥", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        guard let value = Double(cleaned), value.isFinite, value >= 0 else {
            return nil
        }
        return Int((value * 100).rounded())
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
