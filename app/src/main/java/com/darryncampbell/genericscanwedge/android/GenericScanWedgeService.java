package com.darryncampbell.genericscanwedge.android;

import android.app.IntentService;
import android.app.Notification;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;

//  This service handles DataWedge API intents from the broadcast receiver
public class GenericScanWedgeService extends IntentService {

    private static final int NOTIFICATION_ID = 555;

    static final String LOG_TAG = "Generic Scan Wedge";
    //  Parameters associated with the application actions
    private static final int NO_ACTIVE_PROFILE = -1;

    public GenericScanWedgeService() {
        super("GenericScanWedgeService");
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        Notification.Builder builder = new Notification.Builder(this, App.ANDROID_CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("SmartTracker Running")
                .setAutoCancel(true);
        Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    /*
    @see https://techdocs.zebra.com/datawedge/11-0/guide/programmers-guides/dw-programming/
    */
    protected void onHandleIntent(Intent intent) {
        Log.d(LOG_TAG, "onHandleIntent");
        if (intent == null || !Datawedge.ACTION.equals(intent.getAction()))
            return;

        final ArrayList<Profile> profiles = (ArrayList<Profile>) intent.getSerializableExtra("profiles");
        final int activeProfilePosition = intent.getIntExtra("activeProfilePosition", NO_ACTIVE_PROFILE);
        Profile activeProfile = null;
        if (activeProfilePosition != NO_ACTIVE_PROFILE)
            activeProfile = profiles.get(activeProfilePosition);


        //http://techdocs.zebra.com/datawedge/latest/guide/api/getversioninfo/
        if (intent.hasExtra(Datawedge.GET_VERSION_INFO)) {
            Bundle b = new Bundle();
            b.putString("DATAWEDGE", "darryncampbell.genericscanwedge");
            sendResultWithExtra(Datawedge.RESULT_GET_VERSION_INFO, b);
        }

        //http://techdocs.zebra.com/datawedge/latest/guide/api/getprofileslist/
        if (intent.hasExtra(Datawedge.GET_PROFILES_LIST)) {
            String[] names = profiles.stream().map(Profile::getName).toArray(String[]::new);
            sendResultWithExtra(Datawedge.RESULT_GET_PROFILES_LIST, names);
        }

        //http://techdocs.zebra.com/datawedge/latest/guide/api/createprofile/
        if (intent.hasExtra(Datawedge.CREATE_PROFILE)) {
            final String name = intent.getStringExtra(Datawedge.CREATE_PROFILE);
            if(!profiles.stream().anyMatch(profile -> { return profile.getName().equals(name); })) {
                Profile profile = new Profile(name, true);
                profile.setScanningEngine(Profile.ScanningEngine.SCANNING_ENGINE_BLUETOOTH_SPP);
                profiles.add(profile);
                if (activeProfile != null) {
                    activeProfile.setProfileEnabled(false);
                }
                MainActivity.saveProfiles(profiles, getApplicationContext());
                sendCommandResult(Datawedge.CREATE_PROFILE, "SUCCESS", Datawedge.PROFILE_NAME, name);
            }
            else
                sendCommandResult(Datawedge.CREATE_PROFILE, "FAILURE", Datawedge.PROFILE_NAME, name);
        }

        //http://techdocs.zebra.com/datawedge/latest/guide/api/setconfig/
        if (intent.hasExtra(Datawedge.SET_CONFIG)) {
            handleSetConfig(intent.getBundleExtra(Datawedge.SET_CONFIG), profiles);
            Bundle b = new Bundle();
            b.putString("NOTIFICATION_TYPE", "CONFIGURATION_UPDATE");
            sendNotification(b);
        }

        //http://techdocs.zebra.com/datawedge/latest/guide/api/getactiveprofile/
        if (intent.hasExtra(Datawedge.GET_ACTIVE_PROFILE)) {
            sendResultWithExtra(Datawedge.RESULT_GET_ACTIVE_PROFILE, activeProfile == null ? "" : activeProfile.getName());
        }

        //https://techdocs.zebra.com/datawedge/latest/guide/api/switchtoprofile/
        if (intent.hasExtra(Datawedge.SWITCH_TO_PROFILE)) {
            final String name = intent.getStringExtra(Datawedge.SWITCH_TO_PROFILE);
            if (handleSwitchToProfile(name, profiles, activeProfilePosition)) {
                Bundle b = new Bundle();
                b.putString("NOTIFICATION_TYPE", "PROFILE_SWITCH");
                b.putString("PROFILE_NAME", name);
                b.putBoolean("PROFILE_ENABLED", true);
                sendNotification(b);
            }
        }

        if (intent.hasExtra(Datawedge.SOFT_SCAN_TRIGGER)) {
            if (activeProfilePosition == NO_ACTIVE_PROFILE) {
                Log.e(LOG_TAG, "No Active profile currently defined and enabled.  No barcode will be scanned");
            } else {
                handleActionSoftScanTrigger(intent.getStringExtra(Datawedge.SOFT_SCAN_TRIGGER), activeProfile);
            }
        }

        if (intent.hasExtra(Datawedge.SCANNER_INPUT_PLUGIN)) {
            if (activeProfilePosition == NO_ACTIVE_PROFILE) {
                Log.e(LOG_TAG, "No Active profile currently defined and enabled.  No barcode will be enabled");
            } else {
                handleScannerInputPlugin(intent.getStringExtra(Datawedge.SCANNER_INPUT_PLUGIN), profiles, activeProfilePosition);
            }
        }
        if (intent.hasExtra(Datawedge.ENUMERATE_SCANNERS)) {
            handleEnumerateScanners();
        }
    }

    private void sendResultWithExtra(String name, Bundle b) {
        sendIntent(new Intent(Datawedge.RESULT_ACTION).putExtra(name, b));
    }

    private void sendNotification(Bundle b) {
        sendIntent(new Intent(Datawedge.NOTIFICATION_ACTION).putExtra(Datawedge.NOTIFICATION, b));
    }

    private void sendCommandResult(String command, String result, String extraName, String extraValue) {
        sendIntent(new Intent(Datawedge.RESULT_ACTION)
                        .putExtra("COMMAND", command)
                        .putExtra("RESULT", result)
                        .putExtra(extraName, extraValue));
    }

    private void sendResultWithExtra(String extraName, String[] extraValue) {
        sendIntent(new Intent(Datawedge.RESULT_ACTION).putExtra(extraName, extraValue));
    }

    private void sendResultWithExtra(String extraName, String extraValue) {
        sendIntent(new Intent(Datawedge.RESULT_ACTION).putExtra(extraName, extraValue));
    }

    private void sendIntent(Intent barcodeIntent) {
        sendBroadcast(barcodeIntent.addCategory(Intent.CATEGORY_DEFAULT));
    }

    //  Work around for sending to a service.  Could probably be more intelligent here as just
    //  copied from stack overflow
    private static Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }

        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }

