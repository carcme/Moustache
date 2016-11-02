package me.carc.moustache;

/**
 * Created by Carc.me on 16.01.16.
 * <p/>
 * Define the application constants
 */
class AppConstant {

    public static final String DEBUG ="DEBUG";

    public static final int PICK_CONTACT = 2000;
    public static final int MOUSTACHE_INTENT_ID = 3000;
    public static final int BROADCAST_INTENT_ID = 4000;
    public static final int PENDING_INTENT_RINGTONE = 6000;
    public static final int INTENT_RATE = 8000;
    public static final int INTENT_UPDATE = 9000;



    public static final long[] VIBRATE_PATTERN = {0, 700, 1000};

    public static final String PREF_FILE_NAME = "me.carc.moustache";

    public static final String PREFNAME_ELAPSED_TIMEOUT = "ELASPED_TIMEOUT";
    public static final String PREFNAME_SPINNER_POS     = "SPINNER_POS";
    public static final String PREFNAME_TONE_URI        = "TONE_URI";
    public static final String PREFNAME_VIBRATE         = "VIBRATE";
    public static final String PREFNAME_HELP            = "HELP";
    public static final String PREFNAME_LOG_CALL        = "LOG_CALL";
    public static final String PREFNAME_NO_STEALTH      = "DISCRETE_MODE";

    public static final String PREFNAME_AD_INTER        = "ADVERT_INTER";
    public static final String PREFNAME_AD_BANNER_TOP   = "ADVERT_TOP";
    public static final String PREFNAME_AD_BANNER_BOT   = "ADVERT_BOTTOM";
    public static final String PREFNAME_INFO_NO_SHOW    = "ADVERT_DIALOG";

    public static final String CONTACT_NAME             = "NAME";
    public static final String CONTACT_NUMBER           = "NUMBER";
    public static final String CONTACT_IMAGE_URI        = "IMAGE";

    public static final String SHOW_AD_ON_START         = "SHOW_ADVERT";

    public static final int CALL_REJECT = 0;
    public static final int CALL_ACCEPT = 1;
    public static final int CALL_END = 2;
}