package com.deveire.dev.instructacon;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompatSideChannelService;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class ManagerActivity extends FragmentActivity implements DownloadCallback<String>
{

    private GoogleMap mMap;

    private TextView mapText;

    private EditText stationIDText;
    private EditText alertTextText;
    private Button addCleanupAlertButton;
    private Button addSecurityAlertButton;
    private Button registerButton;
    private Button clearButton;
    private TextView signinText;


    private ArrayList<String> allSignins;


    Timer periodicGetSigninsTimer;

    private PowerManager pm;
    private PowerManager.WakeLock wl;

    //[Offline Variables]
    private SharedPreferences savedData;

    private ArrayList<TagsRow> allTags;

    private ArrayList<AlertsRow> allAlerts;

    private ArrayList<SignInsRow> allSignIns;

    private int signInsCount;
    private int tagsCount;
    private int alertsCount;
    //[/Offline Variables]

    private SpannedString fullSignIn;
    ArrayList<SignInsRow> current3LatestSignins;

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
        setContentView(R.layout.activity_manager);




        mapText = (TextView) findViewById(R.id.mapText);
        signinText = (TextView) findViewById(R.id.signinsText);
        addCleanupAlertButton = (Button) findViewById(R.id.addJanitorAlertButton);
        addSecurityAlertButton = (Button) findViewById(R.id.addSecurityAlertButton);
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
                    uploadSecurityAlert();
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
                    uploadCleanupAlert();
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
                startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                retrieveData();
                for (AlertsRow a: allAlerts)
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
        allAlerts = new ArrayList<AlertsRow>();
        allSignIns = new ArrayList<SignInsRow>();
        allTags = new ArrayList<TagsRow>();

        alertsCount = 0;
        signInsCount = 0;
        tagsCount = 0;
        //[/Offline Setup]



        restoreSavedValues(savedInstanceState);


        periodicGetSigninsTimer = new Timer();
        periodicGetSigninsTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                retrieveSignins();
            }
        }, 5000);

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
        allAlerts = new ArrayList<AlertsRow>();
        allTags = new ArrayList<TagsRow>();
        allSignIns = new ArrayList<SignInsRow>();

        for (int i = 0; i < alertsCount; i++)
        {
            allAlerts.add(new AlertsRow(savedData.getString("alerts_stationID" + i, "ERROR"), savedData.getString("alerts_alert" + i, "ERROR"), savedData.getBoolean("alerts_isActive" + i, false), savedData.getString("alerts_type" + i, "ERROR")));
        }

        for (int i = 0; i < tagsCount; i++)
        {
            allTags.add(new TagsRow(savedData.getString("tags_name" + i, "ERROR"), savedData.getString("tags_id" + i, "ERROR"), savedData.getString("tags_type" + i, "ERROR")));
        }

        for (int i = 0; i < signInsCount; i++)
        {
            allSignIns.add(new SignInsRow(savedData.getString("signIns_stationID" + i, "ERROR"), savedData.getString("signIns_tagID" + i, "ERROR"), savedData.getString("signIns_timestamp" + i, "ERROR")));
        }
    }

    private void saveData()
    {
        SharedPreferences.Editor edit = savedData.edit();
        alertsCount = allAlerts.size();
        tagsCount = allTags.size();
        signInsCount = allSignIns.size();
        edit.putInt("alertsCount", alertsCount);
        edit.putInt("tagsCount", tagsCount);
        edit.putInt("signInsCount", signInsCount);


        for (int i = 0; i < allAlerts.size(); i++)
        {
            edit.putString("alerts_stationID" + i, allAlerts.get(i).getStationID());
            edit.putString("alerts_alert" + i, allAlerts.get(i).getAlert());
            edit.putBoolean("alerts_isActive" + i, allAlerts.get(i).isActive());
            edit.putString("alerts_type" + i, allAlerts.get(i).getType());
        }

        for (int i = 0; i < allTags.size(); i++)
        {
            edit.putString("tags_name" + i, allTags.get(i).getName());
            edit.putString("tags_id" + i, allTags.get(i).getTagID());
            edit.putString("tags_type" + i, allTags.get(i).getType());
        }

        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < allSignIns.size(); i++)
        {
            edit.putString("signIns_stationID" + i, allSignIns.get(i).getStationID());
            edit.putString("signIns_tagID" + i, allSignIns.get(i).getTagID());
            edit.putString("signIns_timestamp" + i, format.format(allSignIns.get(i).getTimestamp()));
        }

        edit.commit();
        Log.i("Offline Update", "Saved Data: alertCount: " + alertsCount + ", tagscount: " + tagsCount + ", signinscount: " + signInsCount);
        Log.i("Offline Update", "Saved Data: allalerts: " + allAlerts.size() + ", alltags: " + allTags.size() + ", allsignins: " + allSignIns.size());
    }

    private TagsRow findRowFromID(String tagIDin)
    {
        for (TagsRow arow: allTags)
        {
            if(arow.getTagID().matches(tagIDin))
            {
                return arow;
            }
        }
        return null;
    }
    //[/Offline loading]\

    private void uploadCleanupAlert()
    {
        retrieveData();
        allAlerts.add(new AlertsRow(stationIDText.getText().toString(), alertTextText.getText().toString(), true, "Janitor"));
        saveData();
    }

    private void uploadSecurityAlert()
    {
        retrieveData();
        allAlerts.add(new AlertsRow(stationIDText.getText().toString(), alertTextText.getText().toString(), true, "Security Guard"));
        saveData();
    }

    private void retrieveSignins()
    {
        retrieveData();

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

        for (TagsRow arow: allTags)
        {
            current3LatestSignins = new ArrayList<SignInsRow>();
            for (SignInsRow brow : allSignIns)
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
            for (SignInsRow crow: current3LatestSignins)
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

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                signinText.setText(fullSignIn);
            }
        });


    }

    private void sortRowList(SignInsRow inrow)
    {
        //TODO: fix this horribly inefficent double sorting by making it only do the first sort when its the first time the array reaches a size of 3.
        //0 is latest, 3 is earliest
        //Sort the current 3 times, just to be safe
        SignInsRow temp;
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















/*


 */