    private void sendMainActivityFinishIntent() {
        //  Bit of a hack but want to return to the original calling application
        Intent finishIntent = new Intent(this, MainActivity.class);
        finishIntent.putExtra("finish", true);
        finishIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(finishIntent);
    }

    //  Calling application is asking to initiate or stop a scan in progress
    //@see https://techdocs.zebra.com/datawedge/11-0/guide/api/softscantrigger/
    private void handleActionSoftScanTrigger(String param, Profile activeProfile) {
        if (param.equals(Datawedge.START_SCANNING)) {
            if (!activeProfile.isBarcodeInputEnabled()) {
                Log.d(LOG_TAG, "Barcode scanning is disabled in the current profile");
                return;
            }

            //  If any more scan engines are supported they need adding here.
            if (activeProfile.getScanningEngine() == Profile.ScanningEngine.SCANNING_ENGINE_ZXING) {
                Intent zxingActivity = new Intent(this, ZXingActivity.class);
                zxingActivity.putExtra("activeProfile", activeProfile);
                startActivity(zxingActivity);
            } else if (activeProfile.getScanningEngine() == Profile.ScanningEngine.SCANNING_ENGINE_GOOGLE_VISION) {
                Intent googleVisionActivity = new Intent(this, GoogleVisionBarcodeActivity.class);
                googleVisionActivity.putExtra("activeProfile", activeProfile);
                startActivity(googleVisionActivity);
            } else if (activeProfile.getScanningEngine() == Profile.ScanningEngine.SCANNING_ENGINE_BLUETOOTH_SPP) {
                //  Start Scanning has no effect on a bluetooth connected scanner, expectation is that
                //  scanner has a hardware trigger and the scanner itself does not support soft scans
                //  through the BT interface
                Log.w(LOG_TAG, "START_SCANNING is not implemented for BT scanners connected over SPP");
            }
        } else if (param.equals(Datawedge.STOP_SCANNING)) {
            //  Stop scanning does not make sense for ZXing or Google Vision API
            Log.w(LOG_TAG, "STOP_SCANNING is not implemented for Non Zebra devices");
        } else if (param.equals(Datawedge.TOGGLE_SCANNING)) {
            //  Toggle scanning does not make sense for ZXing or Google Vision API
            Log.w(LOG_TAG, "TOGGLE_SCANNING is not implemented for Non Zebra devices");
        } else {
            //  Unrecognised parameter
            Log.w(LOG_TAG, "Unrecognised parameter to SoftScanTrigger: " + param);
        }
    }

