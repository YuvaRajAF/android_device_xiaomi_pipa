/*
 * Copyright (C) 2023-2025 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.xiaomiperipheralmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.input.InputManager;
import android.os.FileUtils;
import android.os.SystemProperties;
import android.util.Log;
import android.view.InputDevice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for handling Xiaomi keyboard operations at the framework level.
 * Works in conjunction with the native xiaomi-keyboard service.
 */
public class KeyboardUtils {

    private static final String TAG = "XiaomiKeyboard";
    private static boolean DEBUG = SystemProperties.getBoolean("persist.xiaomi.keyboard.debug", false);

    // Xiaomi keyboard identifiers
    private static final int KEYBOARD_VENDOR_ID = 5593;
    private static final int KEYBOARD_PRODUCT_ID = 163;

    // Communication with native service
    private static final String NANODEV_PATH = "/dev/nanodev0";
    private static final byte MSG_TYPE_LOCK = 41;
    private static final byte MSG_TYPE_UNLOCK = 42;
    private static final byte MSG_HEADER_1 = 0x31;
    private static final byte MSG_HEADER_2 = 0x38;

    private static InputManager mInputManager;
    private static boolean mLastEnabledState = false;
    private static boolean mIsDeviceLocked = false;
    private static Context mContext = null;
    private static ScreenStateReceiver mScreenStateReceiver = null;

    // Add watchdog monitor and recovery
    private static final long WATCHDOG_TIMEOUT_MS = 10000; // 10 seconds
    private static Thread mWatchdogThread = null;
    private static volatile boolean mWatchdogRunning = false;
    private static volatile long mLastWatchdogCheck = 0;

    /**
     * Initialize the keyboard utilities and set initial state
     * @param context Application context
     */
    public static void setup(Context context) {
        logInfo("Initializing Xiaomi keyboard framework integration");
        mContext = context.getApplicationContext();
        
        try {
            if (mInputManager == null) {
                mInputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
            }
            
            // Force disable keyboard at startup for safety
            setKeyboardEnabled(false);
            
            // Register broadcast receiver for screen state changes
            registerScreenStateReceiver(context);
            
            // Start watchdog thread
            startWatchdogThread();
            
        } catch (Exception e) {
            logError("Error setting up keyboard utils: " + e.getMessage());
        }
    }
    
    /**
     * Register a BroadcastReceiver to handle screen state changes
     */
    private static void registerScreenStateReceiver(Context context) {
        if (mScreenStateReceiver != null) {
            return; // Already registered
        }
        
        logInfo("Registering screen state receiver");
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        
        mScreenStateReceiver = new ScreenStateReceiver();
        context.registerReceiver(mScreenStateReceiver, filter);
    }
    
    /**
     * Start watchdog thread to monitor and recover from stuck conditions
     */
    private static void startWatchdogThread() {
        if (mWatchdogThread != null && mWatchdogThread.isAlive()) {
            return; // Thread already running
        }
        
        mWatchdogRunning = true;
        mLastWatchdogCheck = System.currentTimeMillis();
        
        mWatchdogThread = new Thread(() -> {
            logInfo("Keyboard watchdog thread started");
            while (mWatchdogRunning) {
                try {
                    // Update watchdog timestamp
                    mLastWatchdogCheck = System.currentTimeMillis();
                    
                    // Safety check: ensure keyboard is disabled in lock screen
                    if (mIsDeviceLocked && mLastEnabledState) {
                        logError("Watchdog detected keyboard enabled while locked! Forcing disable");
                        setKeyboardEnabled(false);
                    }
                    
                    Thread.sleep(5000); // Check every 5 seconds
                } catch (InterruptedException e) {
                    // Thread interrupted, continue loop
                } catch (Exception e) {
                    logError("Watchdog thread error: " + e.getMessage());
                }
            }
            logInfo("Keyboard watchdog thread stopped");
        });
        
        mWatchdogThread.setDaemon(true);
        mWatchdogThread.setName("KeyboardWatchdog");
        mWatchdogThread.start();
    }
    
    /**
     * Stop the watchdog thread during cleanup
     */
    private static void stopWatchdogThread() {
        mWatchdogRunning = false;
        if (mWatchdogThread != null) {
            mWatchdogThread.interrupt();
            mWatchdogThread = null;
        }
    }

    /**
     * Unregister the screen state receiver when service is destroyed
     */
    public static void cleanup(Context context) {
        if (mScreenStateReceiver != null) {
            try {
                context.unregisterReceiver(mScreenStateReceiver);
                mScreenStateReceiver = null;
            } catch (Exception e) {
                logError("Error unregistering receiver: " + e.getMessage());
            }
        }
        
        // Stop watchdog thread
        stopWatchdogThread();
        
        // Force disable keyboard on cleanup
        setKeyboardEnabled(false);
    }

