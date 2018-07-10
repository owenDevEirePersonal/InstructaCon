package com.deveire.dev.instructacon.remastered;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.os.PowerManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.deveire.dev.instructacon.R;

import java.util.ArrayList;

import static com.deveire.dev.instructacon.remastered.Utils.retrieveTags;
import static com.deveire.dev.instructacon.remastered.Utils.saveTagData;

public class Register2Activity extends FragmentActivity
{
    private Button registerButton;
    private Spinner typeSpinner;
    private SpinnerAdapter typeSpinnerAdapter;
    private EditText nameText;
    private EditText tagIDEditText;

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

    private NfcAdapter nfcAdapt;

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

        nfcAdapt = NfcScanner.setupNfcScanner(this);

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

        nfcAdapt = NfcScanner.setupNfcScanner(this);
        if(nfcAdapt == null)
        {
            Toast.makeText(this, "Please turn on NFC scanner before continuing", Toast.LENGTH_LONG).show();
            finish();
        }
        else
        {
            NfcScanner.setupForegroundDispatch(this, nfcAdapt);
        }
    }

    @Override
    protected void onPause()
    {
        Log.e("TileScanner", "onStop");
        hasState = false;

        nfcAdapt = NfcScanner.setupNfcScanner(this);
        if(nfcAdapt == null)
        {
            Toast.makeText(this, "Please turn on NFC scanner before continuing", Toast.LENGTH_LONG).show();
            finish();
        }
        else
        {
            NfcScanner.setupForegroundDispatch(this, nfcAdapt);
        }

        super.onPause();
        //finish();
    }

    @Override
    protected void onStop()
    {
        Log.e("TileScanner", "onStop");
        hasState = false;
        wl.release();

        super.onStop();
    }

    //[NFC CODE]
    @Override
    protected void onNewIntent(Intent intent) {
        /**
         * This method gets called, when a new Intent gets associated with the current activity instance.
         * Instead of creating a new activity, onNewIntent will be called. For more information have a look
         * at the documentation.
         *
         * In our case this method gets called, when the user attaches a Tag to the device.
         */
        Log.i("NFCTEST", "onNewIntent: " + intent.toString());
        tagIDEditText.setText(NfcScanner.getTagIDFromIntent(intent));
    }
    //[END OF NFC CODE]


    //Takes in a name, tag id and type, which are then used to create a new employee tag which is added to and saved to the list of employees on shared preferences,
    // if a tag does not already exist with that same id. If one does exist then it's detials will be overwritten.
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
            case "Lab Security": return IDTag.tagtype_LAB_SECURITY;
            case "Technician Class 1": return IDTag.tagtype_TECHNICIAN_CLASS_1;
            case "Technician Class 2": return IDTag.tagtype_TECHNICIAN_CLASS_2;
            default: return "SPINNER OPTION DOESN'T MAKE ANY KNOWN TAG TYPE: PLEASE CHECK types_spinner_contents.xml";
        }
    }
}
