package com.darryncampbell.genericscanwedge.android;

public final class Datawedge {
    public static final String ACTION = "com.symbol.datawedge.api.ACTION";
    public static final String RESULT_ACTION = "com.symbol.datawedge.api.RESULT_ACTION";
    public static final String NOTIFICATION_ACTION = "com.symbol.datawedge.api.NOTIFICATION_ACTION";

    public static final String SOFT_SCAN_TRIGGER = "com.symbol.datawedge.api.SOFT_SCAN_TRIGGER";
    public static final String START_SCANNING = "START_SCANNING";
    public static final String STOP_SCANNING = "STOP_SCANNING";
    public static final String TOGGLE_SCANNING = "TOGGLE_SCANNING";

    public static final String SCANNER_INPUT_PLUGIN = "com.symbol.datawedge.api.SCANNER_INPUT_PLUGIN";
    public static final String SUSPEND_PLUGIN = "SUSPEND_PLUGIN";
    public static final String RESUME_PLUGIN = "RESUME_PLUGIN";
    public static final String ENABLE_PLUGIN = "ENABLE_PLUGIN";
    public static final String DISABLE_PLUGIN = "DISABLE_PLUGIN";

    public static final String ENUMERATE_SCANNERS = "com.symbol.datawedge.api.ENUMERATE_SCANNERS";
    public static final String RESULT_ENUMERATE_SCANNERS = "com.symbol.datawedge.api.RESULT_ENUMERATE_SCANNERS";
    public static final String SCANNER_NAME = "SCANNER_NAME";
    public static final String SCANNER_INDEX = "SCANNER_INDEX";
    public static final String SCANNER_CONNECTION_STATE = "SCANNER_CONNECTION_STATE";
    public static final String SCANNER_IDENTIFIER = "SCANNER_IDENTIFIER";
    //---------------------------------------------------------------------------
    public static final String SWITCH_TO_PROFILE = "com.symbol.datawedge.api.SWITCH_TO_PROFILE";
    //---------------------------------------------------------------------------
    //sent with bundle profileConfig consisting of [PROFILE_NAME, PROFILE_ENABLED, CONFIG_MODE, PLUGIN_CONFIG, APP_LIST]
    public static final String SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG";
    /*  */public static final String PROFILE_NAME = "PROFILE_NAME";
    /*  */public static final String PROFILE_ENABLED = "PROFILE_ENABLED"; //true    public static final String PROFILE_NAME = "PROFILE_NAME";
    /*  */public static final String CONFIG_MODE = "CONFIG_MODE"; //one CONFIG_MODE value is UPDATE, CREATE_IF_NOT_EXIST, OVERWRITE
    /*  *//*  */public static final String UPDATE = "UPDATE";
    /*  *//*  */public static final String CREATE_IF_NOT_EXIST = "CREATE_IF_NOT_EXIST";
    /*  *//*  */public static final String OVERWRITE = "OVERWRITE";
    /*  *///sent with bundle of bundles [PLUGIN_NAME, RESET_CONFIG, PARAM_LIST]
    /*  */public static final String PLUGIN_CONFIG = "PLUGIN_CONFIG";
    /*  *//*  */public static final String PLUGIN_NAME = "PLUGIN_NAME"; //one PLUGIN_NAME value is BARCODE
    /*  *//*  *//*  */public static final String PLUGIN_BARCODE = "BARCODE";
    /*  *//*  *//*  */public static final String PLUGIN_INTENT = "INTENT";
    /*  *//*  */public static final String RESET_CONFIG = "RESET_CONFIG"; //true
    //sent with bundle of [intent_output_enabled, intent_action, intent_delivery]
    /*  *//*  */public static final String PARAM_LIST = "PARAM_LIST"; // for barcode plugin = [] for
    /*  *//*  */public static final String intent_output_enabled = "intent_output_enabled"; //for intent = true
    /*  *//*  */public static final String intent_action = "intent_action"; //for intent = the name in filter
    /*  *//*  */public static final String intent_delivery = "intent_delivery"; //for intent = 2
    //sent with bundle of bundles. one sample is [[PACKAGE_NAME, ACTIVITY_LIST[]]]
    /*  */public static final String APP_LIST = "APP_LIST";
    /*  *//*  */public static final String PACKAGE_NAME = "PACKAGE_NAME";//getPackageName()
    /*  *//*  */public static final String ACTIVITY_LIST = "ACTIVITY_LIST"; //one ACTIVITY_LIST value is WILDCARD
    /*  *//*  *//*  */public static final String WILDCARD = "*"; //
    //---------------------------------------------------------------------------
    public static final String GET_PROFILES_LIST = "com.symbol.datawedge.api.GET_PROFILES_LIST";
    public static final String RESULT_GET_PROFILES_LIST = "com.symbol.datawedge.api.RESULT_GET_PROFILES_LIST";
}

