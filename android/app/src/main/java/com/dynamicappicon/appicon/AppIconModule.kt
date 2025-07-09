package com.dynamicappicon.appicon

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import com.facebook.react.bridge.*

class AppIconModule(
    reactContext: ReactApplicationContext
) : ReactContextBaseJavaModule(reactContext), Application.ActivityLifecycleCallbacks {

    private val context = reactContext
    private val packageName = context.packageName
    private var componentClass: String = ""
    private val classesToKill = mutableSetOf<String>()
    private var iconChanged = false

    override fun getName(): String = "AppIconModule"

    @ReactMethod
    fun getAppIcon(promise: Promise) {
        val activity = currentActivity
        if (activity == null) {
            promise.reject("ANDROID:ACTIVITY_NOT_FOUND")
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
            promise.reject("ANDROID:ACTIVITY_NOT_FOUND")
            return
        }

        val pm = activity.packageManager
        val currentClass = activity.componentName.className
        val baseAlias = if (iconName.isNullOrEmpty() || iconName.equals("Default", true)) "MainActivityDefault"
                        else "MainActivity$iconName"
        var newAliasClass: String? = null

        try {
            val resolveInfos = pm.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(packageName),
                PackageManager.MATCH_DISABLED_COMPONENTS
            )

            for (info in resolveInfos) {
                val className = info.activityInfo.name
                if (className.endsWith(baseAlias)) {
                    newAliasClass = className
                    break
                }
            }

            if (newAliasClass == null) {
                promise.reject("ANDROID:ICON_ALIAS_NOT_FOUND", "Alias not found for $baseAlias")
                return
            }

            if (currentClass == newAliasClass) {
                promise.reject("ANDROID:ICON_ALREADY_USED", "Already using this icon.")
                return
            }

            // Enable new
            pm.setComponentEnabledSetting(
                ComponentName(packageName, newAliasClass),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // Disable old
            pm.setComponentEnabledSetting(
                ComponentName(packageName, currentClass),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )

            // Relaunch app with new icon alias
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(packageName, newAliasClass!!)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            context.startActivity(intent)
            activity.finish()

            promise.resolve(baseAlias.removePrefix("MainActivity"))

        } catch (e: Exception) {
            Log.e("changeAppIcon", "Error switching icon", e)
            promise.reject("ANDROID:ICON_CHANGE_FAILED", e)
        }
    }


    private fun completeIconChange() {
        if (!iconChanged) return

        val activity = currentActivity ?: return
        val pm = activity.packageManager

        classesToKill.remove(componentClass)
        for (cls in classesToKill) {
            pm.setComponentEnabledSetting(
                ComponentName(packageName, cls),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        classesToKill.clear()
        iconChanged = false
    }

    override fun onActivityDestroyed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) { completeIconChange() }
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}
