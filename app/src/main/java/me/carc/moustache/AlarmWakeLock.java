package me.carc.moustache;

import android.content.Context;
import android.os.PowerManager;

/**
 * Created by Carc.me on 18.02.16.
 * <p/>
 * Create static wake lock to allow aquire and release
 */
public class AlarmWakeLock {

    private static PowerManager.WakeLock lock;


    public static boolean isHeld() {
        return lock != null && lock.isHeld();
    }

    public static PowerManager.WakeLock getLock() {
        if (lock != null)
            return lock;
        return null;
    }

    static PowerManager.WakeLock createPartialWakeLock(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlarmWakeLock");
        return lock;
    }
}