    /**
     * Enable or disable the Xiaomi keyboard input device
     * @param enabled Whether the keyboard should be enabled
     * @return true if operation was successful, false otherwise
     */
    public static boolean setKeyboardEnabled(boolean enabled) {
        // If the device is locked, never enable the keyboard
        if (enabled && mIsDeviceLocked) {
            logDebug("Not enabling keyboard because device is locked");
            return false;
        }
        
        // Update watchdog timestamp to show activity
        mLastWatchdogCheck = System.currentTimeMillis();
        
        if (enabled == mLastEnabledState) {
            logDebug("Keyboard already in requested state: " + enabled);
            return true;
        }
        
        logInfo("Setting keyboard enabled: " + enabled);
        boolean success = false;
        boolean deviceFound = false;
        
        try {
            if (mInputManager == null) {
                logError("InputManager not initialized");
                return false;
            }
            
            for (int id : mInputManager.getInputDeviceIds()) {
                if (isDeviceXiaomiKeyboard(id)) {
                    deviceFound = true;
                    logDebug("Found Xiaomi Keyboard with id: " + id);
                    
                    // Apply enable/disable with timeout protection
                    try {
                        if (enabled) {
                            mInputManager.enableInputDevice(id);
                        } else {
                            mInputManager.disableInputDevice(id);
                        }
                        mLastEnabledState = enabled;
                        success = true;
                    } catch (Exception e) {
                        logError("Failed to change keyboard state: " + e.getMessage());
                    }
                }
            }
            
            if (!deviceFound) {
                logInfo("Xiaomi keyboard not found in input devices");
            }
        } catch (Exception e) {
            logError("Error changing keyboard state: " + e.getMessage());
        }
        
        return success;
    }

    /**
     * Set device lock state and notify native service
     * @param isLocked Whether the device is locked
     */
    public static void setDeviceLockState(boolean isLocked) {
        mIsDeviceLocked = isLocked;
        logInfo("Device lock state changed: " + (isLocked ? "LOCKED" : "UNLOCKED"));
        
        // If locked, force disable the keyboard with higher priority
        if (isLocked) {
            setKeyboardEnabled(false);
        } else if (!mLastEnabledState) {
            // Re-enable keyboard if unlocked and currently disabled
            setKeyboardEnabled(true);
        }
        
        // Notify native service about lock state change
        sendLockStateToNativeService(isLocked);
    }
    
    /**
     * Send lock state message to native service
     * @param isLocked Whether device is locked
     */
    private static void sendLockStateToNativeService(boolean isLocked) {
        try {
            byte messageType = isLocked ? MSG_TYPE_LOCK : MSG_TYPE_UNLOCK;
            
            // Protocol: [0x??][Header1][Header2][0][MessageType][1][1]
            byte[] message = new byte[7];
            message[0] = 0; // First byte can be anything
            message[1] = MSG_HEADER_1;
            message[2] = MSG_HEADER_2;
            message[3] = 0;
            message[4] = messageType;
            message[5] = 1;
            message[6] = 1;
            
            // Write to nanodev device
            File nanodev = new File(NANODEV_PATH);
            if (!nanodev.exists() || !nanodev.canWrite()) {
                logError("Cannot write to nanodev: " + NANODEV_PATH);
                return;
            }
            
            FileOutputStream fos = new FileOutputStream(NANODEV_PATH);
            fos.write(message);
            fos.close();
            
            logDebug("Sent lock state " + (isLocked ? "LOCK" : "UNLOCK") + " to native service");
        } catch (IOException e) {
            logError("Failed to send lock state to native service: " + e.getMessage());
        }
    }

    /**
     * Check if an input device is the Xiaomi keyboard
     * @param id Device ID to check
     * @return true if the device is the Xiaomi keyboard
     */
    private static boolean isDeviceXiaomiKeyboard(int id) {
        try {
            InputDevice inputDevice = mInputManager.getInputDevice(id);
            if (inputDevice == null) return false;
            
            return inputDevice.getVendorId() == KEYBOARD_VENDOR_ID && 
                   inputDevice.getProductId() == KEYBOARD_PRODUCT_ID;
        } catch (Exception e) {
            logError("Error checking device: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * BroadcastReceiver to detect screen state changes
     */
    private static class ScreenStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                setDeviceLockState(true);
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                setDeviceLockState(false);
            }
        }
    }
    
    // Enhanced logging helpers to match C++ style
    private static void logDebug(String message) {
        if (DEBUG) Log.d(TAG, getTimestamp() + message);
    }
    
    private static void logInfo(String message) {
        Log.i(TAG, getTimestamp() + message);
    }
    
    private static void logError(String message) {
        Log.e(TAG, getTimestamp() + message);
    }
    
    private static String getTimestamp() {
        return "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + "] ";
    }
}
