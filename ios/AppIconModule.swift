import Foundation
import AVFoundation
import React
import UIKit

@objc(AppIconModule)
class AppIconModule: NSObject, RCTBridgeModule{
  static func moduleName() -> String! {
    return "AppIconModule";
  }
  
  @objc
  func changeAppIcon(_ iconName: String?, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    guard UIApplication.shared.supportsAlternateIcons else {
      rejecter("not_supported", "Alternate icons not supported on this device", nil)
      return
    }

    let newIconName = (iconName?.isEmpty ?? true || iconName == "default" || iconName == "Default") ? nil : iconName

    DispatchQueue.main.async {
      UIApplication.shared.setAlternateIconName(newIconName) { error in
        if let error = error {
          rejecter("change_failed", "Failed to change icon", error)
        } else {
          resolver("Icon changed successfully \(newIconName)")
        }
      }
    }
  }

  @objc
  func getAppIcon(_ resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    guard UIApplication.shared.supportsAlternateIcons else {
      rejecter("not_supported", "Alternate icons not supported on this device", nil)
      return
    }

    let currentName = UIApplication.shared.alternateIconName ?? "default"
    resolver(currentName)
  }
  
  @objc
  static func requiresMainQueueSetup() -> Bool {
      return true
  }
}
