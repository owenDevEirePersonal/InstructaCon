package com.deveire.dev.instructacon.remastered;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.deveire.dev.instructacon.AlertsRow;
import com.deveire.dev.instructacon.DownloadCallback;
import com.deveire.dev.instructacon.NetworkFragment;
import com.deveire.dev.instructacon.R;
import com.deveire.dev.instructacon.RegisterActivity;
import com.deveire.dev.instructacon.SignInsRow;
import com.deveire.dev.instructacon.TagsRow;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Manager2Activity extends FragmentActivity implements DownloadCallback<String>
{

    private GoogleMap mMap;

    private TextView mapText;

    private EditText stationIDText;
    private EditText alertTextText;
    private Button addCleanupAlertButton;
    private Button addSecurityAlertButton;
    private Button addTechnician1AlertButton;
    private Button addTechnician2AlertButton;
    private Button registerButton;
    private Button clearButton;
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

    private ArrayList<SignInRecord> allSignInRows;

    private int signInsCount;
    private int tagsCount;
    private int alertsCount;
    //[/Offline Variables]

    private SpannedString fullSignIn;
    ArrayList<SignInRecord> current3LatestSignins;

    //[Network and periodic location update, Variables]


    private final String SAVED_LOCATION_KEY = "79";

    private int pingingServerFor;

    private final int pingingServerFor_ItemIds = 1;
    private final int pingingServerFor_Locations = 2;
    private final int pingingServerFor_Extra_Locations = 3;
    private final int pingingServerFor_Keg_Last_Locations = 4;
    private final int pingingServerFor_UploadAlert = 5;
    private final int pingingServerFor_LatestSignins = 6;
    private final int pingingServerFor_Nothing = 0;


    private final String serverIPAddress = "http://192.168.1.188:8080/InstructaConServlet/ICServlet";
    //private final String serverIPAddress = "http://api.eirpin.com/api/TTServlet";
    private String serverURL;
    private NetworkFragment aNetworkFragment;
    //[/Network and periodic location update, Variables]

    //[Testing Variables]
    private ArrayList<LatLng> testStoreOfLocations;
    //[/Testing Variables]

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager2);




        mapText = (TextView) findViewById(R.id.mapText);
        signinText = (TextView) findViewById(R.id.signinsText);
        addCleanupAlertButton = (Button) findViewById(R.id.addJanitorAlertButton);
        addSecurityAlertButton = (Button) findViewById(R.id.addSecurityAlertButton);
        addTechnician1AlertButton = (Button) findViewById(R.id.addTechnician1AlertButton);
        addTechnician2AlertButton = (Button) findViewById(R.id.addTechnician2AlertButton);
        registerButton = (Button) findViewById(R.id.registerButton);
        clearButton = (Button) findViewById(R.id.clearButton);
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
                retrieveData();
                for (AlertData a: allAlerts)
                {
                    a.setActive(false);
                }
                saveData();
            }
        });

        allSignins = new ArrayList<String>();



        pingingServerFor = pingingServerFor_Nothing;


        //0000,0000 is a location in the middle of the atlantic occean south of western africa and unlikely to contain a golf course.



        //[Offline Setup]
        savedData = this.getApplicationContext().getSharedPreferences("InstructaCon SavedData", Context.MODE_PRIVATE);
        allAlerts = new ArrayList<AlertData>();
        allSignInRows = new ArrayList<SignInRecord>();
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

        restoreSavedValues(savedInstanceState);


        periodicGetSigninsTimer = new Timer();
        periodicGetSigninsTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                retrieveSignins();
            }
        }, 1000);

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "Manager_Activity_instructacon tag");
        wl.acquire();

    }

    @Override
    protected void onPause()
    {
        if(aNetworkFragment != null)
        {
            aNetworkFragment.cancelDownload();
        }

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
    private void retrieveData()
    {
        alertsCount = savedData.getInt("alertsCount", 0);
        tagsCount = savedData.getInt("tagsCount", 0);
        signInsCount = savedData.getInt("signInsCount", 0);
        Log.i("Offline", "Total number of Alerts: " + alertsCount + " Tags: " + tagsCount + " SigninsCount: " + signInsCount);
        allAlerts = new ArrayList<AlertData>();
        allTags = new ArrayList<IDTag>();
        allSignInRows = new ArrayList<SignInRecord>();

        for (int i = 0; i < alertsCount; i++)
        {
            allAlerts.add(new AlertData(savedData.getString("alerts" + i, "ERROR")));
        }

        for (int i = 0; i < tagsCount; i++)
        {
            allTags.add(new IDTag(savedData.getString("tags" + i, "ERROR")));
        }

        for (int i = 0; i < signInsCount; i++)
        {
            allSignInRows.add(new SignInRecord(savedData.getString("signIns" + i, "ERROR")));
            Log.i("Signins", savedData.getString("signIns" + i, "ERROR"));
        }
    }

    private void saveData()
    {
        SharedPreferences.Editor edit = savedData.edit();
        alertsCount = allAlerts.size();
        Log.i("Stuff", "Count = " + alertsCount);
        tagsCount = allTags.size();
        signInsCount = allSignInRows.size();
        edit.putInt("alertsCount", alertsCount);
        edit.putInt("tagsCount", tagsCount);
        edit.putInt("signInsCount", signInsCount);


        for (int i = 0; i < allAlerts.size(); i++)
        {
            edit.putString("alerts" + i, allAlerts.get(i).serialize());

        }

        for (int i = 0; i < allTags.size(); i++)
        {
            edit.putString("tags" + i, allTags.get(i).serializeTag());

        }

        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < allSignInRows.size(); i++)
        {
            edit.putString("signIns" + i, allSignInRows.get(i).serializeRecord());
        }

        edit.commit();
        Log.i("Offline Update", "Saved Data: alertCount: " + alertsCount + ", tagscount: " + tagsCount + ", signinscount: " + signInsCount);
        Log.i("Offline Update", "Saved Data: allalerts: " + allAlerts.size() + ", alltags: " + allTags.size() + ", allsignins: " + allSignInRows.size());
    }

    private IDTag findRowFromID(String tagIDin)
    {
        for (IDTag arow: allTags)
        {
            if(arow.getTagID().matches(tagIDin))
            {
                return arow;
            }
        }
        return null;
    }
    //[/Offline loading]\

    private void uploadAlert(String alertType)
    {
        retrieveData();
        allAlerts.add(new AlertData(stationIDText.getText().toString(), alertTextText.getText().toString(), true, alertType));
        saveData();
    }

    private void retrieveSignins()
    {
        retrieveData();
        Log.i("Signins", " starting retrieveSignins");

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                signinText.setText("");
            }
        });

        fullSignIn = new SpannedString("");
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (IDTag arow: allTags)
        {
            current3LatestSignins = new ArrayList<SignInRecord>();
            for (SignInRecord brow : allSignInRows)
            {
                if(arow.getTagID().matches(brow.getTagID()))
                {
                    if(current3LatestSignins.size() > 2)
                    {
                        sortRowList(brow);
                    }
                    else
                    {
                        current3LatestSignins.add(brow);
                    }
                }
            }
            int i = 0;
            for (SignInRecord crow: current3LatestSignins)
            {
                String aSignin;
                aSignin = findRowFromID(crow.getTagID()).getType() + " " + findRowFromID(crow.getTagID()).getName() + " signed in at " + crow.getStationID() + " at " + format.format(crow.getTimestamp());
                Log.i("Signin", " aSignin is: " + aSignin);


                SpannableString bSignin = new SpannableString("");

                //if currentRow is not the oldest row in current3LatestSignins
                if(i < current3LatestSignins.size() - 1)
                {
                    //if time since previous signin less than 1 minute
                    if(((crow.getTimestamp().getTime() - current3LatestSignins.get(i + 1).getTimestamp().getTime())/1000)/60 < 1)
                    {
                        aSignin += ". " + "less than a minute has elapsed since last signin.";
                        bSignin = new SpannableString(aSignin + "\n" + "\n");

                    }
                    else if(((crow.getTimestamp().getTime() - current3LatestSignins.get(i + 1).getTimestamp().getTime())/1000)/60 == 1)
                    {
                        aSignin += ". " + " 1 minute has elapsed since last signin.";
                        bSignin = new SpannableString(aSignin + "\n" + "\n");

                    }
                    else if(((crow.getTimestamp().getTime() - current3LatestSignins.get(i + 1).getTimestamp().getTime())/1000)/60 > 360)
                    {
                        int startOfUnderlinedPart = aSignin.length() + 2; //+2 because of the full stop and space that precedes the next line.
                        aSignin += ". " + "Over " + (((crow.getTimestamp().getTime() - current3LatestSignins.get(i + 1).getTimestamp().getTime()) / 1000) / 60) /60 + " hours have elapsed since last signin.";
                        bSignin = new SpannableString(aSignin + "\n" + "\n");

                        //if time since previous signin greater than 30(1) minutes
                        if(((crow.getTimestamp().getTime() - current3LatestSignins.get(i + 1).getTimestamp().getTime()) / 1000) / 60 > 30)
                        {
                            bSignin.setSpan(new UnderlineSpan(), startOfUnderlinedPart, bSignin.length(), 0);
                            bSignin.setSpan(new ForegroundColorSpan(Color.CYAN), startOfUnderlinedPart, bSignin.length(), 0);
                        }

                    }
                    else
                    {
                        int startOfUnderlinedPart = aSignin.length() + 2; //+2 because of the full stop and space that precedes the next line.
                        aSignin += ". " + ((crow.getTimestamp().getTime() - current3LatestSignins.get(i + 1).getTimestamp().getTime()) / 1000) / 60 + " minutes have elapsed since last signin.";
                        bSignin = new SpannableString(aSignin + "\n" + "\n");

                        //if time since previous signin greater than 30(1) minutes
                        if(((crow.getTimestamp().getTime() - current3LatestSignins.get(i + 1).getTimestamp().getTime()) / 1000) / 60 > 30)
                        {
                            bSignin.setSpan(new UnderlineSpan(), startOfUnderlinedPart, bSignin.length(), 0);
                            bSignin.setSpan(new ForegroundColorSpan(Color.CYAN), startOfUnderlinedPart, bSignin.length(), 0);
                        }
                    }
                }
                else
                {
                    aSignin += ". Time since last signin, unavaiable.";
                    bSignin = new SpannableString(aSignin + "\n" + "\n");
                }
                Log.i("Boop Test", "BOOP A LATEST SIGNIN LOADED " + aSignin);
                allSignins.add(aSignin);
                fullSignIn = (SpannedString) TextUtils.concat(fullSignIn, bSignin);


                i++;
            }

        }

        //for all non registered tags
        String aSignin;
        Log.i("Signins", "Starting Non registered tag signin");
        for (SignInRecord aRow: allSignInRows)
        {
            if (findRowFromID(aRow.getTagID()) == null)
            {
                //if signin does not match any registered tag
                //aSignin = "Other ID " + aRow.getTagID() + " signed in at " + aRow.getStationID() + " at " + format.format(aRow.getTimestamp());
                aSignin = (new IDTag(aRow.getSerializedTag())).getType() + " " + aRow.getTagName() + " signed in at " + aRow.getStationID() + " at " + format.format(aRow.getTimestamp());
                Log.i("Signins", "Non registered tag signin: " + aSignin);
                SpannableString bSignin = new SpannableString("");
                bSignin = new SpannableString(aSignin + "\n" + "\n");

                allSignins.add(aSignin);
                fullSignIn = (SpannedString) TextUtils.concat(fullSignIn, bSignin);
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

    private void sortRowList(SignInRecord inrow)
    {
        //TODO: fix this horribly inefficent double sorting by making it only do the first sort when its the first time the array reaches a size of 3.
        //0 is latest, 3 is earliest
        //Sort the current 3 times, just to be safe
        SignInRecord temp;
        if(current3LatestSignins.get(0).getTimestamp().before(current3LatestSignins.get(1).getTimestamp()))
        {
            temp = current3LatestSignins.get(1);
            current3LatestSignins.set(1, current3LatestSignins.get(0));
            current3LatestSignins.set(0, temp);
        }

        if(current3LatestSignins.get(1).getTimestamp().before(current3LatestSignins.get(2).getTimestamp()))
        {
            temp = current3LatestSignins.get(2);
            current3LatestSignins.set(2, current3LatestSignins.get(1));
            current3LatestSignins.set(1, temp);
        }



        //add the newly scanned time and sort the whole thing.
        current3LatestSignins.add(0, inrow);
        if(current3LatestSignins.get(0).getTimestamp().before(current3LatestSignins.get(1).getTimestamp()))
        {
            temp = current3LatestSignins.get(1);
            current3LatestSignins.set(1, current3LatestSignins.get(0));
            current3LatestSignins.set(0, temp);
        }

        if(current3LatestSignins.get(1).getTimestamp().before(current3LatestSignins.get(2).getTimestamp()))
        {
            temp = current3LatestSignins.get(2);
            current3LatestSignins.set(2, current3LatestSignins.get(1));
            current3LatestSignins.set(1, temp);
        }

        if(current3LatestSignins.get(2).getTimestamp().before(current3LatestSignins.get(3).getTimestamp()))
        {
            temp = current3LatestSignins.get(3);
            current3LatestSignins.set(3, current3LatestSignins.get(2));
            current3LatestSignins.set(2, temp);
        }

        current3LatestSignins.remove(3);
    }

    private void filterSignins(String inFilter)
    {
        Log.i("Signins", " starting filterSignins");
        /*TODO: redo signins text so that all entires are stored a strings in an arraylist, then redo this method to work off that list, rather than recompiling the
          entire list off of all the Arraylists of rows.*/
        allSignins = new ArrayList<String>();
        fullSignIn = new SpannedString("");
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


        ArrayList<SignInRecord> allMatchingRows = new ArrayList<SignInRecord>();
        for (SignInRecord aSignin: allSignInRows)
        {
            /*if(findRowFromID(aSignin.getTagID()).getName().contains(inFilter))
            {
                allMatchingRows.add(aSignin);
            }*/
            if(aSignin.getTagName().contains(inFilter))
            {
                allMatchingRows.add(aSignin);
            }
        }


        /*
        *  The following code is a identical to the code from retrieveSignins()
        *  except that the foreach loop of brow pulls from allMatchingRows rather than allSigninRows
        */
        for (IDTag arow: allTags)
        {
            current3LatestSignins = new ArrayList<SignInRecord>();
            for (SignInRecord brow : allMatchingRows)
            {
                if(arow.getTagID().matches(brow.getTagID()))
                {
                    if(current3LatestSignins.size() > 2)
                    {
                        sortRowList(brow);
                    }
                    else
                    {
                        current3LatestSignins.add(brow);
                    }
                }
            }
            int i = 0;
            for (SignInRecord crow: current3LatestSignins)
            {

                String aSignin = findRowFromID(crow.getTagID()).getType() + " " + findRowFromID(crow.getTagID()).getName() + " signed in at " + crow.getStationID()+ " at " + format.format(crow.getTimestamp());
                SpannableString bSignin = new SpannableString("");

                //if currentRow is not the oldest row in current3LatestSignins
                if(i < current3LatestSignins.size() - 1)
                {
                    //if time since previous signin less than 1 minute
                    if(((crow.getTimestamp().getTime() - current3LatestSignins.get(i + 1).getTimestamp().getTime())/1000)/60 < 1)
                    {
                        aSignin += ". " + "less than a minute has elapsed since last signin.";
                        bSignin = new SpannableString(aSignin + "\n" + "\n");

                    }
                    else if(((crow.getTimestamp().getTime() - current3LatestSignins.get(i + 1).getTimestamp().getTime())/1000)/60 == 1)
                    {
                        aSignin += ". " + " 1 minute has elapsed since last signin.";
                        bSignin = new SpannableString(aSignin + "\n" + "\n");

                    }
                    else
                    {
                        int startOfUnderlinedPart = aSignin.length() + 2; //+2 because of the full stop and space that precedes the next line.
                        aSignin += ". " + ((crow.getTimestamp().getTime() - current3LatestSignins.get(i + 1).getTimestamp().getTime()) / 1000) / 60 + " minutes have elapsed since last signin.";
                        bSignin = new SpannableString(aSignin + "\n" + "\n");

                        //if time since previous signin greater than 30(1) minutes
                        if(((crow.getTimestamp().getTime() - current3LatestSignins.get(i + 1).getTimestamp().getTime()) / 1000) / 60 > 30)
                        {
                            bSignin.setSpan(new UnderlineSpan(), startOfUnderlinedPart, bSignin.length(), 0);
                            bSignin.setSpan(new ForegroundColorSpan(Color.CYAN), startOfUnderlinedPart, bSignin.length(), 0);
                        }
                    }
                }
                else
                {
                    aSignin += ". Time since last signin, unavaiable.";
                    bSignin = new SpannableString(aSignin + "\n" + "\n");
                }
                Log.i("Boop Test", "BOOP A LATEST SIGNIN LOADED " + aSignin);
                allSignins.add(aSignin);
                fullSignIn = (SpannedString) TextUtils.concat(fullSignIn, bSignin);


                i++;
            }

        }

        //for all non registered tags
        String aSignin;
        Log.i("Signins", "Starting Non registered tag signin");
        for (SignInRecord aRow: allSignInRows)
        {
            if (findRowFromID(aRow.getTagID()) == null)
            {
                //if signin does not match any registered tag
                //aSignin = "Other ID " + aRow.getTagID() + " signed in at " + aRow.getStationID() + " at " + format.format(aRow.getTimestamp());
                aSignin = (new IDTag(aRow.getSerializedTag())).getType() + " " + aRow.getTagName() + " signed in at " + aRow.getStationID() + " at " + format.format(aRow.getTimestamp());
                Log.i("Signins", "Non registered tag signin: " + aSignin);
                SpannableString bSignin = new SpannableString("");
                bSignin = new SpannableString(aSignin + "\n" + "\n");

                allSignins.add(aSignin);
                fullSignIn = (SpannedString) TextUtils.concat(fullSignIn, bSignin);
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



    private boolean arrayListContains(ArrayList<LatLng> array, LatLng bLatLng)
    {
        for (LatLng aLatLng: array)
        {
            if(bLatLng.latitude == aLatLng.latitude && bLatLng.longitude == aLatLng.longitude)
            {
                return true;
            }
        }
        return false;
    }


    //**********[Location Update and server pinging Code]


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        //receive request changed.
    }

    @Override
    public void onSaveInstanceState(Bundle savedState)
    {
        super.onSaveInstanceState(savedState);
    }

    private void restoreSavedValues(Bundle savedInstanceState)
    {
        if (savedInstanceState != null)
        {

        }
    }

    //Update activity based on the results sent back by the servlet.
    @Override
    public void updateFromDownload(String result) {
        //intervalTextView.setText("Interval: " + result);
        Log.i("Download Update", "\n Starting UpdateFromDownload \n \n");
        try
        {
            if(result != null)
            {
                JSONArray jsonResultFromServer = new JSONArray(result);
                switch (pingingServerFor)
                {

                    case pingingServerFor_LatestSignins:
                        allSignins = new ArrayList<String>();
                        signinText.setText("");
                        String fullSignins = "";

                        for(int i = 0; i < jsonResultFromServer.length(); i++)
                        {
                            String aSignin = jsonResultFromServer.getJSONObject(i).getString("name") + " last signed in at " + jsonResultFromServer.getJSONObject(i).getString("stationID") + " at " + jsonResultFromServer.getJSONObject(i).getString("timestamp");
                            Log.i("Boop Test", "BOOP A LATEST SIGNIN LOADED " + aSignin);
                            allSignins.add(aSignin);
                            fullSignins += (aSignin + "\n" + "\n");
                        }
                        Log.i("Location Update", "Recieved locations.");
                        mapText.setText("Receiving Latest Signins");

                        signinText.setText(fullSignins);

                        break;

                    default: Log.e("Network Update", "PingingServerFor value does not match any known type"); break;
                }


            }
            else
            {
                mapText.setText("Error: network unavaiable");
            }

        }
        catch(JSONException e)
        {
            Log.e("DownloadUpdate Error", "JSONEception: " + e);
        }



        Log.e("Download Output", "" + result);
        // Update your UI here based on result of download.
    }

    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo;
    }

    @Override
    public void onProgressUpdate(int progressCode, int percentComplete) {
        switch(progressCode) {
            // You can add UI behavior for progress updates here.
            case Progress.ERROR:
                Log.e("Progress Error", "there was an error during a progress report at: " + percentComplete + "%");
                break;
            case Progress.CONNECT_SUCCESS:
                Log.i("Progress ", "connection successful during a progress report at: " + percentComplete + "%");
                break;
            case Progress.GET_INPUT_STREAM_SUCCESS:
                Log.i("Progress ", "input stream acquired during a progress report at: " + percentComplete + "%");
                break;
            case Progress.PROCESS_INPUT_STREAM_IN_PROGRESS:
                Log.i("Progress ", "input stream in progress during a progress report at: " + percentComplete + "%");
                break;
            case Progress.PROCESS_INPUT_STREAM_SUCCESS:
                Log.i("Progress ", "input stream processing successful during a progress report at: " + percentComplete + "%");
                break;
        }
    }

    @Override
    public void finishDownloading() {
        pingingServerFor = pingingServerFor_Nothing;
        Log.i("Network Update", "finished Downloading");
        if (aNetworkFragment != null) {
            Log.e("Network Update", "network fragment found, canceling download");
            aNetworkFragment.cancelDownload();
        }
    }

//**********[/Location Update and server pinging Code]


}
