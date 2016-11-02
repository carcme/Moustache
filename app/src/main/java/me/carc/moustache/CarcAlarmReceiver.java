package me.carc.moustache;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.CallLog;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.frakbot.glowpadbackport.GlowPadView;

import java.io.IOException;
import java.util.Random;

/**
 * Created by carc,me on 16.01.16.
 *
 * Display the incoming call screen
 */
public class CarcAlarmReceiver extends Activity {

    private final String debugTag = AppConstant.DEBUG;

    private Ringtone mRingtone;
    private Context mContext;
    private Button btnEndCall;
    private Vibrator vibrator = null;
    private long callDuration;
    private String mContact, mNumber;
    private GlowPadView glowPad;
    private boolean bCallAccepted = false;
    private boolean bDefaultImageUsed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();

        if(checkCallOngoing()) {
            // Exit if there calll is ongoing or pending
            finish();
        }

        // Release the wake lock
        if(AlarmWakeLock.isHeld())
            AlarmWakeLock.getLock().release();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Show the incoming call screen
        setContentView(R.layout.glowpad_alert);

        setupListeners();
        setupGlowPad();
        setCallScreenColor();
    }

    /**
     * Randomise the colors shown on the answer screen - correspond to the google contacts images
     */
    private void setCallScreenColor() {
        TypedArray colors = getResources().obtainTypedArray(R.array.array_GoogleContactsColors);

        int color, min = 0;
        int max = colors.length();

        // Generate random color
        Random r = new Random();
        color = r.nextInt(max - min + 1) + min;

        TextView contactInfo = (TextView) findViewById(R.id.contactInfo);
        TextView callText = (TextView) findViewById(R.id.tvIncomingCallText);
        TextView contols = (TextView) findViewById(R.id.tvContols);
        ImageView imageView = (ImageView) findViewById(R.id.contact_image);

        ImageButton btnVol = (ImageButton) findViewById(R.id.btnVol);
        ImageButton btnMic = (ImageButton) findViewById(R.id.btnMic);
        ImageButton btnDial = (ImageButton) findViewById(R.id.btnDialPad);
        ImageButton btnPause = (ImageButton) findViewById(R.id.btnPause);

        contactInfo.setBackgroundColor(getResources().getColor(colors.getResourceId(color, R.color.PrimaryBackgroundColor)));
        callText.setBackgroundColor(getResources().getColor(colors.getResourceId(color, R.color.PrimaryBackgroundColor)));
        contols.setBackgroundColor(getResources().getColor(colors.getResourceId(color, R.color.PrimaryBackgroundColor)));

        btnVol.setBackgroundColor(getResources().getColor(colors.getResourceId(color, R.color.PrimaryBackgroundColor)));
        btnMic.setBackgroundColor(getResources().getColor(colors.getResourceId(color, R.color.PrimaryBackgroundColor)));
        btnDial.setBackgroundColor(getResources().getColor(colors.getResourceId(color, R.color.PrimaryBackgroundColor)));
        btnPause.setBackgroundColor(getResources().getColor(colors.getResourceId(color, R.color.PrimaryBackgroundColor)));

        if(bDefaultImageUsed)
            imageView.setBackgroundColor(getResources().getColor(colors.getResourceId(color, R.color.PrimaryBackgroundColor)));
    }

    /**
     *  Create the GlowPad ringer answer display
     */
    private void setupGlowPad() {
        glowPad = (GlowPadView) findViewById(R.id.incomingCallWidget);

        // Arrange depending on the screen size
        DisplayMetrics display = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(display);

        if(display.densityDpi > DisplayMetrics.DENSITY_HIGH) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)glowPad.getLayoutParams();
            params.removeRule(RelativeLayout.BELOW);
            params.addRule(RelativeLayout.BELOW, R.id.contact_image);
            glowPad.setTargetResources(R.array.incoming_call_widget_3way_targets);
        }

        glowPad.setOnTriggerListener(new GlowPadView.OnTriggerListener() {
            @Override
            public void onGrabbed(View v, int handle) {/*Do nothing*/}

            @Override
            public void onReleased(View v, int handle) {
                // Do nothing
                if (vibrator != null)
                    vibrator.vibrate(AppConstant.VIBRATE_PATTERN, 0);
                glowPad.ping();
            }

            @Override
            public void onTrigger(View v, int target) {
                if (vibrator != null) {
                    vibrator.cancel();
                    vibrator = null;
                }

                if (target == 0) {
                    stopNotifications(AppConstant.CALL_ACCEPT);
                    glowPad.setVisibility(View.GONE);
                    btnEndCall.setVisibility(View.VISIBLE);
                    bCallAccepted = true;
                } else if (target == 2) {
                    if (!bCallAccepted) {
                        stopNotifications(AppConstant.CALL_REJECT);
                        logCalls(CallLog.Calls.MISSED_TYPE);
                    } else {
                        stopNotifications(AppConstant.CALL_END);
                        logCalls(CallLog.Calls.INCOMING_TYPE);
                    }
                    finish();
                }
                glowPad.reset(true);
            }

            @Override
            public void onGrabbedStateChange(View v, int handle) {/*Do nothing*/}

            @Override
            public void onFinishFinalAnimation() {/*Do nothing*/}
        });
    }

    /**
     * Localise the listeners
     */
    private void setupListeners() {
        retrieveContactInfo();
        playIncomingNotification();

        btnEndCall = (Button) findViewById(R.id.btnEnd);
        btnEndCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopNotifications(AppConstant.CALL_END);
                logCalls(CallLog.Calls.INCOMING_TYPE);
                finish();
            }
        });
    }

    /**
     * Check if there is an ongoing or pending real call
     * @return Ongoing or pending call is present
     */
    private boolean checkCallOngoing() {
        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE;
    }

    /**
     * Play the ring tone depending on vibrate options. If no ringtone is selected, use the
     * default system ringtone (if there is no vibrate option selected)
     */
    private void playIncomingNotification() {
        String guiRingtone = AppSharedPreferences.getToneUri(mContext);

        if(AppSharedPreferences.getBooleanPref(mContext, AppConstant.PREFNAME_VIBRATE)) {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(AppConstant.VIBRATE_PATTERN, 0);
        }

        try {
            // Ringtone not set and vibrate not ticked
            if(guiRingtone == null) {
                if(vibrator == null) {
                    // Nothing selected - play the default system ringtone
//                    mRingtone = RingtoneManager.getRingtone(mContext,
//                            RingtoneManager.getActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_RINGTONE));
                    mRingtone = RingtoneManager.getRingtone(mContext,RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));

                            mRingtone.play();
                } // vibrator already started
            } else {
                // User selected a ringtone - play it
                mRingtone = RingtoneManager.getRingtone(mContext, Uri.parse(guiRingtone));
                mRingtone.play();
            }
        } catch (Exception e) {
            Toast.makeText(mContext, "Have you selected a ring tone?", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Stop all notifications - switch on the user path of the call
     * @param type Type of notification to stop
     */
    private void stopNotifications(int type) {
        if(vibrator != null)
            vibrator.cancel();
        if (mRingtone != null)
            mRingtone.stop();

        switch (type) {
            case AppConstant.CALL_ACCEPT:
                callDuration = System.currentTimeMillis();
                break;
            case AppConstant.CALL_REJECT:
                break;
            case AppConstant.CALL_END:
                break;
            default:
        }
    }

    /**
     * Get the contact details and display them on the screen
     */
    private void retrieveContactInfo() {
        ImageView imageView = (ImageView) findViewById(R.id.contact_image);
        String photoUri = AppSharedPreferences.getContactInfo(CarcAlarmReceiver.this, AppConstant.CONTACT_IMAGE_URI);

        DisplayMetrics display = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(display);

        switch (display.densityDpi) {
            case DisplayMetrics.DENSITY_LOW:
                imageView.getLayoutParams().width = 75;
                imageView.getLayoutParams().height = 75;
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                imageView.getLayoutParams().width = 100;
                imageView.getLayoutParams().height = 100;
                break;
            case DisplayMetrics.DENSITY_HIGH:
                imageView.getLayoutParams().width = 150;
                imageView.getLayoutParams().height = 150;
                break;
        }

        if (photoUri != null) {
            Uri imageUri = Uri.parse(photoUri);
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            imageView.setImageBitmap(ImageHelper.getRoundedCornerBitmap(bitmap, 500));
//            imageView.setImageURI(Uri.parse(photoUri));
        } else {
//            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.icon_user_alpha);
//            imageView.setImageBitmap(ImageHelper.getRoundedCornerBitmap(bitmap, 500));
            bDefaultImageUsed = true;
            imageView.setBackgroundResource(R.drawable.icon_user_alpha);
        }

        // Build the incoming caller info string
        TextView contactInfo = (TextView) findViewById(R.id.contactInfo);
        mContact = AppSharedPreferences.getContactInfo(CarcAlarmReceiver.this, AppConstant.CONTACT_NAME);
        if (mContact == null)
            mContact = getString(R.string.unknown);

        mNumber = AppSharedPreferences.getContactInfo(CarcAlarmReceiver.this, AppConstant.CONTACT_NUMBER);

        contactInfo.setText(mContact);
        if(mNumber != null)
            if(!mNumber.equals(""))
                contactInfo.setText(mContact + "\n\t\t" + mNumber);
    }

    @Override
    protected void onDestroy() {
        if (mRingtone != null)
            mRingtone.stop();
        if (vibrator != null)
            vibrator.cancel();

        super.onDestroy();
    }

    /**
     * Log the calls
     * @param callType MISSED_TYPE or INCOMING_TYPE
     */
    private void logCalls(int callType) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if(AppSharedPreferences.getBooleanPref(mContext, AppConstant.PREFNAME_LOG_CALL)) {
            ContentValues values = new ContentValues();
            values.put(CallLog.Calls.NUMBER, mNumber != null ? mNumber : mContact);
            values.put(CallLog.Calls.DATE, System.currentTimeMillis());
            values.put(CallLog.Calls.DURATION, (System.currentTimeMillis() - callDuration) / 1000);
            values.put(CallLog.Calls.TYPE, callType);
            values.put(CallLog.Calls.NEW, 1);
            values.put(CallLog.Calls.CACHED_NAME, mContact);
            values.put(CallLog.Calls.CACHED_NUMBER_TYPE, 0);
            values.put(CallLog.Calls.CACHED_NUMBER_LABEL, "");

            mContext.getContentResolver().insert(CallLog.Calls.CONTENT_URI, values);
        }
    }
}