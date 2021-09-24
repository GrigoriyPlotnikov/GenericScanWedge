package com.darryncampbell.genericscanwedge.android;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    static final String LOG_TAG = "Generic Scan Wedge";
    private ArrayList<Profile> profiles = new ArrayList<>();
    private ProfilesListAdapter profilesListAdapter;
    private GenericScanWedgeIntentReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        if (getIntent().getBooleanExtra("finish", false))
        {
            //  Bit of a hack, enables the activity to be hidden after a scan
            finish();
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public  void onClick(View view) {
                profilesListAdapter.add(new Profile("New Profile", false));
                profilesListAdapter.notifyDataSetChanged();
                saveProfiles(profiles, getApplicationContext());
            }
        });

        broadcastReceiver = new GenericScanWedgeIntentReceiver();
        IntentFilter f = new IntentFilter(Datawedge.ACTION);
        registerReceiver(broadcastReceiver, f);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.v(LOG_TAG, "Resume");
        //  Read the profiles here and populate the UI
        try {
            ArrayList<Profile> profiles = readProfiles(getApplicationContext());
            if (profiles != null && profiles.size() > 0)
            {
                this.profiles = profiles;
            }
            else
            {
                profiles.add(new Profile("Profile0 (Default)", true));
            }
            ListView profilesListView = (ListView)findViewById(R.id.profiles_list);
            profilesListAdapter = new ProfilesListAdapter(this, this.profiles);
            profilesListView.setAdapter(profilesListAdapter);
        } catch (IOException e) {
            profiles.add(new Profile("Profile0 (Default)", true));
            ListView profilesListView = (ListView)findViewById(R.id.profiles_list);
            profilesListAdapter = new ProfilesListAdapter(this, this.profiles);
            profilesListView.setAdapter(profilesListAdapter);
        }catch (ClassNotFoundException e) {
            Log.e(LOG_TAG, "Something has gone wrong reading and parsing the existing profiles");
            //e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    //  Save the configured profiles to disk to persist.  This could be changed to the storage card
    //  or some other location later if we want.
    public static void saveProfiles(ArrayList<Profile> profiles, Context context)
    {
        try {
            FileOutputStream fos = context.openFileOutput(context.getResources().getString(R.string.profile_file_name), Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(profiles);
            os.close();
            fos.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error saving profiles to disk");
            //e.printStackTrace();
        }
    }

    //  Read the configured profiles from disk, ensure it's the same location they were saved to ;)
    public static ArrayList<Profile> readProfiles(Context context) throws IOException, ClassNotFoundException {
        FileInputStream fis = context.openFileInput(context.getResources().getString(R.string.profile_file_name));
        ObjectInputStream is = null;
        is = new ObjectInputStream(fis);
        ArrayList<Profile> profiles = (ArrayList<Profile>) is.readObject();
        is.close();
        fis.close();
        return profiles;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }
}
