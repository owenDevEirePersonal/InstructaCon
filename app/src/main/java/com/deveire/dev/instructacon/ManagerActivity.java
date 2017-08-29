package com.deveire.dev.instructacon;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
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

import java.util.ArrayList;

public class ManagerActivity extends FragmentActivity implements DownloadCallback<String>
{

    private GoogleMap mMap;

    private TextView mapText;

    private EditText stationIDText;
    private EditText alertTextText;
    private Button addCleanupAlertButton;
    private Button addSecurityAlertButton;

    private ArrayList<String> itemIdsFromServer;
    private String currentItemID;
    private int numberOfResultsToRetrieve;
    private ArrayList<LatLng> allCurrentItemLocations;

    private ArrayList<String> allCurrentKegIDs;

    private Location userLocation;

    //[Network and periodic location update, Variables]


    private final String SAVED_LOCATION_KEY = "79";

    private int pingingServerFor;

    private final int pingingServerFor_ItemIds = 1;
    private final int pingingServerFor_Locations = 2;
    private final int pingingServerFor_Extra_Locations = 3;
    private final int pingingServerFor_Keg_Last_Locations = 4;
    private final int pingingServerFor_UploadAlert = 5;
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
        addCleanupAlertButton = (Button) findViewById(R.id.addJanitorAlertButton);
        addSecurityAlertButton = (Button) findViewById(R.id.addSecurityAlertButton);
        stationIDText = (EditText) findViewById(R.id.stationIDEditText);
        alertTextText = (EditText) findViewById(R.id.alertTextEditText);

        addSecurityAlertButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                uploadSecurityAlert();
            }
        });

        addCleanupAlertButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                uploadCleanupAlert();
            }
        });

        itemIdsFromServer = new ArrayList<String>();
        allCurrentItemLocations = new ArrayList<LatLng>();

        currentItemID = "";
        numberOfResultsToRetrieve = 10;



        userLocation = new Location("Truck Manager");
        userLocation.setLatitude(52.663585);
        userLocation.setLongitude(-8.636135);

        pingingServerFor = pingingServerFor_Nothing;


        //0000,0000 is a location in the middle of the atlantic occean south of western africa and unlikely to contain a golf course.







        restoreSavedValues(savedInstanceState);

    }

    @Override
    protected void onPause()
    {
        if(aNetworkFragment != null)
        {
            aNetworkFragment.cancelDownload();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }





    private void uploadCleanupAlert()
    {
        pingingServerFor = pingingServerFor_UploadAlert;
        serverURL = serverIPAddress + "?request=addalert&stationid=" + stationIDText.getText().toString().replace(" ", "_") + "&alerttype=" + "janitor" + "&alerttext=" + alertTextText.getText().toString().replace(" ", "_");
        //lat and long are doubles, will cause issue? nope
        Log.i("Network Update", "Attempting to start download from uploadCleanUpAlert." + serverURL);
        aNetworkFragment = NetworkFragment.getInstance(getSupportFragmentManager(), serverURL);
    }

    private void uploadSecurityAlert()
    {
        pingingServerFor = pingingServerFor_UploadAlert;
        serverURL = serverIPAddress + "?request=addalert&stationid=" + stationIDText.getText().toString().replace(" ", "_") + "&alerttype=" + "security" + "&alerttext=" + alertTextText.getText().toString().replace(" ", "_");
        //lat and long are doubles, will cause issue? nope
        Log.i("Network Update", "Attempting to start download from uploadCleanUpAlert." + serverURL);
        aNetworkFragment = NetworkFragment.getInstance(getSupportFragmentManager(), serverURL);
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


                    case pingingServerFor_Keg_Last_Locations:
                        allCurrentItemLocations = new ArrayList<LatLng>();
                        allCurrentKegIDs = new ArrayList<String>();

                        for(int i = 0; i < jsonResultFromServer.length(); i++)
                        {
                            LatLng aloc = new LatLng(jsonResultFromServer.getJSONObject(i).getDouble("lat"), jsonResultFromServer.getJSONObject(i).getDouble("lon"));
                            allCurrentItemLocations.add(aloc);
                            allCurrentKegIDs.add(jsonResultFromServer.getJSONObject(i).getString("kegID"));
                            Log.i("Boop Test", "BOOP KEG LOCATION LOADED " + aloc.toString());
                        }
                        Log.i("Location Update", "Recieved locations.");
                        mapText.setText("Receving Keg Locations");

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
