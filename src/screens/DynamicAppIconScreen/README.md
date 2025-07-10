# Dynamic App Icon (React Native)

This section explains the setup and implementation for Dynamic App Icon (React Native).

## Android Native Module Setup (Required)

This section explains how to set up the `AppIconModule` for changing app icon dynamically on Android application.

### 1. Create Alert Layout
#### **File:** [`android/app/src/main/res/layout/icon_changed_dialog.xml`](https://github.com/DharmikSonani/Dynamic-App-Icon/blob/main/android/app/src/main/res/layout/icon_changed_dialog.xml)

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/dialogRoot"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:background="@android:color/white"
    android:orientation="vertical"
    android:padding="24dp"
    android:minWidth="280dp"
    android:gravity="center_horizontal">

    <ImageView
        android:id="@+id/iconImage"
        android:layout_width="70dp"
        android:layout_height="70dp"
        android:layout_marginBottom="16dp"
        android:scaleType="centerCrop" />

    <TextView
        android:id="@+id/iconText"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textColor="#000000"
        android:textSize="16sp"
        android:textStyle="bold"
        android:gravity="center"
        android:lineSpacingExtra="4dp"/>
</LinearLayout>
```

### 2. Create the Native Module
#### **File:** [`android/app/src/main/java/com/<package-name>/appicon/AppIconModule.kt`](https://github.com/DharmikSonani/Dynamic-App-Icon/blob/main/android/app/src/main/java/com/dynamicappicon/appicon/AppIconModule.kt)

```kotlin
package com.<package-name>.appicon

import com.<package-name>.R
import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import com.facebook.react.bridge.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

class AppIconModule(
    reactContext: ReactApplicationContext
) : ReactContextBaseJavaModule(reactContext), Application.ActivityLifecycleCallbacks {

    private val context = reactContext
    private val packageName = context.packageName
    private var currentAlias: String? = null
    private var pendingAlias: String? = null
    private var switchScheduled = false

    override fun getName(): String = "AppIconModule"

    @ReactMethod
    fun getAppIcon(promise: Promise) {
        val activity = currentActivity
        if (activity == null) {
            promise.resolve("ANDROID:ACTIVITY_NOT_FOUND")
            return
        }

        val activityName = activity.componentName.className
        if (activityName.endsWith("MainActivity")) {
            promise.resolve("default")
        } else if (activityName.contains("MainActivity")) {
            val suffix = activityName.substringAfter("MainActivity")
            promise.resolve(suffix)
        } else {
            promise.resolve("Unknown")
        }
    }

    @ReactMethod
    fun changeAppIcon(iconName: String?, promise: Promise) {
        val activity = currentActivity
        if (activity == null) {
            promise.resolve("ANDROID:ACTIVITY_NOT_FOUND")
            return
        }

        val currentClass = activity.componentName.className
        val aliasSuffix = if (iconName.isNullOrEmpty() || iconName.equals("Default", true)) "Default" else iconName
        val newAlias = "$packageName.MainActivity$aliasSuffix"

        if (currentClass == newAlias) {
            showIconChangeAlert(activity, newAlias);
            switchScheduled = false;
            promise.resolve("Already using this icon.")
            return
        }

        if(pendingAlias == null){
            // Register lifecycle callback once
            activity.application.registerActivityLifecycleCallbacks(this)
        }

        pendingAlias = newAlias
        currentAlias = currentClass
        switchScheduled = true

        showIconChangeAlert(activity, newAlias);
        promise.resolve("Successfully change to => newAlias => $newAlias, currentClass => $currentClass");
    }

    private fun performIconSwitch() {
        if (!switchScheduled || pendingAlias == null || currentAlias == null) return

        try {
            val pm = context.packageManager

            // Enable new alias
            pm.setComponentEnabledSetting(
                ComponentName(packageName, pendingAlias!!),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // Disable old alias
            pm.setComponentEnabledSetting(
                ComponentName(packageName, currentAlias!!),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )

            // Relaunch app with new alias
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(packageName, pendingAlias!!)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            context.startActivity(intent)

            // Reset state
            switchScheduled = false
            currentAlias = null
            pendingAlias = null

        } catch (e: Exception) {
            Log.e("AppIconModule", "Error performing icon switch", e)
        }
    }

    private fun showIconChangeAlert(activity: Activity, aliasClassName: String) {
        try {
            // Inflate layout
            val dialogView = activity.layoutInflater.inflate(R.layout.icon_changed_dialog, null)

            // Find views
            val iconImage = dialogView.findViewById<ImageView>(R.id.iconImage)
            val iconText = dialogView.findViewById<TextView>(R.id.iconText)

            val resolveInfo = activity.packageManager.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(packageName),
                PackageManager.MATCH_DISABLED_COMPONENTS
            ).find {
                it.activityInfo.name == aliasClassName
            }

            val iconDrawable = resolveInfo?.activityInfo?.loadIcon(activity.packageManager)
            iconDrawable?.let { iconImage.setImageDrawable(it) }

            // Set app name
            val appName = activity.applicationInfo.loadLabel(activity.packageManager).toString()
            iconText.text = "You have changed the icon for $appName"

            // Show alert
            activity.runOnUiThread {
                AlertDialog.Builder(activity)
                    .setView(dialogView)
                    .setCancelable(false)
                    .setPositiveButton("OK", null)
                    .show()
            }
        } catch (e: Exception) {
            Log.e("AppIconDialog", "Failed to show alert dialog", e)
        }
    }


    // Trigger icon change when app stops (goes to background)
    override fun onActivityStopped(activity: Activity) {
        if (switchScheduled) {
            performIconSwitch()
            activity.application.unregisterActivityLifecycleCallbacks(this)
        }
    }

    // Required empty implementations
    override fun onActivityDestroyed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}
