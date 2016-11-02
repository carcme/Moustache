package me.carc.moustache;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Created by Carc.me on 18.02.16.
 * <p/>
 * TODO: Add a class header comment!
 */
public class PowerSendMessage extends BroadcastReceiver {

    private final String TAG = AppConstant.DEBUG;

    private Context mContext;
    private static int mCode = 0;

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
            resetCode();

        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            mCode++;
        } else if (intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")) {
            if (mCode == 2) {
                Log.d(TAG, "Unlocked");
                resetCode();
                mContext = context;
                launchAlarmCall();
            } else if(mCode > 2)
                resetCode();
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            // Do nothing
        } else
            resetCode();
    }

    /**
     * Wrapper for start fake call
     *
     */
    private void launchAlarmCall() {
        if(doChecks())
            processRemoteCall();
    }

    /**
     * Check it is ok to start the fake call
     *
     * @return true if ok
     */
    private boolean doChecks() {
        // Exit if user trying to adjust mp3 volume??
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if(audioManager.isMusicActive())
            return false;

        // Init this now - wasn't required previously
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Activity.ALARM_SERVICE);

        // Exit if on-going or pending call
        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
            //check if alarm is set and cancel if needed - don't want fake call going off during call!!
            Intent intentAlarm = new Intent(mContext, CarcAlarmReceiver.class);
            final PendingIntent pendingIntent = PendingIntent.getActivity(mContext,
                    AppConstant.BROADCAST_INTENT_ID, intentAlarm, PendingIntent.FLAG_CANCEL_CURRENT);
            alarmManager.cancel(pendingIntent);
            return false;
        }

        // User has disabled the stealth mode in the setttings
        return !AppSharedPreferences.getBooleanPref(mContext, AppConstant.PREFNAME_NO_STEALTH);
    }

    /**
     * Checks passed - Active the fake caller screen from the standby /  lock screen
     */
    private void processRemoteCall(){
        StartTimer timer = new StartTimer(mContext);
        timer.incAds(true);
        timer.setDelay(AppSharedPreferences.getElapsedTimeoutValue(mContext));
        timer.start();
    }

    /**
     * Reset the unlock code
     */
    public void resetCode() {
        mCode = 0;
    }

/*
    private void suggestLockScreen() {


        // We need a lock screen
        KeyguardManager keyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        if (!keyguardManager.inKeyguardRestrictedInputMode()) {
            if (!keyguardManager.isKeyguardSecure()) {
                suggestLockScreen();
                return false;
            }
        }

        -------------------------------------------

        // Default is false
        if(!AppSharedPreferences.getBooleanPref(mContext, AppConstant.PREFNAME_LOCK_NO_SHOW)) {

            // Make sure screen is either locked or off
            Toast.makeText(mContext, "Suggest you use a lock screen", Toast.LENGTH_SHORT).show();
            AppSharedPreferences.setBooleanPref(mContext, AppConstant.PREFNAME_LOCK_NO_SHOW, true);
        }
    }
*/
}
