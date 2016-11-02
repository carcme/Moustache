package me.carc.moustache;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.ContactsContract;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.meyerlaurent.cactv.AutoCompleteContactTextView;
import com.meyerlaurent.cactv.People;

import me.carc.moustache.helpers.Spinner2;

/**
 * Created by Carc.me on 16.01.16.
 * <p/>
 * The main activity for Moustache
 */
public class Moustache extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = AppConstant.DEBUG;

    private AutoCompleteContactTextView mContactName;
    private TextView mContactNumber;
    private ImageView mContactmage;

    private Context mContext;
    private long nTimeoutValue = 500;
    private String nTimeoutString;

    private boolean bPlayToneSelect = false;
    private boolean bSelectContact = false;
    private boolean bStart = false;

    // Interstitial Advert
    private InterstitialAd mInterstitialAd;
    private AdView adViewTop;
    private AdView adViewBot;
    private boolean bShowAdvertInterstitial = false;
    private boolean bShowAdvertBannerTop = true;
    private boolean bShowAdvertBannerBot = true;;

    /**
     * onCreate
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this.getApplicationContext();
        setContentView(R.layout.plain_layout);

        ActionBar actionBar = getSupportActionBar();
        if(BuildConfig.FLAVOR == "pro")
            actionBar.setTitle(getString(R.string.app_name_pro));
        else if(BuildConfig.FLAVOR == "no_ads")
            actionBar.setTitle(getString(R.string.app_name_no_ads));


        registerStealthAction();

        DisplayMetrics display = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(display);

        if(BuildConfig.FLAVOR == "free") {
            // Include the adverts
            AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice(getString(R.string.MyHandset))
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .build();

            // Screen top advert
            adViewTop = (AdView) findViewById(R.id.adViewTop);
            adViewTop.loadAd(adRequest);

            // Screen bottom advert - maybe make this bigger size depending on the screen available
            adViewBot = (AdView) findViewById(R.id.adViewBottom);
            adViewBot.loadAd(adRequest);

            // Full screen advert setup
            mInterstitialAd = new InterstitialAd(this);
            mInterstitialAd.setAdUnitId(getString(R.string.interstitial_advert));
            requestNewInterstitial();

            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdLeftApplication() {
                    // Reset the adverts counter
                    super.onAdLeftApplication();
                    AppSharedPreferences.resetAdvertCount(mContext, AppConstant.PREFNAME_AD_INTER);
                    bShowAdvertInterstitial = false;
                }

                @Override
                public void onAdClosed() {
                    if (bPlayToneSelect) {
                        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
                        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone");
                        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                        startActivityForResult(intent, AppConstant.PENDING_INTENT_RINGTONE);
                        bPlayToneSelect = false;
                    } else if (bSelectContact) {
                        Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                        i.putExtra(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, true);
                        startActivityForResult(i, AppConstant.PICK_CONTACT);
                        bSelectContact = false;
                    } else if (bStart) {

                        StartTimer timer = new StartTimer(mContext);
                        timer.incAds(false);
                        timer.setDelay(nTimeoutValue);
                        timer.start();
                        setPowerManagement(); // use wake lock to allow timer to trigger when ready

                        Toast.makeText(Moustache.this, "Incoming call in " + nTimeoutString, Toast.LENGTH_SHORT).show();

                        bStart = false;
                        moveTaskToBack(true);
                    }
                    requestNewInterstitial();
                }
            });

            adViewTop.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    animateTopAdvert(true, false);
                }

                public void onAdLeftApplication() {
                    super.onAdLeftApplication();
                    AppSharedPreferences.resetAdvertCount(mContext, AppConstant.PREFNAME_AD_BANNER_TOP);
                    bShowAdvertBannerTop = false;
                    animateTopAdvert(bShowAdvertBannerTop, false);
                }
            });
            adViewBot.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    animateBottomAdvert(bShowAdvertBannerBot);
                }

                @Override
                public void onAdLeftApplication() {
                    super.onAdLeftApplication();
                    AppSharedPreferences.resetAdvertCount(mContext, AppConstant.PREFNAME_AD_BANNER_BOT);
                    bShowAdvertBannerBot = false;
                    animateBottomAdvert(bShowAdvertBannerBot);
                }
            });

            // Check if should show Interstitial advert
            bShowAdvertInterstitial = AppSharedPreferences.getAdvertCount(mContext, AppConstant.PREFNAME_AD_INTER) >
                    getResources().getInteger(R.integer.advertCountInter);

            // Increment the top advert count and animate the banner if needed
            bShowAdvertBannerTop = AppSharedPreferences.incrementAdvertCount(mContext, AppConstant.PREFNAME_AD_BANNER_TOP) >=
                    getResources().getInteger(R.integer.advertCountBanner);
            animateTopAdvert(bShowAdvertBannerTop, false);

            // Increment the bottom advert count and animate the banner if needed
            bShowAdvertBannerBot = AppSharedPreferences.incrementAdvertCount(mContext, AppConstant.PREFNAME_AD_BANNER_BOT) >=
                    getResources().getInteger(R.integer.advertCountBanner);
            animateBottomAdvert(bShowAdvertBannerBot);
        }
        // End of adverts

        mContactName = (AutoCompleteContactTextView) findViewById(R.id.textView_SelectedName);
        mContactName.setType(AutoCompleteContactTextView.TYPE_OF_DATA.PHONE);
        mContactName.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mContactName.setText("");
                    mContactNumber.setText("");
                    setContactImage(null);
                }
                return false;
            }
        });

        // Catch the keyboard finish button
        mContactName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    hideSoftKeyboard();
                    return true;
                }
                return false;
            }
        });

        // Allow room for the keyboard - hide the adverts while edit text has focus
        mContactName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                if (hasFocus) {
                    animateTopAdvert(false, false);
                    RelativeLayout layout = (RelativeLayout) findViewById(R.id.adBotLayout);
                    layout.setVisibility(View.GONE);
                } else
                    // Bottom advert show or hidden on close keyboard event :)
                    animateTopAdvert(true, false);
            }
        });

        // Override from AutoCompleteContactTextView
        mContactName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                People selected = (People) mContactName.getAdapter().getItem(position);
                mContactName.setText(selected.getName().toString());
                mContactNumber.setText(selected.getData().toString());
                if (selected.getPicture() != null) {
                    // TODO: Not tested
                    mContactmage.setImageBitmap(selected.getPicture());
                }
                hideSoftKeyboard();
            }
        });

        mContactNumber = (TextView) findViewById(R.id.textView_SelectedNumber);
        mContactmage = (ImageView) findViewById(R.id.imgBtn_SelectedImage);

        mContactName.setText(AppSharedPreferences.getContactInfo(mContext, AppConstant.CONTACT_NAME));
        mContactNumber.setText(AppSharedPreferences.getContactInfo(mContext, AppConstant.CONTACT_NUMBER));
        setContactImage(AppSharedPreferences.getContactInfo(mContext, AppConstant.CONTACT_IMAGE_URI));

        final Spinner2 spinner = (Spinner2) findViewById(R.id.spinner_time);
        spinner.setSelection(true, AppSharedPreferences.getTimeoutSpinnerPosition(mContext), true);
        spinner.setOnItemSelectedSpinner2Listener(new Spinner2.OnItemSelectedSpinner2Listener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                int[] ms_list = getResources().getIntArray(R.array.intervals_ms);
                int spinnerPosition = pos;
                nTimeoutValue = ms_list[pos];
                AppSharedPreferences.setElapsedTimeoutValue(mContext, nTimeoutValue);
                AppSharedPreferences.setTimeoutSpinnerPosition(mContext, spinnerPosition);
                String[] listList = getResources().getStringArray(R.array.entries_update_intervals);
                nTimeoutString = listList[spinnerPosition];
                hideSoftKeyboard();
            }

            public void onNothingSelected(AdapterView<?> parent) {
                hideSoftKeyboard();
            }
        });

        ImageButton startCall = (ImageButton) findViewById(R.id.imgBtnCall);
        ImageButton contacts = (ImageButton) findViewById(R.id.imgBtn_SelectedImage);
        ImageButton ringtone  =(ImageButton) findViewById(R.id.imgBtnMusic);
        CheckBox chkVibrate = (CheckBox) findViewById(R.id.ckbVibrate);
        startCall.setOnClickListener(this);
        ringtone.setOnClickListener(this);
        contacts.setOnClickListener(this);
        chkVibrate.setChecked(AppSharedPreferences.getBooleanPref(mContext, AppConstant.PREFNAME_VIBRATE));
        chkVibrate.setOnClickListener(this);

        if(BuildConfig.FLAVOR == "free") {
            showAdvertsInformationDialog();
        }

        /* DEBUGGING - put test stuff here */
    }

    /**
     * Animate the showing and hiding of adverts
     * @param animationIn
     */
    private void animateTopAdvert(final boolean animationIn, final boolean override) {

        if(BuildConfig.FLAVOR == "pro") {
            return;
        }
        if(BuildConfig.FLAVOR == "no_ads") {
            return;
        }

        final RelativeLayout layout = (RelativeLayout) findViewById(R.id.adTopLayout);
        Animation a;
        if(bShowAdvertBannerTop || override) {
            if (animationIn) {
                layout.setVisibility(View.VISIBLE);
                a = AnimationUtils.loadAnimation(mContext, R.anim.in_animation);
            } else {
                a = AnimationUtils.loadAnimation(mContext, R.anim.out_animation);
            }

            a.setFillAfter(true);
            layout.setLayoutAnimation(new LayoutAnimationController(a));
            layout.startLayoutAnimation();

            a.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    if (!animationIn)
                        layout.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationStart(Animation animation) {/*Nothing*/}

                @Override
                public void onAnimationRepeat(Animation animation) {/*Nothing*/}
            });
        } else {
            layout.setVisibility(View.GONE);
        }
    }

    /**
     * Animate the showing and hiding of adverts
     * @param override
     */
    private void animateBottomAdvert(final boolean override) {

        if(BuildConfig.FLAVOR == "pro") {
            return;
        }
        if(BuildConfig.FLAVOR == "no_ads") {
            return;
        }

        final RelativeLayout layout = (RelativeLayout) findViewById(R.id.adBotLayout);
        if(bShowAdvertBannerBot && override) {
            layout.setVisibility(View.VISIBLE);
            int mod = AppSharedPreferences.getAdvertCount(mContext, AppConstant.PREFNAME_AD_BANNER_BOT) % 2;

            Animation a= AnimationUtils.loadAnimation(mContext, (mod  == 0 ? R.anim.slide_bottom_up : R.anim.fadein));

            layout.setLayoutAnimation(new LayoutAnimationController(a));
            layout.startLayoutAnimation();
        } else {
            layout.setVisibility(View.GONE);
        }
    }

    private void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        KeyboardGone keyboardResult = new KeyboardGone(new Handler());
        imm.hideSoftInputFromWindow(mContactName.getWindowToken(), 0, keyboardResult);

        RelativeLayout myLayout = (RelativeLayout) findViewById(R.id.baseView);
        myLayout.requestFocus();
    }

    class KeyboardGone extends ResultReceiver
    {
        public KeyboardGone(Handler handler) {
            super(handler);
        }
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if(BuildConfig.FLAVOR == "free")
                animateBottomAdvert(bShowAdvertBannerBot);
        }
    }

    @Override
    public void onClick(View v) {
        hideSoftKeyboard();
        switch (v.getId()) {
            case R.id.imgBtnCall:
                try {
                    String name = mContactName.getText().toString();
                    String number = mContactNumber.getText().toString();

                    if (name.length() > 0) {
                        AppSharedPreferences.setContactInfo(mContext, AppConstant.CONTACT_NAME, name);
                        AppSharedPreferences.setContactInfo(mContext, AppConstant.CONTACT_NUMBER, number);
                    }
                    else {
                        // TODO: bug here - don't set to unknown, messes up the main screen on return from incoming call
                        AppSharedPreferences.setContactInfo(mContext, AppConstant.CONTACT_NAME,
                                getString(R.string.strUnknownCaller));
                        AppSharedPreferences.removePreference(mContext, AppConstant.CONTACT_NUMBER);
                    }

                    // Start of display Advert
                    if(BuildConfig.FLAVOR == "free") {

                        // Increment the adverts count and display adverts if exceeded
                        bShowAdvertInterstitial = AppSharedPreferences.incrementAdvertCount(mContext, AppConstant.PREFNAME_AD_INTER) >=
                                getResources().getInteger(R.integer.advertCountInter);

                        // Check the top Banner
                        bShowAdvertBannerTop = AppSharedPreferences.incrementAdvertCount(mContext, AppConstant.PREFNAME_AD_BANNER_TOP) >=
                                getResources().getInteger(R.integer.advertCountBanner);
                        if (bShowAdvertBannerTop) {
                            // Show after the call if applicable
                            final RelativeLayout layout = (RelativeLayout) findViewById(R.id.adTopLayout);
                            layout.setVisibility(View.VISIBLE);
                        }
                        // Check the bottom Banner
                        bShowAdvertBannerBot = AppSharedPreferences.incrementAdvertCount(mContext, AppConstant.PREFNAME_AD_BANNER_BOT) >=
                                getResources().getInteger(R.integer.advertCountBanner);
                        if (bShowAdvertBannerBot) {
                            // Show after the call if applicable
                            final RelativeLayout layout = (RelativeLayout) findViewById(R.id.adBotLayout);
                            layout.setVisibility(View.VISIBLE);
                        }
                    }
                    // End of display Adverts

                    if(bShowAdvertInterstitial && mInterstitialAd.isLoaded() && (BuildConfig.FLAVOR == "free")) {
                        bStart = true;
                        mInterstitialAd.show();
                    } else {
                        StartTimer timer = new StartTimer(mContext);
                        timer.incAds(false);
                        timer.setDelay(nTimeoutValue);
                        timer.start();

                        Toast.makeText(Moustache.this, "Incoming call in " + nTimeoutString, Toast.LENGTH_SHORT).show();
                        setPowerManagement(); // use wake lock to allow timer to trigger when ready
/*
                        Intent intent = new Intent( getApplicationContext(), MediaPlayerService.class );
                        intent.setAction( MediaPlayerService.ACTION_PLAY );
                        startService(intent);
*/
                        moveTaskToBack(true);
                    }
                } catch (Exception e) { /* Do nothing */}

                break;

            case R.id.ckbVibrate:
                AppSharedPreferences.setBooleanPref(mContext, AppConstant.PREFNAME_VIBRATE,
                        ((CheckBox) findViewById(R.id.ckbVibrate)).isChecked());
                break;

            case R.id.imgBtn_SelectedImage:
                // Remove the image press hint
                TextView hint = (TextView) findViewById(R.id.imageHint);
                hint.setVisibility(View.GONE);

                if(bShowAdvertInterstitial && mInterstitialAd.isLoaded() && (BuildConfig.FLAVOR == "free"))
                {
                    bSelectContact = true;
                    mInterstitialAd.show();
                } else {

                    Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                    i.putExtra(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, true);
                    startActivityForResult(i, AppConstant.PICK_CONTACT);
                }

                break;

            case R.id.imgBtnMusic:

                if (bShowAdvertInterstitial && mInterstitialAd.isLoaded() && (BuildConfig.FLAVOR == "free")) {
                    bPlayToneSelect = true;
                    mInterstitialAd.show();
                } else {

                    Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone");
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                    startActivityForResult(intent, AppConstant.PENDING_INTENT_RINGTONE);
                }
                break;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && (BuildConfig.FLAVOR == "free")) {
            if (AppSharedPreferences.getBooleanPref(mContext, AppConstant.SHOW_AD_ON_START)) {
                AppSharedPreferences.setBooleanPref(mContext, AppConstant.SHOW_AD_ON_START, false);
                mInterstitialAd.show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_settings:
                intent = new Intent(mContext, UserSettingsActivity.class);
                startActivity(intent);
                return true;

            case R.id.action_help:
                showHelpDialog();
                return true;

            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Catch the key presses
     *
     * @param event identifies the event
     * @return was the event consummed
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        // TODO: Add a catch here to grab the exit /  back button. Ask for confirmation maybe??

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            /*do nothing */
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * Catch the results of the intents sent
     *
     * @param requestCode which Intent are we processing
     * @param resultCode  result status
     * @param data        Intent information
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d(AppConstant.DEBUG, "RequestCode " + requestCode);

        if (requestCode == AppConstant.PICK_CONTACT && resultCode == RESULT_OK) {
            if (data != null) {
                Cursor cursor = getContentResolver().query(data.getData(), null, null, null, null);
                assert cursor != null;
                if (cursor.moveToFirst()) {
                    // Fetch other Contact details to use
                    setContactName(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)));
                    setContactNumber(cursor.getString(cursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.NUMBER)));
                    setContactImage(cursor.getString(cursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Photo.PHOTO_URI)));
                }
                cursor.close();
            }
        } else if (requestCode == AppConstant.PENDING_INTENT_RINGTONE && resultCode == RESULT_OK) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null)
                AppSharedPreferences.setToneUri(mContext, uri.toString());
            else
                AppSharedPreferences.removePreference(mContext, AppConstant.PREFNAME_TONE_URI);
        }
    }

    /**
     * @param strPhone contact phone number
     */
    private void setContactNumber(String strPhone) {
        if (strPhone != null) {
            mContactNumber.setVisibility(View.VISIBLE);
            mContactNumber.setText(strPhone);
            AppSharedPreferences.setContactInfo(mContext, AppConstant.CONTACT_NUMBER, strPhone);
        } else {
            mContactNumber.setHint(getString(R.string.unknown));
        }
    }

    /**
     * @param strName contact name
     */
    private void setContactName(String strName) {
        if (strName != null) {
            mContactName.setVisibility(View.VISIBLE);
            mContactName.setText(strName);
            mContactNumber.setText(mContactName.getData());
            AppSharedPreferences.setContactInfo(mContext, AppConstant.CONTACT_NAME, strName);
        } else {
            mContactName.setHint(getString(R.string.strUnknownCaller));
        }
    }

    /**
     * @param strImage string of image uri
     */
    private void setContactImage(String strImage) {
        if (strImage != null) {
            mContactmage.setVisibility(View.VISIBLE);
            mContactmage.setImageURI(Uri.parse(strImage));
            AppSharedPreferences.setContactInfo(mContext, AppConstant.CONTACT_IMAGE_URI, strImage);
        } else {
            AppSharedPreferences.setContactInfo(mContext, AppConstant.CONTACT_IMAGE_URI, null);
            mContactmage.setImageResource(R.drawable.icon_user_default_scaled);
        }
    }

    /**
     * Get new full screen advert
     */
    private void requestNewInterstitial() {
        if(BuildConfig.FLAVOR == "free") {
            AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice(getString(R.string.MyHandset))
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .build();
            mInterstitialAd.loadAd(adRequest);
        }
    }

    private void showAdvertsInformationDialog() {
        // Default is false
        if(!AppSharedPreferences.getBooleanPref(mContext, AppConstant.PREFNAME_INFO_NO_SHOW)) {
            String placeholder = getString(R.string.advert_info_dialog_text); // Here just for reference
            String htmlTitle = "<h1>" + getString(R.string.advert_info) + "</h1>";
            String htmlBody = "<p>This is the free version of Moustache and contains adverts</p>"
                    + "<p>Clicking <em>an</em> advert will disable <em>it</em> for a limtited time</p>"
                    + "<p>An advert is always displayed after using the <em>Stealth Mode</em> but will not "
                    + "be shown until after the call is finished.</p>";

            AlertDialog.Builder alertDialog
                    = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
            alertDialog.setTitle(Html.fromHtml(htmlTitle));
            alertDialog.setMessage(Html.fromHtml(htmlBody));
            alertDialog.setPositiveButton(R.string.dialog_btn_dismiss, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // Do not show this again
                    AppSharedPreferences.setBooleanPref(mContext, AppConstant.PREFNAME_INFO_NO_SHOW, true);
                }
            });
            alertDialog.setIcon(R.drawable.ic_mood_bad_white_24dp);
            alertDialog.show();
        }
    }

    /**
     * Show stealth mode help screen
     */
    private void showHelpDialog() {
        String placeholder = getString(R.string.help_info_dialog_text); // Here just for reference
        String htmlTitle = "<h1>" + getString(R.string.help_info) + "</h1>";
        String htmlBody = "<h2>Stealth Mode</h2>"
                + "<br>• From screen off (blank screen), turn the screen on, off and on again</ br>"
                + "<br>• Now press the volume button to start timer in stealth mode</ br>"
                + "<br>• Timeout value is set in Normal Mode</ br>"
                + "<br>• <em>Requires an activated screen lock</em></ br>"
                + "<h2>Normal Mode</h2>"
                + "<br>• Either:</br>"
                + "<br>&emsp;- Click the image to select the contact</br>"
                + "<br>&emsp;- Type the contact and select from the drop down list (image not available yet)</br>"
                + "<br>• Choose your ring tone using the music library button</br>"
                + "<br>• Choose a how long to wait for the incoming call</br>"
                + "<br>• Press the phone icon to start</br>";

        if(BuildConfig.FLAVOR == "free") {
            animateTopAdvert(false, true);
            animateBottomAdvert(!bShowAdvertBannerBot);
        }
        AlertDialog.Builder alertDialog
                = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        alertDialog.setTitle(Html.fromHtml(htmlTitle));
        alertDialog.setMessage(Html.fromHtml(htmlBody));
        alertDialog.setPositiveButton(R.string.dialog_btn_dismiss, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                animateTopAdvert(true, false);
                animateBottomAdvert(bShowAdvertBannerBot);
            }
        });
        alertDialog.setIcon(R.drawable.ic_help_outline_white_24dp);
        alertDialog.show();
    }

    /**
     * Set wake lock - don't like it but don't really understand it much either
     */
    private void setPowerManagement() {
        AlarmWakeLock.createPartialWakeLock(mContext).acquire();
    }

    /**
     * Register the broadcast receiver
     */
    private void registerStealthAction() {
        if(!AppSharedPreferences.getBooleanPref(mContext, AppConstant.PREFNAME_NO_STEALTH)) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            PowerSendMessage stealthReceiver = new PowerSendMessage();
            registerReceiver(stealthReceiver, filter);
            stealthReceiver.resetCode();
        }
    }

    /**
     * Show msg
     *
     * @param message message to display
     */
    private void showSnackBar(String message) {
        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        Snackbar snackbar = Snackbar
                .make(coordinatorLayout, message, Snackbar.LENGTH_LONG)
                .setDuration(Snackbar.LENGTH_LONG)
                .setAction("SEEN", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                            /*Do nothing*/
                    }
                });
        snackbar.show();
    }
}