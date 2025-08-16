package com.liskovsoft.youtubeapi.app.potokennp2

import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Build.VERSION.SDK_INT
import android.os.PowerManager
import app.revanced.extension.shared.utils.Logger

internal fun isThermalServiceAvailable(context: Context): Boolean {
    // Only Android 10 has the issue
    if (SDK_INT != 29)
        return true

    val powerService = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false

    val listener = PowerManager.OnThermalStatusChangedListener {
        // NOP
    }

    return try {
        powerService.addThermalStatusListener(listener)
        true
    } catch (e: Exception) {
        false
    } finally {
        powerService.removeThermalStatusListener(listener)
    }
}

internal fun hasThermalServiceBug(context: Context): Boolean {
    // Only Android 10 has the issue
    if (SDK_INT != 29)
        return false

    val powerService = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false

    val listener = PowerManager.OnThermalStatusChangedListener {
        // NOP
    }

    return try {
        powerService.addThermalStatusListener(listener)
        false
    } catch (e: Exception) {
        true
    } finally {
        powerService.removeThermalStatusListener(listener)
    }
}

/**
 * NOTE: You will need to declare the android.permission.USB permission in your AndroidManifest.xml
 *
 * Trying to fix: NullPointerException: 'int android.hardware.usb.UsbDevice.getInterfaceCount()' on a null object reference
 */
internal fun hasUsbServiceBug(context: Context): Boolean {
    // Only Android 12 and up has the issue
    if (SDK_INT != 31)
        return false

    val manager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return false

    val deviceList = manager.deviceList
    if (deviceList == null || deviceList.isEmpty()) {
        return false
    }

    for (device in deviceList.values) {
        if (device == null) {
            Logger.printException { "Found a null USB device in the system's device list" }
            return true
        }

        try {
            Logger.printDebug { "Device: ${device.deviceName}, Interface Count: ${device.interfaceCount}" }

            // You could add more checks here, e.g., for specific interfaces
            // or other properties that might be null.
        } catch (e: Exception) {
            return true
        }
    }

    return false
}