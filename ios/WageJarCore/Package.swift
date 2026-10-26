// swift-tools-version: 5.9

import PackageDescription

let package = Package(
    name: "WageJarCore",
    platforms: [
        .iOS(.v17),
        .macOS(.v13),
    ],
    products: [
        .library(name: "WageJarCore", targets: ["WageJarCore"]),
    ],
    targets: [
        .target(name: "WageJarCore"),
        .testTarget(
            name: "WageJarCoreTests",
            dependencies: ["WageJarCore"]
        ),
    ]
)
