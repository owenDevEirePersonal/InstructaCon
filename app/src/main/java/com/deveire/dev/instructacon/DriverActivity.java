package com.deveire.dev.instructacon;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;


import com.acs.bluetooth.*;

import com.deveire.dev.instructacon.bleNfc.*;
import com.deveire.dev.instructacon.bleNfc.card.*;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.GoogleMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class DriverActivity extends FragmentActivity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, LocationListener, DownloadCallback<String>
{

    private GoogleMap mMap;

    private TextView mapText;
    private EditText nameEditText;
    private EditText kegIDEditText;
    private Button scanKegButton;
    private Button pairReaderButton;
    private ImageView adImageView;

    final static int PAIR_READER_REQUESTCODE = 9;

    private SharedPreferences savedData;
    private String itemName;
    private int itemID;

    private boolean hasState;

    private TextToSpeech toSpeech;
    private String speechInText;
    private HashMap<String, String> endOfSpeakIndentifier;
    private String currentTagName;

    private Timer adSwapTimer;
    private int currentAdIndex;

    private ArrayList<String> idsOfTypeSecurity;
    private ArrayList<String> idsOfTypeJanitor;
    private final int ID_TYPE_Janitor = 1;
    private final int ID_TYPE_Security = 2;
    private int currentTagType;

    //[BLE Variables]
    private String storedScannerAddress;
    private final static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"; //UUID for changing notify or not
    private int REQUEST_ENABLE_BT;
    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private BluetoothAdapter.LeScanCallback leScanCallback;

    private BluetoothDevice btDevice;

    private BluetoothGatt btGatt;
    private BluetoothReaderGattCallback btLeGattCallback;
    //[/BLE Variables]

    //[Retreive Alert Data Variables]
    private Boolean pingingServerFor_alertData;
    private TextView alertDataText;
    private String currentUID;
    private String currentStationID;
    //[/Retreive Alert Data Variables]


    //[Tile Reader Variables]
    private DeviceManager deviceManager;
    private Scanner mScanner;

    private ProgressDialog dialog = null;

    private BluetoothDevice mNearestBle = null;
    private int lastRssi = -100;

    private int readCardFailCnt = 0;
    private int disconnectCnt = 0;
    private int readCardCnt = 0;
    private String startTimeString;
    private static volatile Boolean getMsgFlag = false;


    private Timer tileReaderTimer;
    private boolean uidIsFound;
    private boolean hasSufferedAtLeastOneFailureToReadUID;

    private boolean stopAllScans;

    //[/Tile Reader Variables]

    //[Headset Variables]
    /*private ArrayList<String> allHeadsetMacAddresses;
    private BluetoothDevice currentHeadsetDevice;

    private Timer headsetTimer;

    private BluetoothA2dp currentHeadsetProfile;
    private Method connectMethod;*/
    //[/Headset Variables]

    /*[Bar Reader Variables]
    private String barReaderInput;
    private Boolean barReaderInputInProgress;
    private Timer barReaderTimer;



    //[/Bar Reader Variables] */

    //[Scanner Variables]

    /* Default master key. */
    /*private static final String DEFAULT_1255_MASTER_KEY = "ACR1255U-J1 Auth";

    private static final byte[] AUTO_POLLING_START = { (byte) 0xE0, 0x00, 0x00, 0x40, 0x01 };
    private static final byte[] AUTO_POLLING_STOP = { (byte) 0xE0, 0x00, 0x00, 0x40, 0x00 };
    private static final byte[] GET_UID_APDU_COMMAND = {(byte)0xFF , (byte)0xCA, (byte)0x00, (byte)0x00, (byte)0x00};

    private int scannerConnectionState = BluetoothReader.STATE_DISCONNECTED;
    private BluetoothReaderManager scannerManager;
    private BluetoothReader scannerReader;

    private Timer scannerTimer;

    private static final int MAX_AUTHENTICATION_ATTEMPTS_BEFORE_TIMEOUT = 20;
    private boolean scannerIsAuthenticated;*/

    //[/Scanner Variables]

    //[Network and periodic location update, Variables]
    private GoogleApiClient mGoogleApiClient;
    private Location locationReceivedFromLocationUpdates;
    private Location userLocation;
    private DriverActivity.AddressResultReceiver geoCoderServiceResultReciever;
    private int locationScanInterval;

    LocationRequest request;
    private final int SETTINGS_REQUEST_ID = 8888;
    private final String SAVED_LOCATION_KEY = "79";

    private boolean pingingServer;
    private final String serverIPAddress = "http://192.168.1.188:8080/InstructaConServlet/ICServlet";
    //private final String serverIPAddress = "http://api.eirpin.com/api/TTServlet";
    private String serverURL;
    private NetworkFragment aNetworkFragment;
    //[/Network and periodic location update, Variables]


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.



        mapText = (TextView) findViewById(R.id.mapText);
        nameEditText = (EditText) findViewById(R.id.nameEditText);
        //kegIDEditText = (EditText) findViewById(R.id.kegIDEditText);
        //scanKegButton = (Button) findViewById(R.id.scanKegButton);

        adImageView = (ImageView) findViewById(R.id.addImageView);
        adImageView.setImageResource(R.drawable.drinkaware_ad);
        adImageView.setVisibility(View.VISIBLE);


        /*scanKegButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //scanKeg();
                //Log.i("Scanner Connection", "current card status = " + currentCardStatus);
                //transmitApdu();
            }
        });*/

        /*pairReaderButton = (Button) findViewById(R.id.pairReaderButton);
        pairReaderButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                storedScannerAddress = null;
                Intent pairReaderIntent = new Intent(getApplicationContext(), PairingActivity.class);
                startActivityForResult(pairReaderIntent, PAIR_READER_REQUESTCODE);
                /*if(btAdapter != null)
                {
                    btAdapter.startLeScan(leScanCallback);
                }*
            }
        });*/

        hasState = true;

        userLocation = new Location("Truck");
        userLocation.setLatitude(0);
        userLocation.setLongitude(0);

        savedData = this.getApplicationContext().getSharedPreferences("TruckyTrack SavedData", Context.MODE_PRIVATE);
        itemName = savedData.getString("itemName", "Unknown");
        itemID = savedData.getInt("itemID", 0);
        nameEditText.setText(itemName);


        pingingServer = false;

        //aNetworkFragment = NetworkFragment.getInstance(getSupportFragmentManager(), "https://192.168.1.188:8080/smrttrackerserver-1.0.0-SNAPSHOT/hello?isDoomed=yes");
        serverURL = serverIPAddress + "?request=storelocation" + Settings.Secure.ANDROID_ID.toString() + "&name=" + itemName + "&lat=" + 0000 + "&lon=" + 0000;


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();

        locationScanInterval = 60;//in seconds


        request = new LocationRequest();
        request.setInterval(locationScanInterval * 1000);//in mileseconds
        request.setFastestInterval(5000);//caps how fast the locations are recieved, as other apps could be triggering updates faster than our app.
        request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY); //accurate to 100 meters.

        LocationSettingsRequest.Builder requestBuilder = new LocationSettingsRequest.Builder().addLocationRequest(request);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        requestBuilder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>()
        {
            @Override
            public void onResult(@NonNull LocationSettingsResult aResult)
            {
                final Status status = aResult.getStatus();
                final LocationSettingsStates states = aResult.getLocationSettingsStates();
                switch (status.getStatusCode())
                {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.

                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try
                        {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(DriverActivity.this, SETTINGS_REQUEST_ID);
                        } catch (IntentSender.SendIntentException e)
                        {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        break;
                }
            }
        });

        geoCoderServiceResultReciever = new AddressResultReceiver(new Handler());


        pingingServerFor_alertData = false;
        alertDataText = (TextView) findViewById(R.id.kegDataText);

        toSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status)
            {
                Log.i("Text To Speech Update", "onInit Complete");
                toSpeech.setLanguage(Locale.ENGLISH);
                endOfSpeakIndentifier = new HashMap();
                endOfSpeakIndentifier.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "endOfSpeech");
                toSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener()
                {
                    @Override
                    public void onStart(String utteranceId)
                    {
                        Log.i("Text To Speech Update", "onStart called");
                    }

                    @Override
                    public void onDone(String utteranceId)
                    {
                        Log.i("Speech", utteranceId + " DONE!");
                        if(utteranceId.matches("End"))
                        {
                            try
                            {
                                Thread.sleep(10000);
                            } catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    adImageView.setVisibility(View.VISIBLE);
                                    alertDataText.setText("No Instructions.");
                                }
                            });
                        }
                        //toSpeech.shutdown();
                    }

                    @Override
                    public void onError(String utteranceId)
                    {
                        Log.i("Text To Speech Update", "ERROR DETECTED");
                    }
                });
            }
        });


        currentUID = "";
        currentStationID = "bathroom1";
        currentTagName = "Unknown Name";
        nameEditText.setText(currentStationID);


        //+++[Ad Swapping Setup]
        currentAdIndex = 1;

        adSwapTimer = new Timer("adSwapTimer");
        adSwapTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.i("Ad Update", "Changing ad");
                        switch (currentAdIndex)
                        {
                            case 1: adImageView.setImageResource(R.drawable.drinkaware_awareness_1); currentAdIndex++; break;
                            case 2: adImageView.setImageResource(R.drawable.report_ad); currentAdIndex++; break;
                            case 3: adImageView.setImageResource(R.drawable.drinkaware_ad2); currentAdIndex = 1; break;
                        }
                    }
                });

            }
        }, 0, 20000);
        //++++[/Ad Swapping Setup]


        setupTileScanner();
        //setupBluetoothScanner();
        /*
        barReaderTimer = new Timer();
        barReaderInput = "";
        barReaderInputInProgress = false;
        kegIDEditText.requestFocus();*/


        //setupHeadset();
        setupTypesOfID();
        currentTagType = 0;


        restoreSavedValues(savedInstanceState);

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        hasState = true;

        final IntentFilter intentFilter = new IntentFilter();

        uidIsFound = false;
        hasSufferedAtLeastOneFailureToReadUID = true;
        tileReaderTimer = new Timer();
        connectToTileScanner();

        //setupHeadset();

        //barReaderTimer = new Timer();



        /*
        /* Start to monitor bond state change /
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver, intentFilter);

        setupBluetoothScanner();
        */
    }

    @Override
    protected void onPause()
    {
        hasState = false;
        if(aNetworkFragment != null)
        {
            aNetworkFragment.cancelDownload();
        }

        tileReaderTimer.cancel();
        tileReaderTimer.purge();

        //if scanner is connected, disconnect it
        if(deviceManager.isConnection())
        {
            stopAllScans = true;
            deviceManager.requestDisConnectDevice();
        }

        if(mScanner.isScanning())
        {
            mScanner.stopScan();
        }

        adSwapTimer.cancel();
        adSwapTimer.purge();

        //headsetTimer.cancel();
        //headsetTimer.purge();

        /*
        barReaderTimer.cancel();
        barReaderTimer.purge();
        */

        //[Scanner onPause]
        /*
        /* Stop to monitor bond state change /
        unregisterReceiver(mBroadcastReceiver);

        scannerIsAuthenticated = false;

        /* Disconnect Bluetooth reader /
        disconnectReader();

        scannerTimer.cancel();
        scannerTimer.purge();
        */
        //[/Scanner On pause]

        super.onPause();
        //finish();
    }

    @Override
    protected void onStop()
    {
        hasState = false;

        SharedPreferences.Editor edit = savedData.edit();
        edit.putString("itemName", nameEditText.getText().toString());
        edit.putInt("itemID", itemID);

        //edit.putString("ScannerMacAddress", storedScannerAddress);


        edit.commit();



        /*
        if(btGatt != null)
        {
            btGatt.disconnect();
            btGatt.close();
        }
        */

        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        Log.i("PairingResult", "Call received to onActivity Result with reqyestCode: " + requestCode);
        if (requestCode == PAIR_READER_REQUESTCODE) {
            Log.i("PairingResult", "Received Pairing requestCode");
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Log.i("PairingResult", "Recieved Result Ok");
                storedScannerAddress = data.getStringExtra("BTMacAddress");
                SharedPreferences.Editor edit = savedData.edit();
                edit.putString("ScannerMacAddress", storedScannerAddress);
                edit.commit();
                Log.i("Pairing Result", "Recieved scannerMacAddress of : " + storedScannerAddress);


            }
        }
    }

    private void retrieveAlerts(String stationIDin, String tagIDin)
    {
        if(!stationIDin.matches(""))
        {
            serverURL = serverIPAddress + "?request=getalertsfor" + "&stationid=" + stationIDin.replace(" ", "_") + "&tagid=" + tagIDin;
            //lat and long are doubles, will cause issue? nope
            pingingServerFor_alertData = true;
            Log.i("Network Update", "Attempting to start download from retrieveAlerts. " + serverURL);
            aNetworkFragment = NetworkFragment.getInstance(getSupportFragmentManager(), serverURL);
        }
        else
        {
            Log.e("Network Update", "Error in RetreiveAlters, invalid uuid entered");
        }
    }

    private void retrieveSecurityAlerts(String stationIDin, String tagIDin)
    {
        if(!stationIDin.matches(""))
        {
            serverURL = serverIPAddress + "?request=getsecurityalertsfor" + "&stationid=" + stationIDin.replace(" ", "_")  + "&tagid=" + tagIDin;
            //lat and long are doubles, will cause issue? nope
            pingingServerFor_alertData = true;
            Log.i("Network Update", "Attempting to start download from retrieveAlerts. " + serverURL);
            aNetworkFragment = NetworkFragment.getInstance(getSupportFragmentManager(), serverURL);
        }
        else
        {
            Log.e("Network Update", "Error in RetreiveAlters, invalid uuid entered");
        }
    }

    private void scanKeg(String kegIDin)
    {
        if(!kegIDin.matches(""))
        {
            kegIDin = kegIDin.replace(' ', '_');
            serverURL = serverIPAddress + "?request=storekeg" + "&id=" + itemID + "&kegid=" + kegIDin + "&lat=" + locationReceivedFromLocationUpdates.getLatitude() + "&lon=" + locationReceivedFromLocationUpdates.getLongitude();
            //lat and long are doubles, will cause issue? nope
            Log.i("Network Update", "Attempting to start download from scanKeg. " + serverURL);
            aNetworkFragment = NetworkFragment.getInstance(getSupportFragmentManager(), serverURL);
        }
        else
        {
            Log.e("kegScan Error", "invalid uuid entered.");
        }
    }

    /*public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        Log.i("BarReader   ", "OnKeyUp Triggered");

        switch (keyCode)
        {
            case KeyEvent.KEYCODE_0: barReaderInput += "0"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_1: barReaderInput += "1"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_2: barReaderInput += "2"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_3: barReaderInput += "3"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_4: barReaderInput += "4"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_5: barReaderInput += "5"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_6: barReaderInput += "6"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_7: barReaderInput += "7"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_8: barReaderInput += "8"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_9: barReaderInput += "9"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_Q: barReaderInput += "Q"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_W: barReaderInput += "W"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_E: barReaderInput += "E"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_R: barReaderInput += "R"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_T: barReaderInput += "T"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_Y: barReaderInput += "Y"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_U: barReaderInput += "U"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_I: barReaderInput += "I"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_O: barReaderInput += "O"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_P: barReaderInput += "P"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_A: barReaderInput += "A"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_S: barReaderInput += "S"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_D: barReaderInput += "D"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_F: barReaderInput += "F"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_G: barReaderInput += "G"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_H: barReaderInput += "H"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_J: barReaderInput += "J"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_K: barReaderInput += "K"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_L: barReaderInput += "L"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_Z: barReaderInput += "Z"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_X: barReaderInput += "X"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_C: barReaderInput += "C"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_V: barReaderInput += "V"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_B: barReaderInput += "B"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_N: barReaderInput += "N"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_M: barReaderInput += "M"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_BACK: Log.i("BarReader   ", "Current Input equals Back"); finish(); break;
            default: Log.i("BarReader   ", "Unidentified symbol: " + keyCode); break;
        }

        return true;
    }*/

    /*private void barScannerSheduleUpload()
    {
        int delay = 1500;
        if(!barReaderInputInProgress)
        {
            barReaderInputInProgress = true;
            barReaderTimer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    scanKeg(barReaderInput);
                    Log.i("BarReader   ", "Final Input equals: " + barReaderInput);
                    barReaderInputInProgress = false;
                    final String barReaderInputToRead = barReaderInput;
                    barReaderInput = "";
                    barReaderTimer.schedule(new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            Log.i("BarReader  ", "Launching Data Request");
                            retrieveKegData(barReaderInputToRead);
                        }
                    }, 2000);
                }
            }, delay);
        }
    }*/

    private void setupTypesOfID()
    {
        idsOfTypeJanitor = new ArrayList<String>();
        idsOfTypeSecurity = new ArrayList<String>();

        idsOfTypeJanitor.add("0413b3caa74a81");
        idsOfTypeJanitor.add("0433bf3aa94a81");

        idsOfTypeSecurity.add("046e226a9a3184");
    }

    //returns the type of tag in numrical form, given the UID of the tag swiped
    private int getTypeFromUID(String inUID)
    {
        Log.i("setupSpeak", "Getting type of tag scanned");
        for (String aUID: idsOfTypeJanitor)
        {
            if(aUID.matches(inUID))
            {
                Log.i("setupSpeak", "Type of Tag = Janitor");
                return ID_TYPE_Janitor;
            }
        }

        for (String aUID: idsOfTypeSecurity)
        {
            if(aUID.matches(inUID))
            {
                Log.i("setupSpeak", "Type of Tag = Security");
                return ID_TYPE_Security;
            }
        }

        Log.i("setupSpeak", "Type of Tag = Unknown");
        return 0;
    }

    private void speakAlerts(ArrayList<String> inAlerts)
    {
        speechInText = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            adImageView.setVisibility(View.INVISIBLE);
            if (inAlerts.size() == 0)
            {
                toSpeech.speak("You have no new alerts.", TextToSpeech.QUEUE_FLUSH, null, "newAlerts");
                speechInText = "You have no new alerts:\n--------------------------------------------------------\n";
            }
            else
            {
                toSpeech.speak("You have new alerts.", TextToSpeech.QUEUE_FLUSH, null, "newAlerts");
                speechInText = "You have new alerts:\n--------------------------------------------------------\n";
                for (String aAlert : inAlerts)
                {
                    toSpeech.speak(aAlert, TextToSpeech.QUEUE_ADD, null, null);
                    speechInText += "\n" + aAlert + "\n";
                }
            }
            speakInstructions(currentUID);
        }
    }

    private void speakNetworkError()
    {
        speechInText = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            adImageView.setVisibility(View.INVISIBLE);
            toSpeech.speak("Failed to connect to Server. Alerts Unavailable.", TextToSpeech.QUEUE_FLUSH, null, "newAlerts");
            speechInText = "Failed to connect to Server. Alerts Unavailable. \n--------------------------------------------------------\n";
            speakInstructions(currentUID);
        }
    }

    public void speakInstructions(String uidIn)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            switch (currentTagType)
            {
                case ID_TYPE_Janitor:
                    speakDailyBathroomInstructions(uidIn);
                    break;

                case ID_TYPE_Security:
                    speakDailySecurityInstructions(uidIn);
                    break;

                default:
                    speakDailyUnknownInstructions(uidIn);
                    break;
            }
            toSpeech.speak("End of Instructions", TextToSpeech.QUEUE_ADD, null, "End");
            alertDataText.setText(speechInText);

        }
    }

    public void speakDailyBathroomInstructions(String uidIn)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            toSpeech.speak("Here are your instructions, Sanitation Engineer " + getNameFromUID(uidIn), TextToSpeech.QUEUE_ADD, null, "instruct1");
            speechInText += "\nHere are your instructions, Sanitation Engineer " + getNameFromUID(uidIn) + ":\n--------------------------------------------------------\n";
            toSpeech.speak(" 1. Check slash change the toilet paper.", TextToSpeech.QUEUE_ADD, null, "instruct2");
            speechInText += "\n1. Check/Change the toilet paper.\n";
            toSpeech.speak(" 2. Mop the floor.", TextToSpeech.QUEUE_ADD, null, "instruct3");
            speechInText += "\n2. Mop the floor.\n";
            toSpeech.speak(" 3. Empty the Trash.", TextToSpeech.QUEUE_ADD, null, "instruct4");
            speechInText += "\n3. Empty the Trash.\n";
            toSpeech.speak(" 4. Wipe the Mirror", TextToSpeech.QUEUE_ADD, null, null);
            speechInText += "\n 4. Wipe the Mirror.\n";
            toSpeech.speak(" 5. Clean the Drains", TextToSpeech.QUEUE_ADD, null, null);
            speechInText += "\n 5. Clean the Drains.\n";
            toSpeech.speak(" 6. Change the Towels", TextToSpeech.QUEUE_ADD, null, null);
            speechInText += "\n 6. Change the Towels.\n";

            Calendar aCalender = Calendar.getInstance();
            toSpeech.speak(" Today's Weekly Task:", TextToSpeech.QUEUE_ADD, null, null);
            speechInText += "\n\nToday's Weekly Task:\n--------------------------------------------------------\n";
            switch (aCalender.get(Calendar.DAY_OF_WEEK))
            {
                case Calendar.MONDAY:
                    toSpeech.speak(" 7. Clean the Faucets", TextToSpeech.QUEUE_ADD, null, null);
                    speechInText += "\n 7. Clean the Faucets.\n";
                    break;

                case Calendar.TUESDAY:
                    toSpeech.speak(" 7. Wash the Rugs", TextToSpeech.QUEUE_ADD, null, null);
                    speechInText += "\n 7. Wash the Rugs.\n";
                    break;

                case Calendar.WEDNESDAY:
                    toSpeech.speak(" 7. Refill the medicine cabinet", TextToSpeech.QUEUE_ADD, null, null);
                    speechInText += "\n 7. Refill the medicine cabinet.\n";
                    break;

                case Calendar.THURSDAY:
                    toSpeech.speak(" 7. Wash the walls", TextToSpeech.QUEUE_ADD, null, null);
                    speechInText += "\n 7. Wash the walls.\n";
                    break;

                case Calendar.FRIDAY:
                    toSpeech.speak(" 7. Scrub the floors and clean the grout", TextToSpeech.QUEUE_ADD, null, null);
                    speechInText += "\n 7. Scrubs the floors and clean the grout.\n";
                    break;
                case Calendar.SATURDAY:
                    toSpeech.speak(" 7. Clean the toilet bowls, the sinks and the tubs.", TextToSpeech.QUEUE_ADD, null, null);
                    speechInText += "\n 7. Clean the toilet bowls, the sinks and the tubs.\n";
                    break;
            }

            toSpeech.speak(" Monthly Tasks Remaining: ", TextToSpeech.QUEUE_ADD, null, null);
            speechInText += "\n\n Monthly Tasks Remaing:\n--------------------------------------------------------\n";
            toSpeech.speak(" 8. Clean the Windows and Vents.", TextToSpeech.QUEUE_ADD, null, null);
            speechInText += "\n 8. Clean the Windows and Vents.\n";
            toSpeech.speak(" 9. Clean the showers.", TextToSpeech.QUEUE_ADD, null, null);
            speechInText += "\n 9. Clean the showers.\n";
            /*toSpeech.speak(" 10. Dust the ceilings.", TextToSpeech.QUEUE_ADD, null, null);
            speechInText += "\n 10. Dust the ceilings.\n";
            toSpeech.speak(" 11. Wipe the front and back of the doors.", TextToSpeech.QUEUE_ADD, null, null);
            speechInText += "\n 11. Wipe the front and back of the doors.\n";
            toSpeech.speak(" 12. Purge the Toiletries.", TextToSpeech.QUEUE_ADD, null, null);
            speechInText += "\n 12. Purge the Toiletries.\n";
            toSpeech.speak(" 13. Wash the shower curtains.", TextToSpeech.QUEUE_ADD, null, null);
            speechInText += "\n 13. Wash the shower curtains.\n";*/
        }
    }

    public void speakDailySecurityInstructions(String uidIn)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            toSpeech.speak("Here are your instructions, Security Guard " + getNameFromUID(uidIn), TextToSpeech.QUEUE_ADD, null, "instruct1");
            speechInText += "\nHere are your instructions, Security Guard " + getNameFromUID(uidIn) + ":\n--------------------------------------------------------\n";
            toSpeech.speak(" 1. Is the bathroom clean?", TextToSpeech.QUEUE_ADD, null, "instruct2");
            speechInText += "\n1. Is the bathroom clean?\n";
            toSpeech.speak(" 2. Is the water running?", TextToSpeech.QUEUE_ADD, null, "instruct3");
            speechInText += "\n2. Is the water running?\n";
            toSpeech.speak(" 3. Are any of the stalls locked?", TextToSpeech.QUEUE_ADD, null, "instruct4");
            speechInText += "\n3. Are any of the stalls locked?\n";

            Calendar aCalender = Calendar.getInstance();
            //toSpeech.speak(" Today's Weekly Task:", TextToSpeech.QUEUE_ADD, null, null);
            //speechInText += "\n\nToday's Weekly Task:\n--------------------------------------------------------\n";
            switch (aCalender.get(Calendar.DAY_OF_WEEK))
            {

            }
        }
    }

    public void speakDailyUnknownInstructions(String uidIn)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            speechInText = "";
            adImageView.setVisibility(View.INVISIBLE);
            toSpeech.speak("Your ID, " + uidIn.toString() + ", is not on record,", TextToSpeech.QUEUE_ADD, null, "instruct1");
            toSpeech.speak("  please report to your supervisor.", TextToSpeech.QUEUE_ADD, null, "instruct2");
            speechInText += "\nYour id, " + uidIn.toString() + ", is not on record, please report to your supervisor.\n";
        }
    }

    private String getNameFromUID(String inUID)
    {
        return currentTagName;
        /*switch (inUID)
        {
            case "0413b3caa74a81":
                return "Greg Alderman";

            case "0433bf3aa94a81":
                return "Frank Grimes";

            case "046e226a9a3184":
                return "Cloe Fitzgerald";

            default:
                return "Number " + inUID;
        }*/
    }