    //  Calling application is asking to enable or disable scanning in the active profile
    //@see https://techdocs.zebra.com/datawedge/11-0/guide/api/scannerinputplugin/
    private void handleScannerInputPlugin(String param, ArrayList<Profile> profiles, int activeProfilePosition) {
        if (param.equals(Datawedge.ENABLE_PLUGIN)) {
            profiles.get(activeProfilePosition).setBarcodeInputEnabled(true);
        } else if (param.equals(Datawedge.DISABLE_PLUGIN)) {
            profiles.get(activeProfilePosition).setBarcodeInputEnabled(false);
        } else {
            //  Unrecognised parameter
            Log.w(LOG_TAG, "Unrecognised parameter to ScannerInputPlugin: " + param);
        }
        MainActivity.saveProfiles(profiles, getApplicationContext());
    }

    /*
      Calling application is asking to enumerate the available scanners.  For ZXing or Google Barcode API we just return the camera
     @see https://techdocs.zebra.com/datawedge/11-0/guide/api/enumeratescanners/
    */
    private void handleEnumerateScanners() {
        Intent enumerateBarcodesIntent = new Intent();
        enumerateBarcodesIntent.setAction(Datawedge.RESULT_ACTION);

        ArrayList<Bundle> bundles = new ArrayList<>();
        Bundle cam = new Bundle();
        cam.putString(Datawedge.SCANNER_NAME, "Camera Scanner");
        cam.putInt(Datawedge.SCANNER_INDEX, 0);
        cam.putBoolean(Datawedge.SCANNER_CONNECTION_STATE, true);
        cam.putString(Datawedge.SCANNER_IDENTIFIER, "Camera Scanner");
        bundles.add(cam);
        Bundle bt = new Bundle();
        bt.putString(Datawedge.SCANNER_NAME, "Bluetooth Scanner (SPP)");
        bt.putInt(Datawedge.SCANNER_INDEX, 1);
        bt.putBoolean(Datawedge.SCANNER_CONNECTION_STATE, true);
        bt.putString(Datawedge.SCANNER_IDENTIFIER, "Camera Scanner");
        bundles.add(bt);
        enumerateBarcodesIntent.putExtra(Datawedge.RESULT_ENUMERATE_SCANNERS, bundles);
        sendBroadcast(enumerateBarcodesIntent);
    }

