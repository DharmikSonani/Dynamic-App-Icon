package com.dynamicappicon.appicon

import com.dynamicappicon.R
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
        val aliasSuffix = if (iconName.isNullOrEmpty() || iconName.equals("Default", true)) "Default" else iconName
        val newAlias = "$packageName.MainActivity$aliasSuffix"

        if (currentClass == newAlias) {
            showIconChangeAlert(activity, newAlias);
            switchScheduled = false;
            promise.reject("ANDROID:ICON_ALREADY_USED", "Already using this icon.")
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
