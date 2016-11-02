package me.carc.moustache;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by Carc.me on 16.01.16.
 *
 * Store the local settings for the application
 */
class AppSharedPreferences {

    private static final String debug_tag = AppConstant.DEBUG;

    // Set Time parameters
    public static void setElapsedTimeoutValue(Context context, long prefValue) {
        android.content.SharedPreferences sharedPreferences =
                context.getSharedPreferences(AppConstant.PREF_FILE_NAME, Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(AppConstant.PREFNAME_ELAPSED_TIMEOUT, prefValue);
        editor.apply();
    }
    public static void setTimeoutSpinnerPosition(Context context, int prefValue) {
        android.content.SharedPreferences sharedPreferences =
                context.getSharedPreferences(AppConstant.PREF_FILE_NAME, Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(AppConstant.PREFNAME_SPINNER_POS, prefValue);
        editor.apply();
    }

    // Get Time parameters
    public static long getElapsedTimeoutValue(Context context) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(AppConstant.PREF_FILE_NAME, Context.MODE_PRIVATE);
        // Default 5 seconds if not previously set
        return sharedPreferences.getLong(AppConstant.PREFNAME_ELAPSED_TIMEOUT, 500);
    }
    public static int getTimeoutSpinnerPosition(Context context) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(AppConstant.PREF_FILE_NAME, Context.MODE_PRIVATE);
        // Default 5 seconds if not previously set
        return sharedPreferences.getInt(AppConstant.PREFNAME_SPINNER_POS, 0);
    }

    public static void setBooleanPref(Context context, String booleanItem, Boolean prefValue) {
        android.content.SharedPreferences sharedPreferences =
                context.getSharedPreferences(AppConstant.PREF_FILE_NAME, Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(booleanItem, prefValue);
        editor.apply();
    }

    // Get Time parameters
    public static boolean getBooleanPref(Context context, String booleanItem) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(AppConstant.PREF_FILE_NAME, Context.MODE_PRIVATE);
        // Default 5 seconds if not previously set
        return sharedPreferences.getBoolean(booleanItem, false);

    }

    // Set Tone URI
    public static void setToneUri(Context context, String prefValue) {
        android.content.SharedPreferences sharedPreferences =
                context.getSharedPreferences(AppConstant.PREF_FILE_NAME, Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(AppConstant.PREFNAME_TONE_URI, prefValue);
        editor.apply();
    }

    // Set Tone URI
    public static void removePreference(Context context, String prefName) {
        android.content.SharedPreferences sharedPreferences =
                context.getSharedPreferences(AppConstant.PREF_FILE_NAME, Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(prefName);
        editor.apply();
    }

    // Get Tone URI
    public static String getToneUri(Context context) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(AppConstant.PREF_FILE_NAME, Context.MODE_PRIVATE);
        // Default 5 seconds if not previously set
        return sharedPreferences.getString(AppConstant.PREFNAME_TONE_URI, null);
    }

    // Get and Set Contact Information
    public static void setContactInfo(Context context, String prefName, String prefValue) {
        android.content.SharedPreferences sharedPreferences =
                context.getSharedPreferences(AppConstant.PREF_FILE_NAME, Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(prefName, prefValue);
        editor.apply();
    }

    // Get Time parameters
    public static String getContactInfo(Context context, String prefName) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(AppConstant.PREF_FILE_NAME, Context.MODE_PRIVATE);
        // Default 5 seconds if not previously set
        return sharedPreferences.getString(prefName, null);
    }


    public static int incrementAdvertCount(Context context, String id) {
        android.content.SharedPreferences sharedPreferences =
                context.getSharedPreferences(AppConstant.PREF_FILE_NAME, Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
        int count = getAdvertCount(context, id) + 1;
        Log.d(debug_tag, id + " ad count = " + count);
        editor.putInt(id, count);
        editor.apply();
        return count;
    }

    public static int getAdvertCount(Context context, String id) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(AppConstant.PREF_FILE_NAME, Context.MODE_PRIVATE);
//        int i = sharedPreferences.getInt(AppConstant.PREFNAME_ADVERT_COUNT, 0);
//        return i;
        return sharedPreferences.getInt(id, 0);
    }

    public static void resetAdvertCount(Context context, String id) {
        android.content.SharedPreferences sharedPreferences =
                context.getSharedPreferences(AppConstant.PREF_FILE_NAME, Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(id, 0);
        editor.apply();
    }

}