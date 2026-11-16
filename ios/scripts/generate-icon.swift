#!/usr/bin/env swift
// iOS rendition of res/drawable/icon.xml; keep the shared gold tugboat identity.
import AppKit
import CoreGraphics

let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
let catalog = root.appendingPathComponent("BargeBoss/Assets.xcassets")
let output = catalog.appendingPathComponent("AppIcon.appiconset")
try FileManager.default.createDirectory(at: output, withIntermediateDirectories: true)
func color(_ hex: UInt32) -> CGColor {
    CGColor(red: CGFloat((hex >> 16) & 255) / 255, green: CGFloat((hex >> 8) & 255) / 255,
            blue: CGFloat(hex & 255) / 255, alpha: 1)
}
let palettes: [(String, [UInt32])] = [
    ("light", [0x244C54, 0x101E25, 0xEFA94A, 0xEEE7D4, 0x244C54]),
    ("dark", [0x102B33, 0x62838A, 0xEFA94A, 0xEEE7D4, 0x244C54]),
    ("tinted", [0x202020, 0x777777, 0xDDDDDD, 0xFFFFFF, 0x555555])
]
for (name, palette) in palettes {
    let context = CGContext(data: nil, width: 1024, height: 1024, bitsPerComponent: 8,
        bytesPerRow: 4096, space: CGColorSpaceCreateDeviceRGB(),
        bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
    context.scaleBy(x: 1024 / 108, y: 1024 / 108)
    context.setFillColor(color(palette[0]))
    context.fill(CGRect(x: 0, y: 0, width: 108, height: 108))
    context.setFillColor(color(palette[1]))
    context.move(to: CGPoint(x: 22, y: 28))
    for point in [CGPoint(x: 74, y: 28), CGPoint(x: 89, y: 54), CGPoint(x: 74, y: 80), CGPoint(x: 22, y: 80)] {
        context.addLine(to: point)
    }
    context.closePath(); context.fillPath()
    for (index, rect) in [CGRect(x: 28, y: 34, width: 44, height: 40),
                         CGRect(x: 36, y: 40, width: 20, height: 28),
                         CGRect(x: 50, y: 42, width: 7, height: 24)].enumerated() {
        context.setFillColor(color(palette[index + 2])); context.fill(rect)
    }
    let png = NSBitmapImageRep(cgImage: context.makeImage()!).representation(using: .png, properties: [:])!
    try png.write(to: output.appendingPathComponent("icon_\(name).png"))
}
var images: [[String: Any]] = []
for (name, _) in palettes {
    var item: [String: Any] = ["filename": "icon_\(name).png", "idiom": "universal", "platform": "ios", "size": "1024x1024"]
    if name != "light" { item["appearances"] = [["appearance": "luminosity", "value": name]] }
    images.append(item)
}
let info: [String: Any] = ["author": "xcode", "version": 1]
try JSONSerialization.data(withJSONObject: ["images": images, "info": info], options: [.prettyPrinted, .sortedKeys]).write(to: output.appendingPathComponent("Contents.json"))
try JSONSerialization.data(withJSONObject: ["info": info], options: [.prettyPrinted, .sortedKeys]).write(to: catalog.appendingPathComponent("Contents.json"))
print("Generated light, dark and grayscale tinted Barge Boss app icons")
