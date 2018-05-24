package com.deveire.dev.instructacon.remastered;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.deveire.dev.instructacon.DownloadCallback;
import com.deveire.dev.instructacon.NetworkFragment;
import com.deveire.dev.instructacon.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static com.deveire.dev.instructacon.remastered.Utils.*;

public class Manager2Activity extends FragmentActivity
{

    private EditText stationIDText;
    private EditText alertTextText;
    private Button addCleanupAlertButton;
    private Button addSecurityAlertButton;
    private Button addTechnician1AlertButton;
    private Button addTechnician2AlertButton;
    private Button registerButton;
    private Button clearButton;
    private Button clearSigninsButton;
    private TextView signinText;
    private EditText filterEditText;

    private TextWatcher filterTextWatcher;


    private ArrayList<String> allSignins;


    Timer periodicGetSigninsTimer;

    private PowerManager pm;
    private PowerManager.WakeLock wl;

    //[Offline Variables]
    private SharedPreferences savedData;

    private ArrayList<IDTag> allTags;

    private ArrayList<AlertData> allAlerts;

    private ArrayList<SignInRecord> allSignInRecords;

    private int signInsCount;
    private int tagsCount;
    private int alertsCount;
    //[/Offline Variables]

    private String fullSignIn;
    ArrayList<SignInRecord> current3LatestSignins;


    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager2);

        signinText = (TextView) findViewById(R.id.signinsText);
        addCleanupAlertButton = (Button) findViewById(R.id.addJanitorAlertButton);
        addSecurityAlertButton = (Button) findViewById(R.id.addSecurityAlertButton);
        addTechnician1AlertButton = (Button) findViewById(R.id.addTechnician1AlertButton);
        addTechnician2AlertButton = (Button) findViewById(R.id.addTechnician2AlertButton);
        registerButton = (Button) findViewById(R.id.registerButton);
        clearButton = (Button) findViewById(R.id.clearButton);
        clearSigninsButton = (Button) findViewById(R.id.clearSigninsButton);
        stationIDText = (EditText) findViewById(R.id.stationIDEditText);
        alertTextText = (EditText) findViewById(R.id.alertTextEditText);

        addSecurityAlertButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(!stationIDText.getText().toString().matches("") && !alertTextText.getText().toString().matches(""))
                {
                    uploadAlert(IDTag.tagtype_SECURITY);
                    stationIDText.setText("");
                    alertTextText.setText("");
                }
            }
        });

        addCleanupAlertButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(!stationIDText.getText().toString().matches("") && !alertTextText.getText().toString().matches(""))
                {
                    uploadAlert(IDTag.tagtype_JANITOR);
                    stationIDText.setText("");
                    alertTextText.setText("");
                }
            }
        });

        addTechnician1AlertButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(!stationIDText.getText().toString().matches("") && !alertTextText.getText().toString().matches(""))
                {
                    uploadAlert(IDTag.tagtype_TECHNICIAN_CLASS_1);
                    stationIDText.setText("");
                    alertTextText.setText("");
                }
            }
        });

        addTechnician2AlertButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(!stationIDText.getText().toString().matches("") && !alertTextText.getText().toString().matches(""))
                {
                    uploadAlert(IDTag.tagtype_TECHNICIAN_CLASS_2);
                    stationIDText.setText("");
                    alertTextText.setText("");
                }
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(getApplicationContext(), Register2Activity.class));
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                allAlerts = retrieveAlerts(savedData);
                for (AlertData a: allAlerts)
                {
                    a.setActive(false);
                }
                saveAlertData(savedData, allAlerts);
            }
        });

        clearSigninsButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                allSignInRecords = new ArrayList<SignInRecord>();
                saveSignInData(savedData, allSignInRecords);
                retrieveAllSignins();
            }
        });

        allSignins = new ArrayList<String>();


        //[Offline Setup]
        savedData = this.getApplicationContext().getSharedPreferences("InstructaCon SavedData", Context.MODE_PRIVATE);
        allAlerts = new ArrayList<AlertData>();
        allSignInRecords = new ArrayList<SignInRecord>();
        allTags = new ArrayList<IDTag>();

        alertsCount = 0;
        signInsCount = 0;
        tagsCount = 0;
        //[/Offline Setup]

        //[filtering Signins]
        filterTextWatcher = new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {

            }

            @Override
            public void afterTextChanged(Editable s)
            {
                String filter = s.toString();
                Log.i("filterWatcher", filter);
                filterSignins(filter);
            }
        };

        filterEditText = (EditText) findViewById(R.id.filterEditText);
        filterEditText.addTextChangedListener(filterTextWatcher);
        //[/filtering Signins]



        periodicGetSigninsTimer = new Timer();
        periodicGetSigninsTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                retrieveAllSignins();
            }
        }, 1000, 30000);

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "Manager_Activity_instructacon tag");
        wl.acquire();

    }

    @Override
    protected void onPause()
    {

        periodicGetSigninsTimer.cancel();
        periodicGetSigninsTimer.purge();


        super.onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if(!wl.isHeld())
        {
            wl.acquire();
        }
    }

    @Override
    protected void onStop()
    {
        wl.release();
        super.onStop();

    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    //[Offline loading]

    private void saveData()
    {
        Utils.saveAllData(savedData, allTags, allAlerts, allSignInRecords);
    }

    //[/Offline loading]\

    private void uploadAlert(String alertType)
    {
        allAlerts = retrieveAlerts(savedData);
        allAlerts.add(new AlertData(stationIDText.getText().toString(), alertTextText.getText().toString(), true, alertType));
        saveAlertData(savedData, allAlerts);
    }

    private void retrieveAllSignins()
    {
        allSignInRecords = retrieveSignIns(savedData);
        Log.i("Signins", " starting retrieveAllSignins");

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                signinText.setText("");
            }
        });

        fullSignIn = "";
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        ArrayList<SignInRecord> allRecordsOldestFirst = binarySortSignins(allSignInRecords);
        //reverse the order of allRecords so its newest first.
        ArrayList<SignInRecord> allRecordsNewestFirst = new ArrayList<>();
        for(int i = allRecordsOldestFirst.size() - 1; i >= 0; i--)
        {
            allRecordsNewestFirst.add(allRecordsOldestFirst.get(i));

            fullSignIn +=  (allRecordsOldestFirst.get(i).getTagName() + " signed in at " + allRecordsOldestFirst.get(i).getStationID() + " at " + format.format(allRecordsOldestFirst.get(i).getTimestamp()) + ". \n\n");
        }

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                signinText.setText(fullSignIn);
            }
        });
    }

    private void filterSignins(String inFilter)
    {
        Log.i("Signins", " starting filterSignins");
        /*TODO: redo signins text so that all entires are stored a strings in an arraylist, then redo this method to work off that list, rather than recompiling the
          entire list off of all the Arraylists of rows.*/
        allSignins = new ArrayList<String>();
        fullSignIn = "";
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


        ArrayList<SignInRecord> allRecordsOldestFirst = binarySortSignins(allSignInRecords);
        //reverse the order of allRecords so its newest first.
        ArrayList<SignInRecord> allRecordsNewestFirst = new ArrayList<>();
        for(int i = allRecordsOldestFirst.size() - 1; i >= 0; i--)
        {
            if(allRecordsOldestFirst.get(i).getTagName().contains(inFilter))
            {
                allRecordsNewestFirst.add(allRecordsOldestFirst.get(i));

                fullSignIn += (allRecordsOldestFirst.get(i).getTagName() + " signed in at " + allRecordsOldestFirst.get(i).getStationID() + " at " + format.format(allRecordsOldestFirst.get(i).getTimestamp()) + ". \n\n");
            }
        }

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                signinText.setText(fullSignIn);
            }
        });
    }

    private ArrayList<SignInRecord> binarySortSignins(ArrayList<SignInRecord> records)
    {
        Log.i("Binary", "Starting binary sort on records of size: " + records.size());
        ArrayList<SignInRecord> output = new ArrayList<SignInRecord>();
        ArrayList<SignInRecord> before = new ArrayList<SignInRecord>();
        ArrayList<SignInRecord> after = new ArrayList<SignInRecord>();

        if(records.size() > 0)
        {


            SignInRecord middle = records.get((int) (records.size() / 2));
            for (SignInRecord aRecord : records)
            {
                if (aRecord.getTimestamp().after(middle.getTimestamp()))
                {
                    after.add(aRecord);
                }
                else if (aRecord.getTimestamp().before(middle.getTimestamp()))
                {
                    before.add(aRecord);
                }
            }

            if (before.size() > 1)
            {
                binarySortSignins(before);
            }

            if (after.size() > 1)
            {
                binarySortSignins(after);
            }

            output = before;
            output.add(middle);
            output.addAll(after);
        }

        return output;
    }
    
}