```

### 3. Create the React Package
#### **File:** [`android/app/src/main/java/com/<package-name>/appicon/AppIconPackage.kt`](https://github.com/DharmikSonani/Dynamic-App-Icon/blob/main/android/app/src/main/java/com/dynamicappicon/appicon/AppIconPackage.kt)

```kotlin
package com.<package-name>.appicon

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

class AppIconPackage : ReactPackage {
    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        return listOf(AppIconModule(reactContext))
    }

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return emptyList()
    }
}
```

### 4. Register the Package in `MainApplication.kt`
#### **File:** [`android/app/src/main/java/com/<package-name>/MainApplication.kt`](https://github.com/DharmikSonani/Dynamic-App-Icon/blob/main/android/app/src/main/java/com/dynamicappicon/MainApplication.kt)

Modify the `MainApplication.kt` file to include the `AudioDevicePackage`.

```kotlin
package com.<package-name>

import com.<package-name>.appicon.AppIconPackage // Import Package for AppIconPackage (Required)

class MainApplication : Application(), ReactApplication {
  override val reactNativeHost: ReactNativeHost =
      object : DefaultReactNativeHost(this) {
        override fun getPackages(): List<ReactPackage> =
            PackageList(this).packages.apply {
              add(AppIconPackage()) // Required setup for connecting AppIconPackage with React Native (Required)
            }
    }
}
```

### 5. Rebuild the Project
After adding the native module, rebuild the project to apply the changes:

```sh
cd android
./gradlew clean 
cd ..
npm run android
```

## iOS Native Module Setup (Required)

This section explains how to set up the `AppIconModule` for changing app icon dynamically on iOS application.
- **Note** : Create all Required files from **xCode**.

### 1. Create AppIconModule.swift (Swift)
#### **File:** [`ios/AppIconModule.swift`](https://github.com/DharmikSonani/Dynamic-App-Icon/blob/main/ios/AppIconModule.swift)

```swift
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
```

### 2. Create the Bridging Header
#### **File:** [`ios/<YourApplicationName>-Bridging-Header.h`](https://github.com/DharmikSonani/Dynamic-App-Icon/blob/main/ios/DynamicAppIcon-Bridging-Header.h)
```objc
#import "React/RCTBridgeModule.h"
#import "React/RCTEventEmitter.h"
```

### 3. Create AppIconModule.m (Objective-C)
#### **File:** [`ios/AppIconModule.m`](https://github.com/DharmikSonani/Dynamic-App-Icon/blob/main/ios/AppIconModule.m)

```objc
#import <Foundation/Foundation.h>
#import "React/RCTBridgeModule.h"
#import "React/RCTEventEmitter.h"