//---[Headset Code]
/*
    private void setupHeadset()
    {
        allHeadsetMacAddresses = new ArrayList<String>();
        allHeadsetMacAddresses.add("E9:08:EF:C4:1A:65");

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btAdapter.getProfileProxy(this, new BluetoothProfile.ServiceListener()
        {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy)
            {
                currentHeadsetProfile = (BluetoothA2dp) proxy;
                Log.i("Headset Update", "Storing Proxy Profile: " + proxy.toString());
                try
                {
                    connectMethod = BluetoothA2dp.class.getDeclaredMethod("connect", BluetoothDevice.class);
                    Log.i("Headset Update", "Storing Method connect: " + connectMethod.toString());
                }
                catch (NoSuchMethodException e)
                {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(int profile)
            {
                Log.i("Headset Update", "a service has disconnected: " + profile);
            }
        },   BluetoothProfile.A2DP);


        headsetTimer = new Timer();

        headsetTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                Log.i("Headset Update", "Device is badger:" + currentHeadsetProfile.getConnectionState(currentHeadsetDevice));
                if(currentHeadsetProfile.getConnectionState(currentHeadsetDevice) != BluetoothProfile.STATE_CONNECTED)
                {
                    for (String aHeadsetAddress: allHeadsetMacAddresses)
                    {
                        Log.i("Headset Update", "Attempting to link to headset at address: " + aHeadsetAddress);
                        currentHeadsetDevice = btAdapter.getRemoteDevice(aHeadsetAddress);
                        Log.i("Headset Update", "Connected Devices: " + btAdapter.getBondedDevices().toString());
                        try
                        {
                            Log.i("Headset Update", "Attempting to connect to device, with device: " + currentHeadsetDevice + ". and profile: " + currentHeadsetProfile);
                            connectMethod.invoke(currentHeadsetProfile, currentHeadsetDevice);
                        }
                        catch (IllegalAccessException e)
                        {
                            Log.e("Headset Update", "Error Illegal Access exception");
                            e.printStackTrace();
                        }
                        catch (InvocationTargetException e)
                        {
                            Log.i("Headset Update", "Error Invocation Target exception");
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, 5000, 5000);

    }
*/
//---[/Headset Code]


    //+++[TileScanner Code]
    private void setupTileScanner()
    {
        dialog = new ProgressDialog(DriverActivity.this);
        //Set processing bar style(round,revolving)
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        //Set a Button for ProgressDialog
        dialog.setButton("Cancel", new ProgressDialog.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deviceManager.requestDisConnectDevice();
            }
        });
        //set if the processing bar of ProgressDialog is indeterminate
        dialog.setIndeterminate(false);

        //Initial device operation classes
        mScanner = new Scanner(DriverActivity.this, scannerCallback);
        deviceManager = new DeviceManager(DriverActivity.this);
        deviceManager.setCallBack(deviceManagerCallback);


        tileReaderTimer = new Timer();
        uidIsFound = false;
        hasSufferedAtLeastOneFailureToReadUID = false;

        //connection is called from OnResume Anyway
        //connectToTileScanner();
    }

    //Scanner CallBack
    private ScannerCallback scannerCallback = new ScannerCallback() {
        @Override
        public void onReceiveScanDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
            super.onReceiveScanDevice(device, rssi, scanRecord);
            System.out.println("Activity found a device：" + device.getName() + "Signal strength：" + rssi );
            //Scan bluetooth and record the one has the highest signal strength
            if ( (device.getName() != null) && (device.getName().contains("UNISMES") || device.getName().contains("BLE_NFC")) ) {
                if (mNearestBle != null) {
                    if (rssi > lastRssi) {
                        mNearestBle = device;
                    }
                }
                else {
                    mNearestBle = device;
                    lastRssi = rssi;
                }
            }
        }

        @Override
        public void onScanDeviceStopped() {
            super.onScanDeviceStopped();
        }
    };

    //Callback function for device manager
    private DeviceManagerCallback deviceManagerCallback = new DeviceManagerCallback()
    {
        @Override
        public void onReceiveConnectBtDevice(boolean blnIsConnectSuc) {
            super.onReceiveConnectBtDevice(blnIsConnectSuc);
            if (blnIsConnectSuc) {
                Log.i("TileScanner", "Activity Connection successful");
                Log.i("TileScanner", "Connection successful!\r\n");
                Log.i("TileScanner", "SDK version：" + deviceManager.SDK_VERSIONS + "\r\n");

                // Send order after 500ms delay
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.sendEmptyMessage(3);
            }
        }

        @Override
        public void onReceiveDisConnectDevice(boolean blnIsDisConnectDevice) {
            super.onReceiveDisConnectDevice(blnIsDisConnectDevice);
            Log.i("TileScanner", "Activity Unlink");
            Log.i("TileScanner", "Unlink!");
            handler.sendEmptyMessage(5);
        }

        @Override
        public void onReceiveConnectionStatus(boolean blnIsConnection) {
            super.onReceiveConnectionStatus(blnIsConnection);
            System.out.println("Activity Callback for Connection Status");
        }

        @Override
        public void onReceiveInitCiphy(boolean blnIsInitSuc) {
            super.onReceiveInitCiphy(blnIsInitSuc);
        }

        @Override
        public void onReceiveDeviceAuth(byte[] authData) {
            super.onReceiveDeviceAuth(authData);
        }

        @Override
        public void onReceiveRfnSearchCard(boolean blnIsSus, int cardType, byte[] bytCardSn, byte[] bytCarATS)
        {
            super.onReceiveRfnSearchCard(blnIsSus, cardType, bytCardSn, bytCarATS);
            if (!blnIsSus)
            {
                return;
            }
            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 0; i < bytCardSn.length; i++)
            {
                stringBuffer.append(String.format("%02x", bytCardSn[i]));
            }

            StringBuffer stringBuffer1 = new StringBuffer();
            for (int i = 0; i < bytCarATS.length; i++)
            {
                stringBuffer1.append(String.format("%02x", bytCarATS[i]));
            }

            final StringBuffer outUID = stringBuffer;
            if (hasSufferedAtLeastOneFailureToReadUID)
            {
                uidIsFound = true;
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.i("TileScanner", "callback received: UID = " + outUID.toString());
                        alertDataText.setText(outUID);

                        currentUID = outUID.toString();
                        currentStationID = nameEditText.getText().toString();
                        retrieveAlerts(currentStationID, currentUID);
                        /*switch (getTypeFromUID(currentUID))
                        {
                            case ID_TYPE_Janitor: retrieveAlerts(currentStationID, currentUID); break;

                            case ID_TYPE_Security: retrieveSecurityAlerts(currentStationID, currentUID); break;

                            default: speakInstructions(currentUID); break;
                        }*/

                        //TODO: fix this
                    }
                });
            }
            else
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.i("TileScanner", "UID found without a prior failure, assuming its a tag left on the scanner");
                        alertDataText.setText("UID found without a prior failure, assuming its a tag left on the scanner");
                    }
                });

            }

            Log.i("TileScanner","Activity Activate card callback received：UID->" + stringBuffer + " ATS->" + stringBuffer1);
        }

        @Override
        public void onReceiveRfmSentApduCmd(byte[] bytApduRtnData) {
            super.onReceiveRfmSentApduCmd(bytApduRtnData);

            StringBuffer stringBuffer = new StringBuffer();
            for (int i=0; i<bytApduRtnData.length; i++) {
                stringBuffer.append(String.format("%02x", bytApduRtnData[i]));
            }
            Log.i("TileScanner", "Activity APDU callback received：" + stringBuffer);
        }

        @Override
        public void onReceiveRfmClose(boolean blnIsCloseSuc) {
            super.onReceiveRfmClose(blnIsCloseSuc);
        }
    };


    private void connectToTileScanner()
    {
        if (deviceManager.isConnection()) {
            deviceManager.requestDisConnectDevice();
            return;
        }
        Log.i("TileScanner", "connect To Update: Searching Devices");
        //handler.sendEmptyMessage(0);
        if (!mScanner.isScanning()) {
            mScanner.startScan(0);
            mNearestBle = null;
            lastRssi = -100;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    int searchCnt = 0;
                    while ((mNearestBle == null) && (searchCnt < 50000) && (mScanner.isScanning())) {
                        searchCnt++;
                        try {
                            //Log.i("TileScanner", "connect to Update: Sleeping Thread while scanning");
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    try {
                        Log.i("TileScanner", "connect to Update: Sleeping Thread after scan comeplete");
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //mScanner.stopScan();
                    if (mNearestBle != null && !deviceManager.isConnection()) {
                        mScanner.stopScan();
                        Log.i("TileScanner", "connect To Update: Connecting to Device");
                        handler.sendEmptyMessage(0);
                        deviceManager.requestConnectBleDevice(mNearestBle.getAddress());
                    }
                    else {
                        Log.i("TileScanner", "connect To Update: Cannot Find Devices");
                        handler.sendEmptyMessage(0);
                    }
                }
            }).start();
        }
    }

    //Read card Demo
    private void readCardDemo() {
        readCardCnt++;
        Log.i("TileScanner", "Activity Send scan/activate order");
        deviceManager.requestRfmSearchCard((byte) 0x00, new DeviceManager.onReceiveRfnSearchCardListener() {
            @Override
            public void onReceiveRfnSearchCard(final boolean blnIsSus, int cardType, byte[] bytCardSn, byte[] bytCarATS) {
                deviceManager.mOnReceiveRfnSearchCardListener = null;
                if ( !blnIsSus ) {
                    Log.i("TileScanner", "No card is found！Please put ShenZhen pass on the bluetooth card reading area first");
                    handler.sendEmptyMessage(0);
                    Log.i("TileScanner", "No card is found！");
                    hasSufferedAtLeastOneFailureToReadUID = true;
                    readCardFailCnt++;

                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            //alertDataText.setText("No card detected");
                        }
                    });
                    //handler.sendEmptyMessage(4);
                    return;
                }
                if ( cardType == DeviceManager.CARD_TYPE_ISO4443_B ) {   //Find ISO14443-B card（identity card）
                    final Iso14443bCard card = (Iso14443bCard)deviceManager.getCard();
                    if (card != null) {

                        Log.i("TileScanner", "found ISO14443-B card->UID:(Identity card send 0036000008 order to get UID)\r\n");
                        handler.sendEmptyMessage(0);
                        //Order stream to get Identity card DN code
                        final byte[][] sfzCmdBytes = {
                                {0x00, (byte)0xa4, 0x00, 0x00, 0x02, 0x60, 0x02},
                                {0x00, 0x36, 0x00, 0x00, 0x08},
                                {(byte)0x80, (byte)0xB0, 0x00, 0x00, 0x20},
                        };
                        System.out.println("Send order stream");
                        Handler readSfzHandler = new Handler(DriverActivity.this.getMainLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                final Handler theHandler = msg.getTarget();
                                if (msg.what < sfzCmdBytes.length) {  // Execute order stream recurrently
                                    final int index = msg.what;
                                    StringBuffer stringBuffer = new StringBuffer();
                                    for (int i=0; i<sfzCmdBytes[index].length; i++) {
                                        stringBuffer.append(String.format("%02x", sfzCmdBytes[index][i]));
                                    }
                                    Log.i("TileScanner", "Send：" + stringBuffer + "\r\n");
                                    handler.sendEmptyMessage(0);
                                    card.bpduExchange(sfzCmdBytes[index], new Iso14443bCard.onReceiveBpduExchangeListener() {
                                        @Override
                                        public void onReceiveBpduExchange(boolean isCmdRunSuc, byte[] bytBpduRtnData) {
                                            if (!isCmdRunSuc) {
                                                card.close(null);
                                                return;
                                            }
                                            StringBuffer stringBuffer = new StringBuffer();
                                            for (int i=0; i<bytBpduRtnData.length; i++) {
                                                stringBuffer.append(String.format("%02x", bytBpduRtnData[i]));
                                            }
                                            Log.i("TileScanner", "Return：" + stringBuffer + "\r\n");
                                            handler.sendEmptyMessage(0);
                                            theHandler.sendEmptyMessage(index + 1);
                                        }
                                    });
                                }
                                else{ //Order stream has been excuted,shut antenna down
                                    card.close(null);
                                    handler.sendEmptyMessage(4);
                                }
                            }
                        };
                        readSfzHandler.sendEmptyMessage(0);  //Start to execute the first order
                    }
                }
                else if (cardType == DeviceManager.CARD_TYPE_ISO4443_A){  //Find ACPU card
                    Log.i("TileScanner", "Card activation status：" + blnIsSus);
                    Log.i("TileScanner", "Send APDU order - Select main file");

                    final CpuCard card = (CpuCard)deviceManager.getCard();
                    if (card != null) {
                        Log.i("TileScanner", "Found CPU card->UID:" + card.uidToString() + "\r\n");
                        handler.sendEmptyMessage(0);
                        card.apduExchange(SZTCard.getSelectMainFileCmdByte(), new CpuCard.onReceiveApduExchangeListener() {
                            @Override
                            public void onReceiveApduExchange(boolean isCmdRunSuc, byte[] bytApduRtnData) {
                                if (!isCmdRunSuc) {
                                    Log.i("TileScanner", "Main file selection failed");
                                    card.close(null);
                                    readCardFailCnt++;
                                    handler.sendEmptyMessage(4);
                                    return;
                                }
                                Log.i("TileScanner", "Send APDU order- read balance");
                                card.apduExchange(SZTCard.getBalanceCmdByte(), new CpuCard.onReceiveApduExchangeListener() {
                                    @Override
                                    public void onReceiveApduExchange(boolean isCmdRunSuc, byte[] bytApduRtnData) {
                                        if (SZTCard.getBalance(bytApduRtnData) == null) {
                                            Log.i("TileScanner", "This is not ShenZhen Pass！");
                                            handler.sendEmptyMessage(0);
                                            Log.i("TileScanner", "This is not ShenZhen Pass！");
                                            card.close(null);
                                            readCardFailCnt++;
                                            handler.sendEmptyMessage(4);
                                            return;
                                        }
                                        Log.i("TileScanner", "ShenZhen Pass balance：" + SZTCard.getBalance(bytApduRtnData));
                                        handler.sendEmptyMessage(0);
                                        System.out.println("Balance：" + SZTCard.getBalance(bytApduRtnData));
                                        System.out.println("Send APDU order -read 10 trading records");
                                        Handler readSztHandler = new Handler(DriverActivity.this.getMainLooper()) {
                                            @Override
                                            public void handleMessage(Message msg) {
                                                final Handler theHandler = msg.getTarget();
                                                if (msg.what <= 10) {  //Read 10 trading records recurrently
                                                    final int index = msg.what;
                                                    card.apduExchange(SZTCard.getTradeCmdByte((byte) msg.what), new CpuCard.onReceiveApduExchangeListener() {
                                                        @Override
                                                        public void onReceiveApduExchange(boolean isCmdRunSuc, byte[] bytApduRtnData) {
                                                            if (!isCmdRunSuc) {
                                                                card.close(null);
                                                                readCardFailCnt++;
                                                                handler.sendEmptyMessage(4);
                                                                return;
                                                            }
                                                            Log.i("TileScanner", "\r\n" + SZTCard.getTrade(bytApduRtnData));
                                                            handler.sendEmptyMessage(0);
                                                            theHandler.sendEmptyMessage(index + 1);
                                                        }
                                                    });
                                                }
                                                else if (msg.what == 11){ //Shut antenna down
                                                    card.close(null);
                                                    handler.sendEmptyMessage(4);
                                                }
                                            }
                                        };
                                        readSztHandler.sendEmptyMessage(1);
                                    }
                                });
                            }
                        });
                    }
                    else {
                        readCardFailCnt++;
                        handler.sendEmptyMessage(4);
                    }
                }
                else if (cardType == DeviceManager.CARD_TYPE_FELICA) { //find Felica card
                    FeliCa card = (FeliCa) deviceManager.getCard();
                    if (card != null) {
                        Log.i("TileScanner", "Read data block 0000 who serves 008b：\r\n");
                        handler.sendEmptyMessage(0);
                        byte[] pServiceList = {(byte) 0x8b, 0x00};
                        byte[] pBlockList = {0x00, 0x00, 0x00};
                        card.read((byte) 1, pServiceList, (byte) 1, pBlockList, new FeliCa.onReceiveReadListener() {
                            @Override
                            public void onReceiveRead(boolean isSuc, byte pRxNumBlocks, byte[] pBlockData) {
                                if (isSuc) {
                                    StringBuffer stringBuffer = new StringBuffer();
                                    for (int i = 0; i < pBlockData.length; i++) {
                                        stringBuffer.append(String.format("%02x", pBlockData[i]));
                                    }
                                    Log.i("TileScanner", stringBuffer + "\r\n");
                                    handler.sendEmptyMessage(0);
                                }
                                else {
                                    Log.i("TileScanner", "\r\n READing FeliCa FAILED");
                                    handler.sendEmptyMessage(0);
                                }
                            }
                        });

//                        card.write((byte) 1, pServiceList, (byte) 1, pBlockList, new byte[]{0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x18, 0x19, 0x00, 0x11, 0x22, 0x33, 0x44, 0x55}, new FeliCa.onReceiveWriteListener() {
//                            @Override
//                            public void onReceiveWrite(boolean isSuc, byte[] returnBytes) {
//                                msgBuffer.append("" + isSuc + returnBytes);
//                                handler.sendEmptyMessage(0);
//                            }
//                        });
                    }
                }
                else if (cardType == DeviceManager.CARD_TYPE_ULTRALIGHT) { //find Ultralight卡
                    final Ntag21x card  = (Ntag21x) deviceManager.getCard();
                    if (card != null) {
                        Log.i("TileScanner", "find Ultralight card ->UID:" + card.uidToString() + "\r\n");
                        Log.i("TileScanner", "Read tag NDEFText\r\n");
                        handler.sendEmptyMessage(0);

                        card.NdefTextRead(new Ntag21x.onReceiveNdefTextReadListener() {
                            @Override
                            public void onReceiveNdefTextRead(String eer, String returnString) {
                                if (returnString != null) {
                                    Log.i("TileScanner", "read NDEFText successfully：\r\n" + returnString);
                                }
                                if (eer != null) {
                                    Log.i("TileScanner", "reading NDEFText failed：" + eer);
                                }
                                handler.sendEmptyMessage(0);
                                card.close(null);
                            }
                        });
                    }
                }
                else if (cardType == DeviceManager.CARD_TYPE_MIFARE) {
                    final Mifare card = (Mifare)deviceManager.getCard();
                    if (card != null) {
                        Log.i("TileScanner", "Found Mifare card->UID:" + card.uidToString() + "\r\n");
                        Log.i("TileScanner", "Start to verify the first password block\r\n");
                        handler.sendEmptyMessage(0);
                        byte[] key = {(byte) 0xff, (byte) 0xff,(byte) 0xff,(byte) 0xff,(byte) 0xff,(byte) 0xff,};
                        card.authenticate((byte) 1, Mifare.MIFARE_KEY_TYPE_A, key, new Mifare.onReceiveAuthenticateListener() {
                            @Override
                            public void onReceiveAuthenticate(boolean isSuc) {
                                if (!isSuc) {
                                    Log.i("TileScanner", "Verifying password failed\r\n");
                                    handler.sendEmptyMessage(0);
                                }
                                else {
                                    Log.i("TileScanner", "Verify password successfully\r\n");

                                    Log.i("TileScanner", "Charge e-Wallet block 1 1000 Chinese yuan\r\n");
                                    handler.sendEmptyMessage(0);
                                    card.decrementTransfer((byte) 1, (byte) 1, card.getValueBytes(1000), new Mifare.onReceiveDecrementTransferListener() {
                                        @Override
                                        public void onReceiveDecrementTransfer(boolean isSuc) {
                                            if (!isSuc) {
                                                Log.i("TileScanner", "e-Walle is not initialized!\r\n");
                                                handler.sendEmptyMessage(0);
                                                card.close(null);
                                            }
                                            else {
                                                Log.i("TileScanner", "Charge successfully！\r\n");
                                                handler.sendEmptyMessage(0);
                                                card.readValue((byte) 1, new Mifare.onReceiveReadValueListener() {
                                                    @Override
                                                    public void onReceiveReadValue(boolean isSuc, byte address, byte[] valueBytes) {
                                                        if (!isSuc || (valueBytes == null) || (valueBytes.length != 4)) {
                                                            Log.i("TileScanner", "Reading e-Wallet balance failed！\r\n");
                                                            handler.sendEmptyMessage(0);
                                                            card.close(null);
                                                        }
                                                        else {
                                                            int value = card.getValue(valueBytes);
                                                            Log.i("TileScanner", "e-Wallet balance is：" + (value & 0x0ffffffffl) + "\r\n");
                                                            handler.sendEmptyMessage(0);
                                                            card.close(null);
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    });

//                                    //Increase value
//                                    card.incrementTransfer((byte) 1, (byte) 1, card.getValueBytes(1000), new Mifare.onReceiveIncrementTransferListener() {
//                                        @Override
//                                        public void onReceiveIncrementTransfer(boolean isSuc) {
//                                            if (!isSuc) {
//                                                msgBuffer.append("e-Walle is not initialized!\r\n");
//                                                handler.sendEmptyMessage(0);
//                                                card.close(null);
//                                            }
//                                            else {
//                                                msgBuffer.append("Charge successfully！\r\n");
//                                                handler.sendEmptyMessage(0);
//                                                card.readValue((byte) 1, new Mifare.onReceiveReadValueListener() {
//                                                    @Override
//                                                    public void onReceiveReadValue(boolean isSuc, byte address, byte[] valueBytes) {
//                                                        if (!isSuc || (valueBytes == null) || (valueBytes.length != 4)) {
//                                                            msgBuffer.append("Reading e-Wallet balance failed！\r\n");
//                                                            handler.sendEmptyMessage(0);
//                                                            card.close(null);
//                                                        }
//                                                        else {
//                                                            int value = card.getValue(valueBytes);
//                                                            msgBuffer.append("e-Wallet balance is：" + (value & 0x0ffffffffl) + "\r\n");
//                                                            handler.sendEmptyMessage(0);
//                                                            card.close(null);
//                                                        }
//                                                    }
//                                                });
//                                            }
//                                        }
//                                    });

//                                    //Test read and write block
//                                    msgBuffer.append("write 00112233445566778899001122334455 to block 1\r\n");
//                                    handler.sendEmptyMessage(0);
//                                    card.write((byte) 1, new byte[]{0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99, 0x00, 0x11, 0x22, 0x33, 0x44, 0x55}, new Mifare.onReceiveWriteListener() {
//                                        @Override
//                                        public void onReceiveWrite(boolean isSuc) {
//                                            if (isSuc) {
//                                                msgBuffer.append("Write successfully！\r\n");
//                                                msgBuffer.append("read data from block 1\r\n");
//                                                handler.sendEmptyMessage(0);
//                                                card.read((byte) 1, new Mifare.onReceiveReadListener() {
//                                                    @Override
//                                                    public void onReceiveRead(boolean isSuc, byte[] returnBytes) {
//                                                        if (!isSuc) {
//                                                            msgBuffer.append("reading data from block 1 failed！\r\n");
//                                                            handler.sendEmptyMessage(0);
//                                                        }
//                                                        else {
//                                                            StringBuffer stringBuffer = new StringBuffer();
//                                                            for (int i=0; i<returnBytes.length; i++) {
//                                                                stringBuffer.append(String.format("%02x", returnBytes[i]));
//                                                            }
//                                                            msgBuffer.append("Block 1 data:\r\n" + stringBuffer);
//                                                            handler.sendEmptyMessage(0);
//                                                        }
//                                                        card.close(null);
//                                                    }
//                                                });
//                                            }
//                                            else {
//                                                msgBuffer.append("Write fails！\r\n");
//                                                handler.sendEmptyMessage(0);
//                                            }
//                                        }
//                                    });
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            getMsgFlag = true;
            SimpleDateFormat formatter = new SimpleDateFormat ("yyyy MM dd HH:mm:ss ");
            Date curDate = new Date(System.currentTimeMillis());//Get current time
            String str = formatter.format(curDate);

            if (deviceManager.isConnection()) {
                Log.i("TileScanner", "Ble is connected");
            }
            else {
                Log.i("TileScanner", "Search device");
            }

            if (msg.what == 1) {
                dialog.show();
            }
            else if (msg.what == 2) {
                dialog.dismiss();
            }
            else if (msg.what == 3) {
                handler.sendEmptyMessage(4);
//                deviceManager.requestVersionsDevice(new DeviceManager.onReceiveVersionsDeviceListener() {
//                    @Override
//                    public void onReceiveVersionsDevice(byte versions) {
//                        msgBuffer.append("Device version:" + String.format("%02x", versions) + "\r\n");
//                        handler.sendEmptyMessage(0);
//                        deviceManager.requestBatteryVoltageDevice(new DeviceManager.onReceiveBatteryVoltageDeviceListener() {
//                            @Override
//                            public void onReceiveBatteryVoltageDevice(double voltage) {
//                                msgBuffer.append("Device battery voltage:" + String.format("%.2f", voltage) + "\r\n");
//                                if (voltage < 3.4) {
//                                    msgBuffer.append("Device has low battery, please charge！");
//                                }
//                                else {
//                                    msgBuffer.append("Device has enough battery！");
//                                }
//                                handler.sendEmptyMessage(4);
//                            }
//                        });
//                    }
//                });
            }
            else if (msg.what == 4) {
                if (deviceManager.isConnection()) {
                    getMsgFlag = false;

                    Log.i("TileScanner", "Stuff is happening");

                    scheduleCallForUID();

                    /*readCardDemo();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (getMsgFlag == false) {
                                handler.sendEmptyMessage(4);


                            }
                        }
                    }).start();*/
                }
            }
            else if (msg.what == 5) {
                disconnectCnt++;
                //searchButton.performClick();
                if(!stopAllScans)
                {
                    connectToTileScanner();
                }
            }
        }
    };

    //Recursive Method that schedules a call to the TileScanner to read the card currently on the scanner and return the UID. Then the method calls itself, creating a periodic call to the TileScanner.
    //  ceases calling itself if a UID has already been received and then calls scheduleRestartOfCallForUID().
    private void scheduleCallForUID()
    {
        try
        {
            Log.i("TileScanner", " scheduling the next cycle of the call for uid loop");
            tileReaderTimer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    if (!uidIsFound)
                    {
                        Log.i("TileScanner", " running the next cycle of the call for uid loop");
                        readCardDemo();
                        scheduleCallForUID();
                    }
                    else
                    {
                        scheduleRestartOfCallForUID();
                    }
                }
            }, 2000);
        }
        catch (IllegalStateException e)
        {
            Log.e("TileScanner", "Timer has been canceled, aborting the call for uid loop");
        }
    }

    //Schedules a task to restart the recursive method scheduleCallForUID (and thus the periodic calling to the TileScanner to read the card) after a short delay.
    private void scheduleRestartOfCallForUID()
    {
        Log.i("TileScanner", " scheduling the restart of the call for uid loop");
        tileReaderTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                if(uidIsFound)
                {
                    uidIsFound = false;
                    hasSufferedAtLeastOneFailureToReadUID = false; //is used to check if the previously read card was left on the scanner, preventing false positive readings.
                    // If the card was removed then the scanner will report a failure to read.
                    Log.i("TileScanner", " restarting the call for uid loop");
                    scheduleCallForUID();
                }
            }
        }, 3000);
    }

//+++[/TileScanner Code]



    //**********[Location Update and server pinging Code]
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // An unresolvable error has occurred and a connection to Google APIs
        // could not be established. Display an error message, or handle
        // the failure silently

        // ...
    }

    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            locationReceivedFromLocationUpdates = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, request, this);
            if(locationReceivedFromLocationUpdates != null)
            {
                //YES, lat and long are multi digit.
                if(Geocoder.isPresent())
                {

                }
                else
                {
                    Log.e("ERROR:", "Geocoder is not avaiable");
                }
            }
            else
            {

            }


        }



    }

    @Override
    public void onConnectionSuspended(int i)
    {
        //put other stuff here
    }

    //update app based on the new location data, and then begin pinging servlet with the new location
    @Override
    public void onLocationChanged(Location location)
    {


    }


    @Override
    public void onSaveInstanceState(Bundle savedState)
    {
        savedState.putParcelable(SAVED_LOCATION_KEY, locationReceivedFromLocationUpdates);
        super.onSaveInstanceState(savedState);
    }

    private void restoreSavedValues(Bundle savedInstanceState)
    {
        if (savedInstanceState != null)
        {

            // Update the value of mCurrentLocation from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(SAVED_LOCATION_KEY))
            {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that
                // mCurrentLocationis not null.
                locationReceivedFromLocationUpdates = savedInstanceState.getParcelable(SAVED_LOCATION_KEY);
            }

        }
    }


    //Update activity based on the results sent back by the servlet.
    @Override
    public void updateFromDownload(String result) {
        //intervalTextView.setText("Interval: " + result);

        if(result != null)
        {
            // matches uses //( as matches() takes a regular expression, where ( is a special character.
            if(!result.matches("failed to connect to /192.168.1.188 \\(port 8080\\) after 3000ms: isConnected failed: ECONNREFUSED \\(Connection refused\\)"))
            {
                Log.e("Download", result);
                try
                {
                    JSONArray jsonResultFromServer = new JSONArray(result);

                    Log.i("Network UPDATE", "Non null result received.");
                    //mapText.setText("We're good");
                    if (pingingServerFor_alertData)
                    {
                        pingingServerFor_alertData = false;
                        alertDataText.setText(result);

                        currentTagName = jsonResultFromServer.getJSONObject(0).getString("name");
                        if(currentTagName.matches("No Name Found"))
                        {
                            currentTagName = currentUID;
                        }

                        switch (jsonResultFromServer.getJSONObject(1).getString("type"))
                        {
                            case "Security Guard": currentTagType = ID_TYPE_Security; break;
                            case "Janitor": currentTagType = ID_TYPE_Janitor; break;
                            default: currentTagType = 0; break;
                        }

                        ArrayList<String> results = new ArrayList<String>();
                        for (int i = 2; i < jsonResultFromServer.length(); i++)
                        {
                            results.add(jsonResultFromServer.getJSONObject(i).getString("alert"));
                        }

                        //mapText.setText(result);
                        speakAlerts(results);
                    }
                    else
                    {
                        if (itemID == 0 && !result.matches(""))//if app has no assigned id, receive id from servlet.
                        {
                            try
                            {
                                JSONArray jin = new JSONArray(result);
                                JSONObject obj = jin.getJSONObject(0);
                                itemID = obj.getInt("id");
                            } catch (JSONException e)
                            {
                                Log.e("JSON ERROR", "Error retrieving id from servlet with exception: " + e.toString());
                            }
                        }
                    }
                }
                catch (JSONException e)
                {
                    Log.e("Network Update", "ERROR in Json: " + e.toString());
                }

            }
            else
            {
                mapText.setText("Error: network unavaiable");
                Log.e("Network UPDATE", "Error: network unavaiable, error: " + result);
                speakNetworkError();
            }
        }
        else
        {
            mapText.setText("Error: network unavaiable");
            Log.e("Network UPDATE", "Error: network unavaiable");
            speakNetworkError();
        }

        Log.e("Download Output", "" + result);
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
        pingingServer = false;
        Log.i("Network Update", "finished Downloading");
        if (aNetworkFragment != null) {
            Log.e("Network Update", "network fragment found, canceling download");
            aNetworkFragment.cancelDownload();
        }
    }

    class AddressResultReceiver extends ResultReceiver
    {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string
            // or an error message sent from the intent service.
            resultData.getString(Constants.RESULT_DATA_KEY);


            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT)
            {
                Log.i("Success", "Address found");
            }
            else
            {
                Log.e("Network Error:", "in OnReceiveResult in AddressResultReceiver: " +  resultData.getString(Constants.RESULT_DATA_KEY));
            }

        }
    }
//**********[/Location Update and server pinging Code]
}


/*





  */
