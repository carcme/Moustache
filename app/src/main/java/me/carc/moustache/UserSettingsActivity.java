package me.carc.moustache;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * Created by Carc.me on 13.02.16.
 * <p/>
 * TODO: Add a class header comment!
 */
public class UserSettingsActivity extends PreferenceActivity {

    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getApplicationContext();
        addPreferencesFromResource(R.xml.settings);

        // Logging Selected
        final CheckBoxPreference callLogPref = (CheckBoxPreference)findPreference("log");
        final CheckBoxPreference stealthmode = (CheckBoxPreference)findPreference("stealthmode");

        final Preference resetPref = findPreference("reset");
        final Preference sharingPref = findPreference("share");
        final Preference ratePref = findPreference("rate");
        final Preference feedbackPref = findPreference("feedback");
        final Preference update = findPreference("update");

        if(BuildConfig.FLAVOR == "pro" || BuildConfig.FLAVOR == "no_ads") {
            PreferenceScreen screen = getPreferenceScreen();
            PreferenceCategory upgradeCat = (PreferenceCategory)findPreference("upgradeCategory");
            screen.removePreference(upgradeCat);
        } else {
            final Preference upgradePref = findPreference("upgrade");

            // Rate selected
            upgradePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Uri uriStore = Uri.parse("market://details?id=" + getPackageName() + ".pro");
                    Intent goToMarket = new Intent(Intent.ACTION_VIEW, uriStore);
                    startActivityForResult(goToMarket, AppConstant.INTENT_UPDATE);
                    return true;
                }
            });
        }

        // Enable logging of call in device record
        callLogPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                if (callLogPref.isChecked())
                    AppSharedPreferences.setBooleanPref(mContext, AppConstant.PREFNAME_LOG_CALL, true);
                else
                    AppSharedPreferences.removePreference(mContext, AppConstant.PREFNAME_LOG_CALL);
                return true;
            }
        });

        // Stealth mode enable/disable
        stealthmode.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                if (stealthmode.isChecked()) {
                    AppSharedPreferences.setBooleanPref(mContext, AppConstant.PREFNAME_NO_STEALTH, true);
                } else {
                    AppSharedPreferences.removePreference(mContext, AppConstant.PREFNAME_NO_STEALTH);
                }
                return true;
            }
        });

        //Clear all saved preferences
        resetPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                AppSharedPreferences.removePreference(mContext, AppConstant.PREFNAME_ELAPSED_TIMEOUT);
                AppSharedPreferences.removePreference(mContext, AppConstant.PREFNAME_SPINNER_POS);
                AppSharedPreferences.removePreference(mContext, AppConstant.PREFNAME_VIBRATE);
                AppSharedPreferences.removePreference(mContext, AppConstant.CONTACT_NAME);
                AppSharedPreferences.removePreference(mContext, AppConstant.CONTACT_NUMBER);
                AppSharedPreferences.removePreference(mContext, AppConstant.CONTACT_IMAGE_URI);
                AppSharedPreferences.removePreference(mContext, AppConstant.PREFNAME_TONE_URI);
                AppSharedPreferences.removePreference(mContext, AppConstant.PREFNAME_HELP);
                AppSharedPreferences.removePreference(mContext, AppConstant.PREFNAME_LOG_CALL);
                callLogPref.setChecked(false);

                startActivity(new Intent(mContext, Moustache.class)); // restart the app
                return true;
            }
        });

        //Send feedback via email
        feedbackPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent Email = new Intent(Intent.ACTION_SEND);
                Email.setType("text/email");
                Email.putExtra(Intent.EXTRA_EMAIL, new String[]{"moustache@carc.me"});
                Email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedbackSubject));
                Email.putExtra(Intent.EXTRA_TEXT, getString(R.string.feedbackBody));
                startActivity(Intent.createChooser(Email, getString(R.string.feedbackChooserTitle)));
                return true;
            }
        });

        // Share Selected
        sharingPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");

                String shareBody = getString(R.string.strShareBody)
                        + getString(R.string.strShareDownloadMsg)
                        + getString(R.string.strShareDownloadLink) + getPackageName();

                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.strShareSubject));
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, getString(R.string.shareChooserTitle)));
                return true;
            }
        });

        // Rate selected
        ratePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Uri uriStore = Uri.parse("market://details?id=" + getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uriStore);
                startActivityForResult(goToMarket, AppConstant.INTENT_RATE);
                return true;
            }
        });

/*
        // Update selected
        update.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                new UpdateRunnable(mContext, new Handler()).force(true).start();
                return true;
            }
        });
*/

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d(AppConstant.DEBUG, "RequestCode " + requestCode);

        if(requestCode == AppConstant.INTENT_RATE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, R.string.strRateResultPositive, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.strRateResultNegative, Toast.LENGTH_LONG).show();
            }
        } else if(requestCode == AppConstant.INTENT_UPDATE) {
            if (resultCode == RESULT_OK)
                showHelpDialog();
        }
    }

    private void showHelpDialog() {
        String placeholder = getString(R.string.help_info_dialog_text); // Here just for reference
        String htmlTitle = "<h1>" + getString(R.string.remove_free_title) + "</h1>";
        String htmlBody = "<br>Remove the free version from your device?</ br>"
                + "<br>Please rate it while you're there</ br>"
                + "<br>Thank you</ br>";

        AlertDialog.Builder alertDialog
                = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        alertDialog.setTitle(Html.fromHtml(htmlTitle));
        alertDialog.setMessage(Html.fromHtml(htmlBody));
        alertDialog.setPositiveButton(R.string.dialog_btn_sure, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Uri uriStore = Uri.parse("market://details?id=" + getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uriStore);
                startActivity(goToMarket);
            }
        });
        alertDialog.setNegativeButton(R.string.dialog_btn_dismiss, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {/*Do Nothing*/
            }
        });
        alertDialog.setIcon(R.drawable.ic_help_outline_white_24dp);
        alertDialog.show();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        LinearLayout root = (LinearLayout)findViewById(android.R.id.list).getParent().getParent().getParent();

        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
        root.addView(bar, 0); // insert at top
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}