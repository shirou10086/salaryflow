# WageJar iOS

This directory contains the native SwiftUI version of WageJar. Its product version is aligned
with Android `0.4.5 (12)`.

The app includes:

- a live salary counter based on the user's work schedule
- configurable work hours, workdays, lunch and dinner breaks
- expense and income records with categories and notes
- monthly ledger filtering, deletion, balances and six-month statistics
- savings balance calibration and a configurable savings goal
- system-language support for English and Simplified Chinese

## Installation

There is no pre-signed iOS install package in GitHub Releases. iOS cannot distribute an
unsigned `.ipa` like an Android APK. To run it on a real iPhone:

1. Open the project in Xcode and configure Signing & Capabilities with your Apple ID.
2. Select the connected iPhone and run the `WageJar` scheme.
3. For other people to install it directly, publish through TestFlight or the App Store with an
   Apple Developer Program account.

The GitHub Actions workflow builds and tests the app in an iPhone simulator and publishes
screenshots as artifacts. It does not publish a signed device build.

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
