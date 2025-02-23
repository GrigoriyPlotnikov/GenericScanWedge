package com.darryncampbell.genericscanwedge.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by darry on 12/08/2016.
 */
public class GenericScanWedgeIntentReceiver extends BroadcastReceiver {

    static final String LOG_TAG = "Generic Scan Wedge";

    //  This function is called when the Datawedge Intent API is called e.g. StartSoftScan.
    @Override
    public void onReceive(Context context, Intent intent) {
        //  Just forward any intent we receive to the intent service unless we're running
        //  on a Zebra device
        if (android.os.Build.MANUFACTURER.equalsIgnoreCase("Zebra Technologies") ||
                Build.MANUFACTURER.equalsIgnoreCase("Motorola Solutions"))
            return;

        if (intent.hasCategory(Intent.CATEGORY_DEFAULT))
            return;

        try {
            //  Read the configured profiles and launch the GenericScanWedgeService to handle
            //  whatever it is the caller wants to do
            ArrayList<Profile> profiles = MainActivity.readProfiles(context);
            if (profiles != null) {
                Profile activeProfile = null;
                int activeProfilePosition = -1;
                for (int i = 0; i < profiles.size(); i++) {
                    if (profiles.get(i).getProfileEnabled()) {
                        activeProfile = profiles.get(i);
                        activeProfilePosition = i;
                        break;
                    }
                }
                //  We have successfully read in the configured profiles, find the active one
                if (activeProfile != null)
                    Log.d(LOG_TAG, "Active Profile: " + activeProfile.getName());
                Intent newIntent = new Intent(context, GenericScanWedgeService.class);
                newIntent.setAction(intent.getAction());
                if (intent.getExtras() != null)
                    newIntent.putExtras(intent.getExtras());
                newIntent.putExtra("activeProfilePosition", activeProfilePosition);
                newIntent.putExtra("profiles", profiles);
                //  Start GenericScanWedgeService
                context.startForegroundService(newIntent);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Unable to read DataWedge profile, please configure an active profile");
        } catch (ClassNotFoundException e) {
            Log.e(LOG_TAG, "Unable to read DataWedge profile, please configure an active profile");
        }

    }
}