@interface RCT_EXTERN_MODULE(AppIconModule, RCTEventEmitter)
RCT_EXTERN_METHOD(changeAppIcon:(NSString *)iconName
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
RCT_EXTERN_METHOD(getAppIcon:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
@end
```

### 4. Rebuild the Project
#### After adding the native module, clear ios/build and rebuild the project to apply the changes:

```sh
cd ios
pod install  
cd ..
npm run ios
```

## Android Dynamic App Icon Setup (Required)

### 1. Generate and Add Icons
#### **Generate Icon:**
Use a service such as https://www.appicon.co in order to generate the platform specific icon files.
#### **Add Icon:**
![Add Android Icons](../../../guide-assets/Add-Android-Icon.png)

### 2. Modify Menifest File
#### **File:** [`android/app/src/main/AndroidManifest.xml`](https://github.com/DharmikSonani/Dynamic-App-Icon/blob/main/android/app/src/main/AndroidManifest.xml)
- Remove **intent-filter** from **activity**
- Add **activity-alias** for each icon (Including default app icon)
```xml
 <activity-alias
    android:name="com.dynamicappicon.MainActivity<icon-name>"
    android:enabled="false"
    android:exported="true"
    android:icon="@mipmap/ic_launcher_<icon-name>"
    android:targetActivity=".MainActivity">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity-alias>
```
- **Note**: The icon-name is case sensetive. Only the Default activity-alias have ***android:enabled="true"***
- After Applying changes your AndroidManifest file look like
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
      android:name=".MainApplication"
      android:label="@string/app_name"
      android:icon="@mipmap/ic_launcher"
      android:allowBackup="false"
      android:theme="@style/AppTheme"
      android:usesCleartextTraffic="true"
      android:requestLegacyExternalStorage="true"
      android:supportsRtl="true">
      <activity
        android:name=".MainActivity"
        android:label="@string/app_name"
        android:configChanges="keyboard|keyboardHidden|orientation|screenLayout|screenSize|smallestScreenSize|uiMode"
        android:launchMode="singleTask"
        android:windowSoftInputMode="adjustResize"
        android:screenOrientation="portrait" 
        android:exported="true"/>
      
        <activity-alias
            android:name="com.<package-name>.MainActivityDefault"
            android:enabled="true"
            android:exported="true"
            android:icon="@mipmap/ic_launcher"
            android:targetActivity=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity-alias>

        <activity-alias
            android:name="com.<package-name>.MainActivityAlternate"
            android:enabled="false"
            android:exported="true"
            android:icon="@mipmap/ic_launcher_alternate"
            android:targetActivity=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity-alias>
    </application>
</manifest>
```

### 3. Rebuild the Project
After all, rebuild the project to apply the changes:

```sh
cd android
./gradlew clean 
cd ..
npm run android
```

## iOS Dynamic App Icon Setup (Required)

### 1. Generate and Add Icons
#### **Generate Icon:** 
Use a service such as https://www.appicon.co in order to generate the platform specific icon files.
#### **Add Icons:**
![Add iOS Icons](../../../guide-assets/Add-iOS-Icon.png)

#### **Configure:**
In XCode, in your app's **General settings**, under **App Icons and Launch Screen**, set **"App Icon"** to **Default** and **check** the **"Include all app icon assets"** checkbox below.

![Configure iOS Icons](../../../guide-assets/Configue-iOS-Icon.png)

### 2. Modify Info.plist File
#### **File:** [`ios/<YourAppName>/Info.plist`](https://github.com/DharmikSonani/Dynamic-App-Icon/blob/main/ios/DynamicAppIcon/Info.plist)
- Add **CFBundleIcons**
```
<key>CFBundleIcons</key>
<dict>
    <key>UINewsstandIcon</key>
    <dict>
        <key>CFBundleIconFiles</key>
        <array>
            <string>Default</string>
        </array>
        <key>UINewsstandBindingType</key>
        <string>UINewsstandBindingTypeMagazine</string>
        <key>UINewsstandBindingEdge</key>
        <string>UINewsstandBindingEdgeLeft</string>
    </dict>
    <key>CFBundlePrimaryIcon</key>
    <dict>
        <key>CFBundleIconName</key>
        <string></string>
        <key>CFBundleIconFiles</key>
        <array>
            <string>Default</string>
        </array>
        <key>UIPrerenderedIcon</key>
        <false/>
    </dict>
    <key>CFBundleAlternateIcons</key>
    <dict>
        ...
    </dict>
</dict>
```
- Add **icon-key** for each icon in **CFBundleAlternateIcons** (icon-key is case sensetive)
```
<key><icon-key></key>
<dict>
    <key>CFBundleIconFiles</key>
    <array>
        <string><icon-key></string>
    </array>
    <key>UIPrerenderedIcon</key>
    <false/>
</dict>	
```
- After Applying changes your Info.plist file look like

```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleIcons</key>
    <dict>
        <key>UINewsstandIcon</key>
        <dict>
            <key>CFBundleIconFiles</key>
            <array>
                <string>Default</string>
            </array>
            <key>UINewsstandBindingType</key>
            <string>UINewsstandBindingTypeMagazine</string>
            <key>UINewsstandBindingEdge</key>
            <string>UINewsstandBindingEdgeLeft</string>
        </dict>
        <key>CFBundlePrimaryIcon</key>
        <dict>
            <key>CFBundleIconName</key>
            <string></string>
            <key>CFBundleIconFiles</key>
            <array>
                <string>Default</string>
            </array>
            <key>UIPrerenderedIcon</key>
            <false/>
        </dict>
        <key>CFBundleAlternateIcons</key>
        <dict>
            <key>Alternate</key>
            <dict>
                <key>CFBundleIconFiles</key>
                <array>
                    <string>Alternate</string>
                </array>
                <key>UIPrerenderedIcon</key>
                <false/>
            </dict>
        </dict>
    </dict>
</dict>
</plist>
```

### 3. Rebuild the Project
#### After all, clear ios/build and rebuild the project to apply the changes:

```sh
cd ios
pod install  
cd ..
npm run ios
```

## Usage - useAppIcons hook
#### Code [`src/hooks/useAppIcons.js`](https://github.com/DharmikSonani/Dynamic-App-Icon/blob/main/src/hooks/useAppIcons.js)

```javascript
import { NativeModules, Platform } from 'react-native';
const { AppIconModule } = NativeModules;

export const getAppIcon = async () => { return await AppIconModule?.getAppIcon() };

export const handleIconChange = async (icon) => {
    try {
        const currentIcon = await getAppIcon();
        if (currentIcon?.toLowerCase() !== icon?.toLowerCase() || Platform.OS === 'android') {
            await AppIconModule?.changeAppIcon(icon);
        }
    } catch (error) {
        console.log(error);
    }
}

export const useDefaultIcon = () => handleIconChange('default');
export const useAlternateIcon = () => handleIconChange('Alternate');

/*
- Use "useCurrentIcon" hook only on Android in specific cases, such as Google authentication,
- where the Android activity might be stopped or backgrounded unexpectedly.
*/
export const useCurrentIcon = async () => Platform.OS === 'android' ? handleIconChange(await getAppIcon()) : {};
```

#### Use in `App.js`
```javascript
import { StyleSheet, Text, View } from 'react-native'
import React from 'react'
import { useDefaultIcon, useAlternateIcon } from './src/hooks/useAppIcons'

const App = () => {
    // useDefaultIcon();
    useAlternateIcon();
    return (
        <View>
            <Text>App</Text>
        </View>
    )
}

export default App

const styles = StyleSheet.create({})
```