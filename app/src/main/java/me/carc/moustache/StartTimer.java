package me.carc.moustache;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/**
 * Created by Carc.me on 18.02.16.
 * <p/>
 * Handle the timer to start the fake call
 */
public class StartTimer {

    private final String TAG = AppConstant.DEBUG;

    final private Context mContext;
    final private AlarmManager alarmManager;
    private long mDelay = 500;
    private boolean mShowAds = false;

    public StartTimer(Context context) {
        mContext = context;
        alarmManager = (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
    }

    public void setDelay(long delay) {
        mDelay = delay;
    }

    public void incAds(boolean showAds) {
        mShowAds = showAds;
    }

    public void start() {

        // Setup the alarm with the new incoming call
        Intent intentAlarm = new Intent(mContext, CarcAlarmReceiver.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(mContext,
                AppConstant.MOUSTACHE_INTENT_ID, intentAlarm, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()
                + mDelay, pendingIntent);

        // Used the Stealth mode - Show the advert :)
        if(mShowAds) {
            AppSharedPreferences.incrementAdvertCount(mContext, AppConstant.PREFNAME_AD_INTER);
            AppSharedPreferences.setBooleanPref(mContext, AppConstant.SHOW_AD_ON_START, true);
        }
    }

}
