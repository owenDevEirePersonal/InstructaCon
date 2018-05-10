package com.deveire.dev.instructacon.remastered;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.PowerManager;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.deveire.dev.instructacon.AlertsRow;
import com.deveire.dev.instructacon.DownloadCallback;
import com.deveire.dev.instructacon.NetworkFragment;
import com.deveire.dev.instructacon.R;
import com.deveire.dev.instructacon.SignInsRow;
import com.deveire.dev.instructacon.StationActivity;
import com.deveire.dev.instructacon.TagsRow;
import com.deveire.dev.instructacon.bleNfc.DeviceManager;
import com.deveire.dev.instructacon.bleNfc.Scanner;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Timer;

import static com.deveire.dev.instructacon.remastered.Utils.retrieveTags;
import static com.deveire.dev.instructacon.remastered.Utils.saveTagData;

public class Register2Activity extends FragmentActivity
{
    private Button registerButton;
    private Spinner typeSpinner;
    private SpinnerAdapter typeSpinnerAdapter;
    private EditText nameText;
    private EditText tagIDEditText;
    private TextView mapText;

    private Boolean hasState;

    PowerManager pm;
    PowerManager.WakeLock wl;

    //[Offline Variables]
    private SharedPreferences savedData;

    private ArrayList<IDTag> allTags;

    private ArrayList<AlertData> allAlerts;

    private ArrayList<SignInRecord> allSignIns;

    private int signInsCount;
    private int tagsCount;
    private int alertsCount;
    //[/Offline Variables]


    //[Retreive Alert Data Variables]
    private Boolean pingingServerFor_alertData;
    private TextView alertDataText;
    private String currentUID;
    private String currentStationID;
    //[/Retreive Alert Data Variables]

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register2);

        nameText= (EditText) findViewById(R.id.nameEditText);
        tagIDEditText = (EditText) findViewById(R.id.tagIDEditText);


        typeSpinner = (Spinner) findViewById(R.id.typeSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.roles_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_layout);
        // Apply the adapter to the spinner
        typeSpinner.setAdapter(adapter);

        registerButton = (Button) findViewById(R.id.registerButton);
        registerButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                uploadEmployeeData(nameText.getText().toString(), tagIDEditText.getText().toString(), typeSpinner.getSelectedItem().toString());
            }
        });


        //[Offline Setup]
        savedData = this.getApplicationContext().getSharedPreferences("InstructaCon SavedData", Context.MODE_PRIVATE);
        allAlerts = new ArrayList<AlertData>();
        allSignIns = new ArrayList<SignInRecord>();
        allTags = new ArrayList<IDTag>();

        alertsCount = 0;
        signInsCount = 0;
        tagsCount = 0;
        //[/Offline Setup]

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "activity_register_instructacon tag");
        wl.acquire();
    }


    @Override
    protected void onResume()
    {
        super.onResume();
        hasState = true;

        if(!wl.isHeld())
        {
            wl.acquire();
        }
    }

    @Override
    protected void onPause()
    {
        Log.e("TileScanner", "onStop");
        hasState = false;

        super.onPause();
        //finish();
    }

    @Override
    protected void onStop()
    {
        Log.e("TileScanner", "onStop");
        hasState = false;
        wl.release();



        /*
        if(btGatt != null)
        {
            btGatt.disconnect();
            btGatt.close();
        }
        */

        super.onStop();
    }


    private void uploadEmployeeData(String namein, String tagIDin, String typeIn)
    {
        allTags = retrieveTags(savedData);
        if(!namein.matches("") && !tagIDin.matches("-Please Enter Tag-"))
        {
            boolean matchFound = false;
            for (IDTag arow: allTags)
            {
                if (arow.getTagID().matches(tagIDin))
                {
                    arow.setName(namein);
                    arow.setType(getTypeFromSpinner(typeIn));
                    Log.i("Network Update", "Changing tag, with " + namein + ", " + tagIDin + ", " + typeIn);
                    matchFound = true;
                    break;
                }
            }
            if(!matchFound)
            {
                allTags.add(new IDTag(namein, tagIDin, getTypeFromSpinner(typeIn)));
                Log.i("Network Update", "Adding new tag, with " + namein + ", " + tagIDin + ", " + typeIn);
            }
            saveTagData(savedData, allTags);
            finish();
        }
        else
        {
            Log.e("Network Update", "Error in uploadEmployeeData, invalid uuid entered, or no name entered");
        }
    }

    private String getTypeFromSpinner(String inSpinnerString)
    {
        switch (inSpinnerString)
        {
            case "Janitor": return IDTag.tagtype_JANITOR;
            case "Security Guard": return IDTag.tagtype_SECURITY;
            case "Technician Class 1": return IDTag.tagtype_TECHNICIAN_CLASS_1;
            case "Technician Class 2": return IDTag.tagtype_TECHNICIAN_CLASS_2;
            default: return "SPINNER OPTION DOESN'T MAKE ANY KNOWN TAG TYPE: PLEASE CHECK types_spinner_contents.xml";
        }
    }
}