    //  Calling application has requested to switch to a specific profile
    private boolean handleSwitchToProfile(String param, ArrayList<Profile> profiles, int activeProfileIndex) {
        Log.d(LOG_TAG, "Switching to profile: " + param);
        //  Change the enabled profile to the specified profile name
        boolean bFoundProfile = false;
        int enabledProfileIndex = -1;
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getName().equals(param)) {
                if (activeProfileIndex != NO_ACTIVE_PROFILE)
                    profiles.get(activeProfileIndex).setProfileEnabled(false);
                profiles.get(i).setProfileEnabled(true);
                bFoundProfile = true;
                enabledProfileIndex = i;
            }
        }
        if (bFoundProfile) {
            MainActivity.saveProfiles(profiles, getApplicationContext());
            //  If we are switching to a profile which uses the Serial Port Profile then try to connect
            //  to the scanner if it was previously connected
            if (profiles.get(enabledProfileIndex).getScanningEngine() == Profile.ScanningEngine.SCANNING_ENGINE_BLUETOOTH_SPP) {
                String address = ProfileConfiguration.lastConnectedMacAddress;
                if (address != null) {
                    Intent bluetoothConnectionConnectIntent = new Intent(this, BluetoothConnectionService.class);
                    bluetoothConnectionConnectIntent.setAction(BluetoothConnectionService.ACTION_CONNECT);
                    bluetoothConnectionConnectIntent.putExtra("macAddress", address);
                    bluetoothConnectionConnectIntent.putExtra("activeProfile", profiles.get(enabledProfileIndex));
                    startService(bluetoothConnectionConnectIntent);
                }
            }
            return true;
        } else {
            Log.w(LOG_TAG, "Unrecognised profile to switch to: " + param);
        }
        return false;
    }

    //https://techdocs.zebra.com/datawedge/11-0/guide/api/setconfig/
    private void handleSetConfig(Bundle mainBundle, ArrayList<Profile> profiles) {
        final String name = mainBundle.getString(Datawedge.PROFILE_NAME);
        if (name == null) {
            Log.e(LOG_TAG, "Set config with empty name");
            return;
        }

        final String mode = mainBundle.getString(Datawedge.CONFIG_MODE);
        Profile current = profiles.stream().filter(new Predicate<Profile>() {
            @Override
            public boolean test(Profile profile) {
                return name.equals(profile.getName());
            }
        }).findAny().orElse(null);
        switch (mode) {
            case Datawedge.UPDATE:
            case Datawedge.OVERWRITE:
                if (current == null) {
                    Log.e(LOG_TAG, "Update or overwrite missing profile");
                    return;
                }
                break;
            case Datawedge.CREATE_IF_NOT_EXIST:
                if (current == null) {
                    current = new Profile(name, mainBundle.getBoolean(Datawedge.PROFILE_ENABLED, true));
                    profiles.add(current);
                }
                break;
        }

        if (mainBundle.containsKey(Datawedge.PROFILE_ENABLED))
            current.setProfileEnabled(value(mainBundle.getString(Datawedge.PROFILE_ENABLED)));


        try {
            Parcelable[] configs = mainBundle.getParcelableArray(Datawedge.PLUGIN_CONFIG);
            for (Parcelable plugin : configs) {
                handleConfigChange((Bundle) plugin, current);
            }
        } catch (Exception ex) {
            Log.e(LOG_TAG, "getParcelableArray on configs is not working", ex);
            try {
                handleConfigChange(mainBundle.getBundle(Datawedge.PLUGIN_CONFIG), current);
            } catch (Exception ex2) {
                Log.e(LOG_TAG, "getBundle on configs is not working", ex2);
            }
        }
        MainActivity.saveProfiles(profiles, getApplicationContext());
    }

    private void handleConfigChange(Bundle plugin, Profile current) {
        final String name = plugin.getString(Datawedge.PLUGIN_NAME);
        if (Datawedge.PLUGIN_INTENT.equals(name)) {
            final Bundle params = plugin.getBundle(Datawedge.PARAM_LIST);
            if (params == null)
                return;
            if (params.containsKey(Datawedge.intent_action))
                current.setIntentAction(params.getString(Datawedge.intent_action));
            if (params.containsKey(Datawedge.intent_delivery))
                current.setIntentDelivery(Profile.IntentDelivery.values()[params.getInt(Datawedge.intent_delivery)]);
//            if(plugin.containsKey(Datawedge.intent_output_enabled))
//                current.(plugin.getString(Datawedge.intent_action));
        } else if (Datawedge.PLUGIN_BARCODE.equals(name)) {
            final Bundle params = plugin.getBundle(Datawedge.PARAM_LIST);
            if (params == null)
                return;
        }
    }

    private static String value(Boolean value) {
        if (value)
            return "true";
        return "false";
    }

    private static Boolean value(String value) {
        return "true".equals(value);
    }
}
