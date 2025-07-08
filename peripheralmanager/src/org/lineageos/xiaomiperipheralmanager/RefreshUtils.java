package org.lineageos.xiaomiperipheralmanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

public final class RefreshUtils {
    private static final String TAG = "RefreshUtils";

    private static final String KEY_PEAK_REFRESH_RATE = "peak_refresh_rate";
    private static final String KEY_MIN_REFRESH_RATE = "min_refresh_rate";
    private static final String KEY_PEN_MODE = "pen_mode";
    private static final String KEY_ORIG_MIN = "orig_min_rate";
    private static final String KEY_ORIG_MAX = "orig_max_rate";

    private static final String PREF_FILE_NAME = "pen_refresh_prefs";

    private final Context mContext;
    private final SharedPreferences mSharedPrefs;

    protected RefreshUtils(Context context) {
        mContext = context;
        mSharedPrefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
    }

    protected void setPenRefreshRate() {
        boolean penActive = mSharedPrefs.getBoolean(KEY_PEN_MODE, false);

        if (!penActive) {
            float currentMin = Settings.System.getFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, 60f);
            float currentMax = Settings.System.getFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, 144f);

            // Save original rates for later
            mSharedPrefs.edit()
                    .putFloat(KEY_ORIG_MIN, currentMin)
                    .putFloat(KEY_ORIG_MAX, currentMax)
                    .putBoolean(KEY_PEN_MODE, true)
                    .apply();

            // Set both min and max to 120 for pen mode
            Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, 120f);
            Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, 120f);

            Log.d(TAG, "Pen mode enabled: forced 120Hz");
        }
    }

    protected void setDefaultRefreshRate() {
        boolean penWasActive = mSharedPrefs.getBoolean(KEY_PEN_MODE, false);

        if (penWasActive) {
            float restoreMin = mSharedPrefs.getFloat(KEY_ORIG_MIN, 60f);
            float restoreMax = mSharedPrefs.getFloat(KEY_ORIG_MAX, 144f);

            Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, restoreMin);
            Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, restoreMax);

            mSharedPrefs.edit().putBoolean(KEY_PEN_MODE, false).apply();

            Log.d(TAG, "Pen mode disabled: restored " + restoreMin + "–" + restoreMax + "Hz");
        }
    }
}
