package org.lineageos.xiaomiperipheralmanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

public final class RefreshUtils {
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
        mPrefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
    }

    protected void setPenRefreshRate() {
        if (mPrefs.getBoolean(PREF_PEN_MODE, false)) return;

        float currentPeak = Settings.System.getFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, 144f);
        float currentMin = Settings.System.getFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, 60f);

        // Only override if current peak is 144Hz (pen doesn't work at 144)
        if (currentPeak == 144f) {
            mPrefs.edit()
                .putFloat(PREF_ORIG_MIN, currentMin)
                .putFloat(PREF_ORIG_PEAK, currentPeak)
                .putBoolean(PREF_PEN_MODE, true)
                .apply();

            Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, 120f);
            Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, 120f);
        }
    }

    protected void setDefaultRefreshRate() {
        if (!mPrefs.getBoolean(PREF_PEN_MODE, false)) return;

        float origMin = mPrefs.getFloat(PREF_ORIG_MIN, 60f);
        float origPeak = mPrefs.getFloat(PREF_ORIG_PEAK, 144f);

        Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, origMin);
        Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, origPeak);

        mPrefs.edit().putBoolean(PREF_PEN_MODE, false).apply();
    }
}
