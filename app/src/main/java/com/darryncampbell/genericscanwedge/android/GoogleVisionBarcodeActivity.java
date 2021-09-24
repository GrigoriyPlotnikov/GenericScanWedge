package com.darryncampbell.genericscanwedge.android;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.darryncampbell.genericscanwedge.android.googleVisionBarcode.BarcodeCaptureActivity;

import java.util.List;

//  This class enables the Google Barcode API to be called from the DataWedge lite service.  I have
//  just copy / pasted the Google Vision example for Barcode which seems a bit buggy on my phone but
//  gets the job done.  All the Google Barcode specific stuff is stored under /googleVisionBarcode
//  and only differs from the Google sample by me changing the package name and adding the requested
//  decoder formats.
//  https://developers.google.com/android/reference/com/google/android/gms/vision/barcode/Barcode
public class GoogleVisionBarcodeActivity extends AppCompatActivity {

    private Profile activeProfile;
    private static final int RC_BARCODE_CAPTURE = 9001;
    static final String LOG_TAG = "Generic Scan Wedge";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null)
            return;

        setContentView(R.layout.activity_hiddenactivity);

        activeProfile = (Profile) getIntent().getSerializableExtra("activeProfile");
        Intent intent = new Intent(this, BarcodeCaptureActivity.class);
        intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
        intent.putExtra(BarcodeCaptureActivity.UseFlash, true);
        //  Added on top of the Google Barcode sample, supported decoders are flags.
        intent.putExtra("formats", giveSupportedDecoders());
        startActivityForResult(intent, RC_BARCODE_CAPTURE);

    }

    //  A barcode has been read from the Google Barcode API, notify the calling application
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    Log.d(LOG_TAG, "Barcode read from Google Vision Barcode API: " + barcode.displayValue);

                    //  https://developers.google.com/android/reference/com/google/android/gms/vision/barcode/Barcode

                    Intent barcodeIntent = new Intent(this.activeProfile.getIntentAction());
                    //  Same format as defined by Datawedge including backwards compatibility with the old MSI devices.
                    barcodeIntent.putExtra("com.symbol.datawedge.source", "camera-google-vision");
                    barcodeIntent.putExtra("com.symbol.datawedge.label_type", decoderFormatToString(barcode.format));
                    barcodeIntent.putExtra("com.symbol.datawedge.data_string", barcode.displayValue);
                    barcodeIntent.putExtra("com.symbol.datawedge.decode_data", barcode.rawValue.getBytes());
                    barcodeIntent.putExtra("com.motorolasolutions.emdk.datawedge.source", "scanner-google-vision");
                    barcodeIntent.putExtra("com.motorolasolutions.emdk.datawedge.label_type", decoderFormatToString(barcode.format));
                    barcodeIntent.putExtra("com.motorolasolutions.emdk.datawedge.data_string", barcode.displayValue);
                    barcodeIntent.putExtra("com.motorolasolutions.emdk.datawedge.decode_data", barcode.rawValue.getBytes());
                    if (!this.activeProfile.getIntentCategory().equalsIgnoreCase(""))
                        barcodeIntent.addCategory(this.activeProfile.getIntentCategory());
                    if (this.activeProfile.getIntentDelivery() == Profile.IntentDelivery.INTENT_DELIVERY_START_ACTIVITY) {
                        try {
                            barcodeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(barcodeIntent);
                            sendMainActivityFinishIntent();
                        } catch (ActivityNotFoundException e) {
                            Log.w(LOG_TAG, "No Activity found to handle barcode.  Current profile action is " + this.activeProfile.getIntentAction());
                        }
                    }
                    else if (this.activeProfile.getIntentDelivery() == Profile.IntentDelivery.INTENT_DELIVERY_START_SERVICE)
                    {
                        try {
                            startService(createExplicitFromImplicitIntent(getApplicationContext(), barcodeIntent));
                            sendMainActivityFinishIntent();
                        }
                        catch (Exception e)
                        {
                            Log.w(LOG_TAG, "No Service found to handle barcode.  Current profile action is " + this.activeProfile.getIntentAction());
                        }
                    }
                    else if (this.activeProfile.getIntentDelivery() == Profile.IntentDelivery.INTENT_DELIVERY_BROADCAST_INTENT)
                    {
                        if (this.activeProfile.getReceiverForegroundFlag())
                            barcodeIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                        sendBroadcast(barcodeIntent);
                        sendMainActivityFinishIntent();
                    }

                } else {
                    Log.d(LOG_TAG, "No barcode captured from Google Vision Barcode, intent data is null");
                    //  The user has cancelled the capture
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
                    sendMainActivityFinishIntent();
                }
            } else {
                Log.d(LOG_TAG, String.format(getString(R.string.barcode_error),
                        CommonStatusCodes.getStatusCodeString(resultCode)));
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
        finish();
    }

    private void sendMainActivityFinishIntent()
    {
        //  Bit of a hack but want to return to the original calling application so call finish
        //  on any DWLite service activity.
        Intent finishIntent = new Intent(this, MainActivity.class);
        finishIntent.putExtra("finish", true);
        finishIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(finishIntent);
    }

    //  Work around for sending to a service.  Could probably be more intelligent here as just
    //  copied from stack overflow
    private Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
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

    //  The format of the decoded barcode is an int, convert this to a string so it's compatible
    //  with the return format we need.
    private String decoderFormatToString(int decoderFormat)
    {
        switch(decoderFormat) {
            case Barcode.AZTEC:
                return "AZTEC";
            case Barcode.CODABAR:
                return "CODABAR";
            case Barcode.CODE_39:
                return "CODE 39";
            case Barcode.CODE_93:
                return "CODE 93";
            case Barcode.CODE_128:
                return "CODE 128";
            case Barcode.DATA_MATRIX:
                return "Data Matrix";
            case Barcode.EAN_8:
                return "EAN 8";
            case Barcode.EAN_13:
                return "EAN 13";
            case Barcode.ISBN:
                return "ISBN";
            case Barcode.ITF:
                return "ITF";
            case Barcode.PDF417:
                return "PDF417";
            case Barcode.QR_CODE:
                return "QR Code";
            case Barcode.UPC_A:
                return "UPCA";
            case Barcode.UPC_E:
                return "UPCE";
            default:
                return "Unknown";
        }
    }

    //  Create the flags we need to pass to the Google Barcode API depending on the decoders defined
    // in the DW Lite profile
    private int giveSupportedDecoders()
    {
        int returnVal = 0;
        if (activeProfile.isDecoderEnabled(Profile.DECODER_UPCA))
            returnVal = returnVal | Barcode.UPC_A;
        if (activeProfile.isDecoderEnabled(Profile.DECODER_UPCE))
            returnVal = returnVal | Barcode.UPC_E;
        if (activeProfile.isDecoderEnabled(Profile.DECODER_EAN8))
            returnVal = returnVal | Barcode.EAN_8;
        if (activeProfile.isDecoderEnabled(Profile.DECODER_EAN13))
            returnVal = returnVal | Barcode.EAN_13;
        if (activeProfile.isDecoderEnabled(Profile.DECODER_RSS14))
        {}    //  Not supported by this decode engine.
        if (activeProfile.isDecoderEnabled(Profile.DECODER_CODE_39))
            returnVal = returnVal | Barcode.CODE_39;
        if (activeProfile.isDecoderEnabled(Profile.DECODER_CODE_93))
            returnVal = returnVal | Barcode.CODE_93;
        if (activeProfile.isDecoderEnabled(Profile.DECODER_CODE_128))
            returnVal = returnVal | Barcode.CODE_128;
        if (activeProfile.isDecoderEnabled(Profile.DECODER_ITF))
            returnVal = returnVal | Barcode.ITF;
        if (activeProfile.isDecoderEnabled(Profile.DECODER_RSS_Expanded))
        {}//  Not supported by this decode engine.
        if (activeProfile.isDecoderEnabled(Profile.DECODER_QR_CODE))
            returnVal = returnVal | Barcode.QR_CODE;
        if (activeProfile.isDecoderEnabled(Profile.DECODER_DATA_MATRIX))
            returnVal = returnVal | Barcode.DATA_MATRIX;

        //  Note: There are other formats supported by the scan engine but I haven't exposed an API
        //  for them.

        return returnVal;
    }

}
