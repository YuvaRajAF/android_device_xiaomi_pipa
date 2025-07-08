package org.lineageos.xiaomiperipheralmanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

public final class RefreshUtils {
    private static final String TAG = "RefreshUtils";

    private static final String KEY_PEAK_REFRESH_RATE = "peak_refresh_rate";
    private static final String KEY_MIN_REFRESH_RATE = "min_refresh_rate";

    private static final String PREF_FILE_NAME = "pen_refresh_prefs";
    private static final String PREF_ORIG_MIN = "orig_min_refresh_rate";
    private static final String PREF_ORIG_PEAK = "orig_peak_refresh_rate";
    private static final String PREF_PEN_MODE = "pen_mode";

    private final Context mContext;
    private final SharedPreferences mPrefs;

    protected RefreshUtils(Context context) {
        mContext = context.getApplicationContext();
        mPrefs = mContext.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
    }

    protected void setPenRefreshRate() {
        if (mPrefs.getBoolean(PREF_PEN_MODE, false)) {
            Log.d(TAG, "setPenRefreshRate: Pen mode already active. Skipping.");
            return;
        }

        float currentPeak = Settings.System.getFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, 144f);
        float currentMin = Settings.System.getFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, 60f);

        Log.d(TAG, "setPenRefreshRate: Detected peak = " + currentPeak + ", min = " + currentMin);

        if (Math.abs(currentPeak - 144f) < 1f) {
            Log.d(TAG, "setPenRefreshRate: Switching to 120 Hz for pen compatibility.");

            mPrefs.edit()
                .putFloat(PREF_ORIG_MIN, currentMin)
                .putFloat(PREF_ORIG_PEAK, currentPeak)
                .putBoolean(PREF_PEN_MODE, true)
                .apply();

            Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, 120f);
            Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, 120f);
        } else {
            Log.d(TAG, "setPenRefreshRate: Current peak is not 144 Hz. No change needed.");
        }
    }

    protected void setDefaultRefreshRate() {
        if (!mPrefs.getBoolean(PREF_PEN_MODE, false)) {
            Log.d(TAG, "setDefaultRefreshRate: Pen mode not active. Skipping.");
            return;
        }

        float origMin = mPrefs.getFloat(PREF_ORIG_MIN, 60f);
        float origPeak = mPrefs.getFloat(PREF_ORIG_PEAK, 144f);

        Log.d(TAG, "setDefaultRefreshRate: Restoring to min = " + origMin + ", peak = " + origPeak);

        Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, origMin);
        Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, origPeak);

        mPrefs.edit().putBoolean(PREF_PEN_MODE, false).apply();
    }
}
