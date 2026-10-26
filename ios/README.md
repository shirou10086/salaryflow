# WageJar iOS

This directory contains the native SwiftUI version of WageJar. Its product version is aligned
with Android `0.4.4 (11)`.

The app includes:

- a live salary counter based on the user's work schedule
- configurable work hours, workdays, lunch and dinner breaks
- expense and income records with categories and notes
- monthly ledger filtering, deletion, balances and six-month statistics
- savings balance calibration and a configurable savings goal
- system-language support for English and Simplified Chinese

## Requirements

- macOS with full Xcode installed and selected with `sudo xcode-select -s /Applications/Xcode.app`
- An installed iOS 17+ Simulator runtime
- XcodeGen: `brew install xcodegen`

## Generate and run

```bash
cd ios
xcodegen generate
open WageJar.xcodeproj
```

In Xcode, choose an iPhone simulator and run the `WageJar` scheme.
The `iOS CI` GitHub Actions workflow also builds the app, launches it in an available iPhone
simulator, and uploads a simulator screenshot as a build artifact.

The scheduling package can be tested without Xcode:

```bash
swift test --package-path WageJarCore
```

## Platform note

iOS does not allow a third-party app to keep an always-running notification counter like an
Android foreground service. The current iOS version therefore renders the live counter while the
app is open. A future iOS release can add a Live Activity, but iOS controls and throttles its
refresh cadence, so it cannot guarantee one-second updates.
