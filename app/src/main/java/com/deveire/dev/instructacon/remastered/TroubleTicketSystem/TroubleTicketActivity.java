package com.deveire.dev.instructacon.remastered.TroubleTicketSystem;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.PowerManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.deveire.dev.instructacon.R;
import com.deveire.dev.instructacon.remastered.NfcScanner;
import com.deveire.dev.instructacon.remastered.SpeechIntent;
import com.deveire.dev.instructacon.remastered.SpeechIntents.PingingFor_Clarification;
import com.deveire.dev.instructacon.remastered.SpeechIntents.PingingFor_YesNo;
import com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents.PingingFor_IntialDescription;
import com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents.PingingFor_MatchesKeyword;
import com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents.PingingFor_MatchesOneOfKeywords;
import com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents.PingingFor_MatchesTask;
import com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents.PingingFor_CallMeBack;
import com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents.PingingFor_GetDirections_RoomA;
import com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents.PingingFor_GetDirections_RoomB;
import com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents.PingingFor_TroublerStart;
import com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents.PingingFor_YourOwnTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class TroubleTicketActivity extends Activity implements RecognitionListener
{

    private NfcAdapter nfcAdapt;

    private TextView debugText;
    private TextView outputText;

    private ImageView coverImage;

    private SpeechRecognizer recog;
    private Intent recogIntent;
    private SpeechIntent pingingRecogFor;
    private SpeechIntent previousPingingRecogFor;

    private PowerManager pm;
    private PowerManager.WakeLock wl;

    private Timer imageTimer;

    private Timer recogTimeoutTimer;

    //private final PingingForTest pingingForTest = new PingingForTest();

    //[Experimental Recog instantly stopping BugFix Variables]
    private boolean recogIsRunning;
    private Timer recogDefibulatorTimer;
    private TimerTask recogDefibulatorTask; //will check to see if recogIsRunning and if not will destroy and instanciate recog, as recog sometimes kills itself silently
    //requiring a restart. This loop will continually kill and restart recog, preventing it from killing itself off.
    private RecognitionListener recogListener;
    //[/Experimental Recog instantly stopping BugFix Variables]

    //[TextToSpeech Variables]
    private TextToSpeech toSpeech;
    //[/End of TextToSpeech Variables]

    //[Alert Variables]
    private ArrayList<TroubleEmployee> allEmployees;
    private ArrayList<TroubleAlert> allAlerts;
    private int currentAlertReadoutIndex;
    private int numberOfAlertsReadOut;
    //[End of Alert Variables]

    //[Troubler Variables]
    private String deviceLocationName;

    private ArrayList<TroubleTask> allTroubleTasks;
    private ArrayList<TroubleTask> potentialTroubleTasks;
    private ArrayList<TroubleTask> eliminatedTroubleTasks;

    private ArrayList<TroubleKeyword> allKnownKeywords;
    private ArrayList<TroubleKeyword> usedKeywords;
    private TroubleKeyword currentKeyword;

    private int currentFinalistIndex;

    private PingingFor_MatchesOneOfKeywords currentMatchesOneOfKeywords;
    //[End of Troubler Variables]

    private PingingFor_YesNo pingingFor_PlumberIsThereGas;

    private Switch locationSwitch;
    private String lastSwipedID;

    private final String PLUMBER_TAG_UUID = "0x0456b2527d3680";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trouble_ticket);

        debugText = (TextView) findViewById(R.id.debugText);
        outputText = (TextView) findViewById(R.id.outputText);

        locationSwitch = (Switch) findViewById(R.id.locationSwitch);
        locationSwitch.setText("Lab Location");
        locationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if(isChecked)
                {
                    locationSwitch.setText("Lab Location");
                    deviceLocationName = "Lab";
                    Log.i("Loc", "location is: " + deviceLocationName);
                }
                else
                {
                    locationSwitch.setText("Bathroom Location");
                    deviceLocationName = "Bathroom";
                    Log.i("Loc", "location is: " + deviceLocationName);
                }
            }
        });

        pingingFor_PlumberIsThereGas = new PingingFor_YesNo();
        pingingFor_PlumberIsThereGas.setName("pingingFor_PlumberIsThereGas");
        pingingFor_PlumberIsThereGas.setSpeechPrompt("Has the gas sensor gone off?");

        coverImage = (ImageView) findViewById(R.id.coverImage);

        imageTimer = new Timer();

        setupSpeechRecognition();
        recogTimeoutTimer = new Timer();
        setupTextToSpeech();

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
        wl.acquire();

        setupUsers();
        setupAlerts();
        setupTroubler();

        showImage(R.drawable.menu);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

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

        if(!wl.isHeld())
        {
            wl.acquire();
        }
    }

    @Override
    protected void onPause()
    {
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

        //coverImage.setVisibility(View.INVISIBLE);
        imageTimer.cancel();
        imageTimer.purge();

        recogTimeoutTimer.cancel();
        recogTimeoutTimer.purge();

        super.onPause();
    }

    @Override
    protected void onStop()
    {
        toSpeech.stop();
        toSpeech.shutdown();

        //coverImage.setVisibility(View.INVISIBLE);
        imageTimer.cancel();
        imageTimer.purge();

        recogTimeoutTimer.cancel();
        recogTimeoutTimer.purge();

        wl.release();

        super.onStop();
    }

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
        swipeActionHandler(NfcScanner.getTagIDFromIntent(intent));
    }

    private void swipeActionHandler(final String id)
    {

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                //hideImage();
                debugText.setText("Card Swiped, begining");
                Log.i("TTDemo", debugText.getText().toString());
                showImage(R.drawable.menu);

                currentAlertReadoutIndex = 0;
                lastSwipedID = id;
                numberOfAlertsReadOut = 0;
                toSpeech.speak("Fetching Alerts:", TextToSpeech.QUEUE_FLUSH, null, "NewAlerts");
                readOutAlerts(); //uses currentAlertReadoutIndex and lastSwipedID

                /*if(id.matches(PLUMBER_TAG_UUID) && locationSwitch.isChecked())
                {
                    startDialog(pingingFor_PlumberIsThereGas);
                }*/
            }
        });

        //start Dialog here
    }

    //Displays and image(resID) on screen
    private void showImage(int resID)
    {
        coverImage.setImageResource(resID);
        coverImage.setVisibility(View.VISIBLE);

        imageTimer.cancel();
        imageTimer.purge();
    }

    //Displays an image(resID) for timeInMilliseconds then hides the imageview
    private void showImage(int resID, int timeInMilliseconds)
    {
        coverImage.setImageResource(resID);
        coverImage.setVisibility(View.VISIBLE);

        imageTimer.cancel();
        imageTimer.purge();
        imageTimer = new Timer();
        imageTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        coverImage.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }, timeInMilliseconds);
    }

    //Displays an image(resID) for timeInMilliseconds before swapping to another image(resID2)
    private void showImage(int resID, int timeInMilliseconds, int resID2)
    {
        coverImage.setImageResource(resID);
        coverImage.setVisibility(View.VISIBLE);

        final int defaultToRes = resID2;

        imageTimer.cancel();
        imageTimer.purge();
        imageTimer = new Timer();
        imageTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        coverImage.setImageResource(defaultToRes);
                    }
                });
            }
        }, timeInMilliseconds);
    }

    //Hides the image view
    private void hideImage()
    {
        coverImage.setVisibility(View.INVISIBLE);
        imageTimer.cancel();
        imageTimer.purge();
    }

    //returns all alerts with assignedEmployee with card UUID matching id
    private ArrayList<TroubleAlert> getAlertsForID(String id)
    {
        ArrayList<TroubleAlert> matchingAlerts = new ArrayList<TroubleAlert>();
        for (TroubleAlert aAlert: allAlerts)
        {
            if(aAlert.getAssignedEmployee() != null && aAlert.getAssignedEmployee().getTagUUID().matches(id))
            {
                matchingAlerts.add(aAlert);
            }
        }
        return matchingAlerts;
    }

    //Returns employee data with card UUID matching id
    private TroubleEmployee getEmployeeFromID(String id)
    {
        for (TroubleEmployee a: allEmployees)
        {
            if(a.getTagUUID().matches(id))
            {
                return a;
            }
        }
        return new TroubleEmployee();
    }


//+++++++++++++++++++++++++++++++Troubler Code++++++++++++++++++++++++++++++++++++++
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    private void setupTroubler()
    {
        deviceLocationName = "Lab";

        allTroubleTasks = new ArrayList<TroubleTask>();
        potentialTroubleTasks = new ArrayList<TroubleTask>();
        eliminatedTroubleTasks = new ArrayList<TroubleTask>();

        allKnownKeywords = new ArrayList<TroubleKeyword>();
        usedKeywords = new ArrayList<TroubleKeyword>();

        currentFinalistIndex = 0;

        currentMatchesOneOfKeywords = new PingingFor_MatchesOneOfKeywords();

        labDemoSetup();
        bathroomDemoSetup();
    }

    private void labDemoSetup()
    {
        TroubleTask newTask;

        TroubleKeyword Tag_Bathroom = new TroubleKeyword("Bathroom", new String[]{"Bathroom"}, "Is the problem in a Bathroom?");
        TroubleKeyword Tag_Lab = new TroubleKeyword("Lab", new String[]{"Lab", "Laboratory"}, "Is the problem in a Laboratory?");
        //allKnownKeywords.add(Tag_Bathroom);
        //allKnownKeywords.add(Tag_Lab);

        TroubleKeyword Tag_StrangeNoise = new TroubleKeyword("Strange Noise", new String[]{"making noise", "strange noise", "odd noise", "vibrations", "vibrating", "rumbling", "rattling"}, "Is the fixture making a strange noise or emitting unusual vibrations?");
        TroubleKeyword Tag_LiquidLeak = new TroubleKeyword("Liquid Leak", new String[]{"water", "leaking water", "wet", "liquid", "spray", "leak", "puddle", "pool", "puddles", "pools"}, "Is there liquid leaking from the fixture or on the ground near the fixture?");
        TroubleKeyword Tag_GasLeak = new TroubleKeyword("Gas Leak", new String[]{"gas", "gas leak", "leaking gas"}, "Do you smell gas near the machine?");
        TroubleKeyword Tag_BlockedDrain = new TroubleKeyword("Blocked Drain", new String[]{"blocked drain", "drain blockage", "drain is blocked", "drain"}, "Does it have a drain and is it blocked?");
        TroubleKeyword Tag_Smoking = new TroubleKeyword("Smoking", new String[]{"smoking", "smoke", "fumes"}, "Is the fixture smoking?");

        TroubleKeyword Tag_Sink = new TroubleKeyword("Sink", new String[]{"sink", "wash basin", "washing basin", "sinks", "tap", "taps"}, "Is the problem with a sink?");
        TroubleKeyword Tag_CentrifugalSeparator = new TroubleKeyword("Centrifugal Separator", new String[]{"centrifugal separator", "separator"}, "Is the problem with a Centrifugal Separator?");
        TroubleKeyword Tag_Oven = new TroubleKeyword("Oven", new String[]{"oven", "ovens"}, "Is the problem with the Oven?");
        TroubleKeyword Tag_Mixer = new TroubleKeyword("Mixer", new String[]{"mixer", "mixing machine"}, "Is the problem with the mixer?");
        TroubleKeyword Tag_Scales = new TroubleKeyword("Scales", new String[]{"scales", "scale"}, "Is the problem with the scales?");
        TroubleKeyword Tag_FumeHood = new TroubleKeyword("Fume Hood", new String[]{"fume hood", "fumehood", "fume cupboard", "fume containing thing"}, "Is the problem with a Fume hood?");


        ArrayList<TroubleKeyword> newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(Tag_Smoking);
        newTaskTags.add(Tag_GasLeak);
        newTaskTags.add(Tag_Oven);
        newTaskTags.add(Tag_Lab);
        newTask = new TroubleTask("Fix a gas leak in the Ovens. Please vacant the room immediately and avoid touching anything electrical.", "Is the problem that the oven has a gas leak?", newTaskTags, "Spanner,Flat Head Screwdriver and 10 type 5 screws", R.drawable.oven, R.drawable.gasleak);
        allTroubleTasks.add(newTask);
        addToKnownKeywords(newTask);

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(Tag_StrangeNoise);
        newTaskTags.add(Tag_LiquidLeak);
        newTaskTags.add(Tag_Mixer);
        newTaskTags.add(Tag_Lab);
        newTask = new TroubleTask("Fix damaged bearings on the mixer", "Is the problem that the mixer's bearings have been damaged?", newTaskTags, "Pipe Wrench, replacement C-15 mixer bearings and grease", R.drawable.mixer, R.drawable.mixertt);
        allTroubleTasks.add(newTask);
        addToKnownKeywords(newTask);

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(Tag_StrangeNoise);
        newTaskTags.add(Tag_Scales);
        newTaskTags.add(Tag_Lab);
        newTask = new TroubleTask("Fix the broken scales.", "Is the problem that the scales are not working?", newTaskTags, "5 Type 3 Screws, screwdriver, 3 feet of solder, and a soldering iron", R.drawable.scales, R.drawable.scalestt);
        allTroubleTasks.add(newTask);
        addToKnownKeywords(newTask);

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(Tag_Smoking);
        newTaskTags.add(Tag_GasLeak);
        newTaskTags.add(Tag_FumeHood);
        newTaskTags.add(Tag_Lab);
        newTask = new TroubleTask("Seal leaks in the ducts in the Fume Hood.", "Is the problem that there are fumes coming out of the ducts on the Fume Hood?", newTaskTags, "a ladder, 2 tubes of sealant and a Sealant Gun", R.drawable.furne, R.drawable.furnett);
        allTroubleTasks.add(newTask);
        addToKnownKeywords(newTask);

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(Tag_StrangeNoise);
        newTaskTags.add(Tag_GasLeak);
        newTaskTags.add(Tag_CentrifugalSeparator);
        newTaskTags.add(Tag_Lab);
        newTask = new TroubleTask("Fix the Centrifugal Separator", "Is the problem that the Centrifugal Separator is grinding?", newTaskTags, "Full toolbox, Centrifugal Repair manual, replacement part J-23 and knowledge of how to spell Centrifugal", R.drawable.separator, R.drawable.separatortt);
        allTroubleTasks.add(newTask);
        addToKnownKeywords(newTask);

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(Tag_BlockedDrain);
        newTaskTags.add(Tag_LiquidLeak);
        newTaskTags.add(Tag_Sink);
        newTaskTags.add(Tag_Lab);
        newTask = new TroubleTask("Unclog the stainless steel sink's drain.", "Is the problem a blocked drain in the stainless steel sink?", newTaskTags, "Pipe wench, drain cleaner, plunger and a bucket", R.drawable.sink, R.drawable.stainlesssteelsinktt);
        allTroubleTasks.add(newTask);
        addToKnownKeywords(newTask);
    }

    private void bathroomDemoSetup()
    {
        TroubleTask newTask;

        TroubleKeyword Tag_Bathroom = new TroubleKeyword("Bathroom", new String[]{"Bathroom"}, "Is the problem in a Bathroom?");
        TroubleKeyword Tag_Lab = new TroubleKeyword("Lab", new String[]{"Lab", "Laboratory"}, "Is the problem in a Laboratory?");
        //allKnownKeywords.add(Tag_Bathroom);
        //allKnownKeywords.add(Tag_Lab);

        TroubleKeyword Tag_StrangeNoise = new TroubleKeyword("Strange Noise", new String[]{"making noise", "strange noise", "odd noise", "vibrations", "vibrating", "rumbling", "rattling"}, "Is the fixture making a strange noise or emitting unusual vibrations?");
        TroubleKeyword Tag_LiquidLeak = new TroubleKeyword("Liquid Leak", new String[]{"water", "leaking water", "wet", "liquid", "spray", "leak", "puddle", "pool", "puddles", "pools"}, "Is there liquid leaking from the fixture or on the ground near the machine?");
        TroubleKeyword Tag_BlockedDrain = new TroubleKeyword("Blocked Drain", new String[]{"blocked drain", "drain blockage", "drain is blocked", "drain"}, "Does it have a drain and is it blocked?");

        TroubleKeyword Tag_Toilet = new TroubleKeyword("Toilet", new String[]{"toilet", "john", "water closet"},"Is the problem with a toilet?");
        TroubleKeyword Tag_Sink = new TroubleKeyword("Sink", new String[]{"sink", "wash basin", "washing basin", "sinks", "tap", "taps"}, "Is the problem with a sink?");
        TroubleKeyword Tag_Ceiling = new TroubleKeyword("Ceiling", new String[]{"ceiling", "roof"}, "Is the problem with the ceiling?");
        TroubleKeyword Tag_Floor = new TroubleKeyword("Floor", new String[]{"floor", "tiles", "carpet", "ground"}, "Is the problem something to do with the floor?");
        TroubleKeyword Tag_Urinal = new TroubleKeyword("Urinal", new String[]{"urinal"}, "Is the problem with the Urinal?");
        TroubleKeyword Tag_Heater = new TroubleKeyword("Heater", new String[]{"heater", "fan", "radiator"}, "Is the problem something to do with the fan assisted heater?");

        ArrayList<TroubleKeyword> newTaskTags = new ArrayList<TroubleKeyword>();

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(Tag_LiquidLeak);
        newTaskTags.add(Tag_Bathroom);
        newTaskTags.add(Tag_Toilet);
        newTask = new TroubleTask("Fix the toilet's leaking Cistern.", "Is the problem that one of the toilet's cistern is leaking?", newTaskTags, "Pipe wench, sealant, plunger and a bucket", R.drawable.elements_toilet, R.drawable.tt_wc);
        allTroubleTasks.add(newTask);
        addToKnownKeywords(newTask);

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(Tag_BlockedDrain);
        newTaskTags.add(Tag_Bathroom);
        newTaskTags.add(Tag_Toilet);
        newTask = new TroubleTask("Clear the toilet's clog.", "Is the problem that the toilet is clogged?", newTaskTags, "Pipe wench, drain cleaner, plunger and a bucket", R.drawable.elements_toilet, R.drawable.tt_wc);
        allTroubleTasks.add(newTask);
        addToKnownKeywords(newTask);

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(Tag_BlockedDrain);
        newTaskTags.add(Tag_Bathroom);
        newTaskTags.add(Tag_Sink);
        newTask = new TroubleTask("Fix the leaking sink and make sure the sink's drain is unclogged.", "Is the problem that the sink is leaking?", newTaskTags, "Pipe wench, drain cleaner, sealant, plunger and a bucket", R.drawable.elements_sink, R.drawable.tt_sink);
        allTroubleTasks.add(newTask);
        addToKnownKeywords(newTask);

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(Tag_LiquidLeak);
        newTaskTags.add(Tag_Urinal);
        newTaskTags.add(Tag_Bathroom);
        newTask = new TroubleTask("Fix the leaking Urinal Cistern", "Is the problem that one of the urinal's cistern is leaking?", newTaskTags, "Pipe wench, sealant, plunger and a bucket", R.drawable.elements_urinal, R.drawable.tt_urinal);
        allTroubleTasks.add(newTask);
        addToKnownKeywords(newTask);

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(Tag_LiquidLeak);
        newTaskTags.add(Tag_Ceiling);
        newTaskTags.add(Tag_Bathroom);
        newTask = new TroubleTask("Fix the leaking water pipe in the ceiling.", "Is the problem that the ceiling is leaking water?", newTaskTags, "Pipe wench, sealant, plunger and a bucket", R.drawable.elements_ceiling, R.drawable.tt_ceiling);
        allTroubleTasks.add(newTask);
        addToKnownKeywords(newTask);

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(Tag_StrangeNoise);
        newTaskTags.add(Tag_Heater);
        newTaskTags.add(Tag_Bathroom);
        newTask = new TroubleTask("Fix the faulty Fan assisted heater.", "Is the problem that the fan assisted heater is not working?", newTaskTags, "Pipe wench, sealant, replacement fan, electrician to turn off the power", R.drawable.elements_hvac, R.drawable.tt_fan);
        allTroubleTasks.add(newTask);
        addToKnownKeywords(newTask);

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(Tag_LiquidLeak);
        newTaskTags.add(Tag_Floor);
        newTaskTags.add(Tag_Bathroom);
        newTask = new TroubleTask("Clean up water on the floor.", "Is there water covering the floor?", newTaskTags, "A mop and a bucket", R.drawable.elements_floor, R.drawable.tt_water);
        allTroubleTasks.add(newTask);
        addToKnownKeywords(newTask);
    }

    private void setupUsers()
    {
        allEmployees = new ArrayList<TroubleEmployee>();
        allEmployees.add(new TroubleEmployee("Frank Alderman",PLUMBER_TAG_UUID,"Plumber"));
    }

    private void setupAlerts()
    {
        allAlerts = new ArrayList<TroubleAlert>();
    }

    private void addToKnownKeywords(TroubleTask aTask)
    {
        for (TroubleKeyword aKey: aTask.getTags())
        {
            if (!isInArray(allKnownKeywords, aKey))
            {
                allKnownKeywords.add(aKey);
            }
        }
    }

    private TroubleKeyword getMostUsefulKeyword()
    {
        Log.i("TTDemo", "getting Most Useful Keyword");
        Boolean noUsefulKeywordsFound = true;
        TroubleKeyword currentMostUseful = allKnownKeywords.get(0);
        int currentMostUsefulCount = Math.abs(potentialTroubleTasks.size());


        for (TroubleKeyword aKeyword: allKnownKeywords)
        {
            Log.i("TTDemo", "  Testing for past use of: " + aKeyword.getKeyword());
            if(!isInArray(usedKeywords, aKeyword))
            {
                Log.i("TTDemo", "  running heuristic on: " + aKeyword.getKeyword());
                int currentCount = 0;
                for (TroubleTask aTask: potentialTroubleTasks)
                {
                    if(isInArray(aTask.getTags(), aKeyword))
                    {
                        currentCount++;
                    }
                }

                if(Math.abs(currentCount - ((int)(potentialTroubleTasks.size() / 2))) < Math.abs(currentMostUsefulCount - ((int)(potentialTroubleTasks.size() / 2))))
                {
                    currentMostUseful = aKeyword;
                    currentMostUsefulCount = currentCount;

                    //At least 1 keyword was found to not be a tag of at least 1 of the potential tasks, meaning that asking about tags can still eliminate potential tasks.
                    //Log.i("TTDemo", "Unique keyword found, setting to false");
                    noUsefulKeywordsFound = false;
                }
            }
        }

        if(noUsefulKeywordsFound)
        {
            //will trigger an if statement for handling the situtation in which all remaining potential tasks have identical tags and thus asking for tags would be irrelevant
            Log.i("TTDemo", "allKeywords are present, returning null");
            return null;
        }
        else
        {
            Log.i("TTDemo", "returning most useful keyword");
            return currentMostUseful;
        }
    }

    private boolean isInArray(ArrayList<TroubleKeyword> list, TroubleKeyword item)
    {
        for (TroubleKeyword a:list)
        {
            if(a.matches(item))
            {
                return true;
            }
        }
        return false;
    }

    private void updatePotentialTasks(TroubleKeyword tag, boolean isTagCorrect)
    {
        if(tag == null)
        {
            Log.e("TTDemo", tag + " not found");
        }
        else
        {
            if (isTagCorrect)
            {
                outputText.setText("Tag " + tag.getKeyword() + " is correct.");
            }
            else
            {
                outputText.setText("Tag " + tag.getKeyword() + " is incorrect.");
            }


            ArrayList<TroubleTask> newPotentialTroubleTasks = new ArrayList<TroubleTask>();
            for (TroubleTask aTask : potentialTroubleTasks)
            {
                if ((isTagCorrect && isInArray(aTask.getTags(), tag)) || (!isTagCorrect && !isInArray(aTask.getTags(), tag)))
                {
                    newPotentialTroubleTasks.add(aTask);
                }
                else
                {
                    eliminatedTroubleTasks.add(aTask);
                }
            }
            usedKeywords.add(tag);
            potentialTroubleTasks = newPotentialTroubleTasks;
        }
    }

    private void collectTagsFromString(String result)
    {
        String tagsFound = "";
        //tagsFound is for debugging only
        for (TroubleKeyword aTag: allKnownKeywords)
        {
            if(result.contains(aTag.getKeyword()))
            {
                updatePotentialTasks(aTag, true);
                tagsFound += " " + aTag.getKeyword();
            }
            else
            {
                for (String aSynomyn: aTag.getSynonyms())
                {
                    if(result.contains(aSynomyn))
                    {
                        updatePotentialTasks(aTag, true);
                        tagsFound += " " + aTag.getKeyword();
                        break;
                    }
                }
            }
        }
        Log.i("TTDemo", "collectTagsFroString: Tags found from initial description: " + tagsFound);
    }

    private void collectTagsFromLocation()
    {
        Log.i("TTDemo", "Collecting tags from location: " + deviceLocationName);
        if(locationSwitch.isChecked())
        {
            //lab
            updatePotentialTasks(findTagInList("Lab"), true);
            updatePotentialTasks(findTagInList("Bathroom"), false);
        }
        else
        {
            //bathroom
            updatePotentialTasks(findTagInList("Lab"), false);
            updatePotentialTasks(findTagInList("Bathroom"), true);
        }
    }

    private TroubleKeyword findTagInList(String tagName)
    {
        for(TroubleKeyword atag: allKnownKeywords)
        {
            if(atag.getKeyword().matches(tagName))
            {
                return atag;
            }
        }
        return null;
    }

    private void createNewAlert()
    {
        //toSpeech.speak("Creating New Alert: " + potentialTroubleTasks.get(currentFinalistIndex).getDescription() + ". And the requirements are as follows: " + potentialTroubleTasks.get(currentFinalistIndex).getRequirements(), TextToSpeech.QUEUE_FLUSH, null, "Final Alert Creation");
        //outputText.setText("Creating New Alert: " + potentialTroubleTasks.get(currentFinalistIndex).getDescription() + " \n\n And the requirements are as follows: " + potentialTroubleTasks.get(currentFinalistIndex).getRequirements());
        toSpeech.speak("Creating New Alert: " + potentialTroubleTasks.get(currentFinalistIndex).getDescription(), TextToSpeech.QUEUE_FLUSH, null, "Final Alert Creation");
        outputText.setText("Creating New Alert: " + potentialTroubleTasks.get(currentFinalistIndex).getDescription());
        showImage(potentialTroubleTasks.get(currentFinalistIndex).getTroubleTicketImageID());
        TroubleAlert newAlert = new TroubleAlert(potentialTroubleTasks.get(currentFinalistIndex), getEmployeeFromID(lastSwipedID), deviceLocationName);
        newAlert.setAssignedEmployee(getEmployeeFromID(PLUMBER_TAG_UUID));
        Log.i("TTDemo", "Creating new alert with task description:" + newAlert.getTask().getDescription() + " and raised by: " + newAlert.getRaisedBy() + " and at location: " + newAlert.getLocation() + " and assigned to: " + newAlert.getAssignedEmployee().getTagUUID());
        allAlerts.add(newAlert);
    }

    private void resolveUpdatedPotentialTasks()
    {
        if(potentialTroubleTasks.size() == 1)
        {
            Log.e("TTDemo", "Only 1 Potential Task Remains");
            createNewAlert();
        }
        else if (potentialTroubleTasks.size() == 0)
        {
            startDialog(new PingingFor_YourOwnTask());
        }
        else
        {
            currentKeyword = getMostUsefulKeyword();
            if(currentKeyword != null)
            {
                Log.i("TTDemo", "currentKeyword is not null, moving to Task tag elimination");
                //usedKeywords.add(currentKeyword);
                startDialog(new PingingFor_MatchesKeyword(currentKeyword));
            }
            else
            {
                Log.i("TTDemo", "currentKeyword is null, moving to Task finalist elimination");
                currentFinalistIndex = 0;
                Log.i("TTDemo", "currentTask Finalist: " + potentialTroubleTasks.get(currentFinalistIndex).getPromptQuestion());
                showImage(potentialTroubleTasks.get(currentFinalistIndex).getPromptImageID());
                startDialog(new PingingFor_MatchesTask(potentialTroubleTasks.get(currentFinalistIndex)));
            }
        }
    }

    private void readOutAlerts()
    {
        boolean isReadingOutAlertResponse = false;
        ArrayList<TroubleAlert> employeesAlerts = getAlertsForID(lastSwipedID);
        if(employeesAlerts.size() > 0)
        {
            for (;employeesAlerts.size() > currentAlertReadoutIndex;)
            {
                TroubleAlert anAlert = employeesAlerts.get(currentAlertReadoutIndex);
                if(processAlertReadOut(anAlert, lastSwipedID))
                {
                    currentAlertReadoutIndex++;
                    isReadingOutAlertResponse = true;
                    break;
                }
                else
                {
                    currentAlertReadoutIndex++;
                }
            }

            if(employeesAlerts.size() <= currentAlertReadoutIndex && !isReadingOutAlertResponse)
            {
                toSpeech.speak("No further alerts", TextToSpeech.QUEUE_ADD, null, "EndOfAlerts");
                startDialogAfterCurrentDialog(new PingingFor_TroublerStart());
            }
        }
        else
        {
            startDialog(new PingingFor_TroublerStart());
        }
    }

    private boolean processAlertReadOut(TroubleAlert anAlert , String id)
    {

        boolean isDialogResponse = false;
        if(anAlert.getLocation().matches(deviceLocationName))
        {
            numberOfAlertsReadOut++;
            TroubleEmployee anEmployee = getEmployeeFromID(id);
            toSpeech.speak("Alert " + numberOfAlertsReadOut + ". " + anAlert.getTask().getDescription(), TextToSpeech.QUEUE_ADD, null, "alert" + numberOfAlertsReadOut);

            if (anEmployee.getType().matches("Plumber") && anAlert.getTask().getDescription().matches("Fix the faulty Fan assisted heater."))
            {
                toSpeech.speak("Warning: The Electrician has not yet isolated the power supply to the Fan Assisted Heater, do not proceed with repairs.", TextToSpeech.QUEUE_ADD, null, "WarningPowerSupplyNotIsolated");
                isDialogResponse = false;
            }
            else if (anEmployee.getType().matches("Plumber") && anAlert.getTask().getDescription().matches("Seal leaks in the ducts in the Fume Hood."))
            {
                startDialogAfterCurrentDialog(pingingFor_PlumberIsThereGas);
                isDialogResponse = true;
            }
            //======>Put new custom alert responses here<======
        }
        return isDialogResponse;
    }
//+++++++++++++++++++++++++++++++End of Troubler Code+++++++++++++++++++++++++++++++
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++





//+++++++++++++++++++++++++++++++Voice Interface Code+++++++++++++++++++++++++++++++
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    //++++++[Text To Speech Code]
    public void startDialog(SpeechIntent intent)
    {
        pingingRecogFor = intent;
        Log.i("Speech", "Starting Dialog with textToSpeech for intent: " + intent.getName());
        toSpeech.speak(pingingRecogFor.getSpeechPrompt(), TextToSpeech.QUEUE_FLUSH, null, pingingRecogFor.getName());
    }

    public void startDialogAfterCurrentDialog(SpeechIntent intent)
    {
        pingingRecogFor = intent;
        Log.i("Speech", "Starting Dialog after current Dialog with textToSpeech for intent: " + intent.getName());
        toSpeech.speak(pingingRecogFor.getSpeechPrompt(), TextToSpeech.QUEUE_ADD, null, pingingRecogFor.getName());
    }

    private void setupTextToSpeech()
    {
        toSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status)
            {
                Log.i("Text To Speech Update", "onInit Complete");
                toSpeech.setLanguage(Locale.ENGLISH);
                HashMap<String, String> endOfSpeakIndentifier = new HashMap();
                endOfSpeakIndentifier.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "endOfSpeech");
                toSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener()
                {
                    @Override
                    public void onStart(String utteranceId)
                    {
                        Log.i("Speech", "onStart called");
                    }

                    @Override
                    public void onDone(String utteranceId)
                    {
                        try
                        {
                            Log.i("Speech", utteranceId + " DONE!");
                            if (utteranceId.matches(new PingingFor_Clarification().getName())
                                    || utteranceId.matches(new PingingFor_CallMeBack().getName())
                                    || utteranceId.matches(new PingingFor_GetDirections_RoomA().getName())
                                    || utteranceId.matches(new PingingFor_GetDirections_RoomB().getName())
                                    || utteranceId.matches(new PingingFor_TroublerStart().getName())
                                    || utteranceId.matches(new PingingFor_IntialDescription().getName())
                                    || utteranceId.matches(new PingingFor_YourOwnTask().getName())
                                    || utteranceId.matches(currentMatchesOneOfKeywords.getName())
                                    || utteranceId.matches(pingingFor_PlumberIsThereGas.getName())
                                    || utteranceId.matches(new PingingFor_MatchesTask(potentialTroubleTasks.get(currentFinalistIndex)).getName())
                                    || utteranceId.matches(new PingingFor_MatchesKeyword(currentKeyword).getName()))
                            {
                                runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        startRecogListening(pingingRecogFor);
                                    }
                                });
                            }
                            else
                            {
                                Log.e("Speech", "Unrecognised utteranceID");
                            }
                            //TODO: Add calls to startRecogListening for each SpeechIntent
                            //toSpeech.shutdown();
                        }
                        catch (Exception e)
                        {
                            Log.e("Speech", "An exception occured in OnDone in the Utterance listener, while identifying the utteranceID: " + e.toString());
                        }
                    }

                    @Override
                    public void onError(String utteranceId)
                    {
                        Log.i("Speech", "ERROR DETECTED");
                    }
                });
            }
        });
    }
    //++++++[/End of Text To Speech Code]

    //++++++[Recognistion Setup Code]
    private void setupSpeechRecognition()
    {
        recogIsRunning = false;
        recog = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        recogListener = this;
        recog.setRecognitionListener(recogListener);
        recogIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recogIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
        recogIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplicationContext().getPackageName());
        recogIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recogIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        /*recogDefibulatorTimer = new Timer();
        recogDefibulatorTask = new TimerTask()
        {
            @Override
            public void run()
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if(!recogIsRunning)
                        {
                            recog.destroy();
                            recog = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
                            recog.setRecognitionListener(recogListener);
                        }
                    }
                });
            }
        };
        recogDefibulatorTimer.schedule(recogDefibulatorTask, 0, 4000);*/
    }
//++++++[end of Recognition Setup Code]

    //++++++++[Recognition Listener Code]
    @Override
    public void onReadyForSpeech(Bundle bundle)
    {
        Log.e("Recog", "ReadyForSpeech");
        Toast.makeText(getApplicationContext(), "Speak Now", Toast.LENGTH_SHORT).show();
        //recogIsRunning = false;
    }

    @Override
    public void onBeginningOfSpeech()
    {
        Log.e("Recog", "BeginningOfSpeech");
        //recogIsRunning = true;
    }

    @Override
    public void onRmsChanged(float v)
    {
        Log.e("Recog", "onRmsChanged");
    }

    @Override
    public void onBufferReceived(byte[] bytes)
    {
        Log.e("Recog", "onBufferReceived");
    }

    @Override
    public void onEndOfSpeech()
    {
        Log.e("Recog", "End ofSpeech");
        Toast.makeText(getApplicationContext(), "End of Speech", Toast.LENGTH_SHORT).show();
        recog.stopListening();
        recogTimeoutTimer.cancel();
        recogTimeoutTimer.purge();
        recogTimeoutTimer = new Timer();
        recogTimeoutTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        toSpeech.speak("I'm sorry but I'm having trouble connecting to the servers. Please check your wifi connection and try again.", TextToSpeech.QUEUE_FLUSH, null, "ErrRecogConnectionTimeout");
                        recog.cancel();
                    }
                });
            }
        }, 5000);
    }

    @Override
    public void onError(int i)
    {
        recogTimeoutTimer.cancel();
        recogTimeoutTimer.purge();

        switch (i)
        {
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                Log.e("Recog", "SPEECH TIMEOUT ERROR");
                break;
            case SpeechRecognizer.ERROR_SERVER:
                Log.e("Recog", "SERVER ERROR");
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                Log.e("Recog", "BUSY ERROR");
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                Log.e("Recog", "NETWORK TIMEOUT ERROR");
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                Log.e("Recog", "TIMEOUT ERROR");
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                Log.e("Recog", "INSUFFICENT PERMISSIONS ERROR");
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                Log.e("Recog", "CLIENT ERROR");
                break;
            case SpeechRecognizer.ERROR_AUDIO:
                Log.e("Recog", "AUDIO ERROR");
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                Log.e("Recog", "NO MATCH ERROR");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                {
                    //debugText.setText("No Acceptable Response Detected aborting.");
                    //showImage(R.drawable.menu);
                    //toSpeech.speak("No Acceptable Response Detected, aborting.", TextToSpeech.QUEUE_FLUSH, null, "EndError");
                    toSpeech.speak("I'm sorry, I didn't catch that. ", TextToSpeech.QUEUE_FLUSH, null, "StartError");
                    startDialogAfterCurrentDialog(pingingRecogFor);
                }
                break;
            default:
                Log.e("Recog", "UNKNOWN ERROR: " + i);
                break;
        }
    }


    @Override
    public void onResults(Bundle bundle)
    {
        ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        debugText.setText("" + matches.toString());
        recogTimeoutTimer.cancel();
        recogTimeoutTimer.purge();
        handleResults(matches);
    }

    @Override
    public void onPartialResults(Bundle bundle)
    {
        Log.e("Recog", "Partial Result");
    }

    @Override
    public void onEvent(int i, Bundle bundle)
    {
        Log.e("Recog", "onEvent");
    }
//++++++++[end of Recognition Listener Code]

//++++++++[Recognition Other Code]

    //Start listening for a user response to intent
    private void startRecogListening(SpeechIntent intent)
    {
        Log.i("Output", "starting Recog for: " + intent.getName());
        debugText.setText("starting Recog for: " + intent.getName());

        pingingRecogFor = intent;
        recog = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        recogListener = this;
        recog.setRecognitionListener(recogListener);
        recogIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recogIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
        recogIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplicationContext().getPackageName());
        recogIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recogIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        recog.startListening(recogIntent);


    }

    //Start of Recog result handling, if the recog intent was for clarification get the 1st keyword and prepare a response,
    // otherwise send the user response to be clarified.
    private void handleResults(ArrayList<String> matches)
    {
        Log.i("Output", "handleResults with: " + matches.toString());
        debugText.setText("handleResults");

        if (pingingRecogFor.getName().matches(new PingingFor_Clarification().getName())) //this is messy, FIND A BETTER WAY OF GETTING THE NAME FOR PingingFor_Clarification
        {
            if (sortThroughForFirstMatch(matches, pingingRecogFor).matches("-NoMatchFound-"))
            {
                //repeat clarification
                startRecogListening(pingingRecogFor);
            }
            else
            {
                prepareResponseFor(sortThroughForFirstMatch(matches, pingingRecogFor), previousPingingRecogFor);
            }
        }
        else
        {
            if (pingingRecogFor.isFillInIntent())
            {
                fillResponse(matches, pingingRecogFor);
                //prepareResponseFor(matches.get(0), pingingRecogFor);
            }
            else
            {
                filterThroughClarification(matches, pingingRecogFor);
            }
        }
    }

    private void fillResponse(ArrayList<String> results, SpeechIntent pingingFor)
    {
        if (results.size() > 0)
        {
            prepareResponseFor(results.get(0), pingingFor);
        }
        else
        {
            Log.e("Output", "Error, fillResponse got no results");
        }
    }

    //Returns first matching keyword found
    private String sortThroughForFirstMatch(ArrayList<String> results, SpeechIntent pingingFor)
    {
        Log.i("Output", "sortThroughForFirstMatch");
        debugText.setText("sortThroughForFirstMatch");

        for (String aResult : results)
        {
            for (String keyword : pingingFor.getResponseKeywords())
            {
                if (aResult.toLowerCase().contains(keyword.toLowerCase()))
                {
                    return keyword;
                }
                else
                {
                    for (String synonym : pingingFor.getResponseSynonyms(keyword))
                    {
                        if (aResult.toLowerCase().contains(synonym.toLowerCase()))
                        {
                            return keyword;
                        }
                    }
                }
            }
        }
        return "-NoMatchFound-";
    }

    //Check all results for keywords regardless of accuracy, then present all found keys words to clarification methods.
    //More thorough but more likely to ask the user for clarification
    private ArrayList<String> sortAllPossibleResultsForAllMatches(ArrayList<String> results, SpeechIntent pingingFor)
    {
        Log.i("Output", "sortAllPossibleResultsForAllMatches");
        debugText.setText("sortAllPossibleResultsForAllMatches");

        ArrayList<String> foundMatches = new ArrayList<String>();
        for (String aResult : results)
        {
            for (String keyword : pingingFor.getResponseKeywords())
            {
                for (String synonym : pingingFor.getResponseSynonyms(keyword))
                {
                    if (aResult.toLowerCase().contains(synonym.toLowerCase()))
                    {
                        foundMatches.add(keyword);
                        break;
                    }
                }
            }
        }

        ArrayList<String> foundUniqueMatches = new ArrayList<>();
        for (String aMatch : foundMatches)
        {
            boolean isDupe = false;
            for (String aUniqueMatch : foundUniqueMatches)
            {
                if (aMatch.matches(aUniqueMatch))
                {
                    isDupe = true;
                    break;
                }
            }
            if (!isDupe)
            {
                foundUniqueMatches.add(aMatch);
            }
        }
        foundMatches = foundUniqueMatches;

        return foundMatches;
    }

    //Check for matches from most accurate to least accurate, but stop searching as soon as any result produces a match.
    //Less through but less likely to ask the user for unnessary clarification
    //if the most accurate result(1st in the array) contains no key words, swap to the next most accurate result.
    private ArrayList<String> sortForAllMatchesInMostAccurateResult(ArrayList<String> results, SpeechIntent pingingFor)
    {
        Log.i("Output", "sortAllPossibleResultsForAllMatches");
        debugText.setText("sortAllPossibleResultsForAllMatches");

        ArrayList<String> foundMatches = new ArrayList<String>();
        for (String aResult : results)
        {
            for (String keyword : pingingFor.getResponseKeywords())
            {
                for (String synonym : pingingFor.getResponseSynonyms(keyword))
                {
                    if (aResult.toLowerCase().contains(synonym.toLowerCase()))
                    {
                        foundMatches.add(keyword);
                        break;
                    }
                }
            }


            if (foundMatches.size() > 0)
            {
                break;
            }
        }

        return foundMatches;
    }

    //Gets possible keywords said by the user from sortAllPossibleResultsForAllMatches then launches recog for Clarification if more than 1 keyword found,
    // else, passes intent to output handling.
    private void filterThroughClarification(ArrayList<String> results, SpeechIntent pingingFor)
    {
        Log.i("Output", "filterThroughClarification");
        debugText.setText("filterThroughClarification");

        ArrayList<String> possibleKeywords = sortAllPossibleResultsForAllMatches(results, pingingFor);

        if (possibleKeywords.size() > 1)
        {
            Log.i("Output", "about to start pinging for Clarification with: " + possibleKeywords.toString());
            debugText.setText("filterThroughClarification for clarification: Did you mean? " + possibleKeywords.toString());
            previousPingingRecogFor = pingingFor;
            startDialog(new PingingFor_Clarification(possibleKeywords));
        }
        else if (possibleKeywords.size() == 1)
        {
            prepareResponseFor(possibleKeywords.get(0), pingingFor);
        }
        else
        {
            Log.i("Output", "Error prepareingResponses from filterThroughClarification, as possibleKeywords is empty: ");
            debugText.setText("Error prepareingResponses from filterThroughClarification, as possibleKeywords is empty: ");
            previousPingingRecogFor = pingingFor;
            //startDialog(new PingingFor_Clarification(pingingFor.getResponseKeywords()));
            this.onError(SpeechRecognizer.ERROR_NO_MATCH);
        }
    }

    //Call the Output method for the given intent
    private void prepareResponseFor(String result, SpeechIntent pingingFor)
    {
        Log.i("Output", "prepareResponseFor" + pingingFor.getName() + " with result: " + result);
        debugText.setText("prepareResponseFor" + pingingFor.getName() + " with result: " + result);

        if (pingingFor.getName().matches(new PingingFor_TroublerStart().getName()))
        {
            if(result.matches("Raise Trouble Ticket"))
            {
                outputText.setText("Raising Trouble Ticket");
                potentialTroubleTasks = allTroubleTasks;
                eliminatedTroubleTasks = new ArrayList<TroubleTask>();
                usedKeywords = new ArrayList<TroubleKeyword>();
                startDialog(new PingingFor_IntialDescription());
                if(locationSwitch.isChecked())
                {
                    showImage(R.drawable.elements_2);
                }
                else
                {
                    showImage(R.drawable.elements);
                }
            }
            else if(result.matches("Directions to room A"))
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        showImage(R.drawable.meeting_amy_map, 10000, R.drawable.menu);
                        startDialog(new PingingFor_GetDirections_RoomA());
                    }
                });

            }
            else if(result.matches("Directions to room B"))
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        showImage(R.drawable.meeting_amy_map_2, 10000, R.drawable.menu);
                        startDialog(new PingingFor_GetDirections_RoomB());
                    }
                });

            }
            else if(result.matches("Call me back"))
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        startDialog(new PingingFor_CallMeBack());
                    }
                });
            }
            else if(result.matches("Help"))
            {
                toSpeech.speak("To use me, swipe again and use one of the following commands. . " +
                                "If there you wish to test the Troubler Trouble Ticketing system, use the command. Raise Trouble Ticket. . " +
                                "If you want directions to a room, use the command. Give me directions to room A. For example. . " +
                                "If you want a staff member to contact you, use the command. Call me back. . ", TextToSpeech.QUEUE_FLUSH, null, "HelpSpeech");
                toSpeech.speak("Thank you and have a nice day", TextToSpeech.QUEUE_ADD, null, "EndOfScript");
            }
            else if(result.matches("Nevermind"))
            {
                toSpeech.speak("Ok. Have a nice day.", TextToSpeech.QUEUE_FLUSH, null, "NevermindSpeech");
            }
        }
        else if(pingingFor.getName().matches(new PingingFor_CallMeBack().getName()))
        {
            if(result.matches("No"))
            {
                toSpeech.speak("Ok lets start over. Please tap your card to begin.", TextToSpeech.QUEUE_FLUSH, null, "EndOfScript");
            }
            else
            {
                toSpeech.speak("Thank you and have a nice day", TextToSpeech.QUEUE_FLUSH, null, "EndOfScript");
            }
        }
        else if(pingingFor.getName().matches(new PingingFor_GetDirections_RoomA().getName()))
        {
            toSpeech.speak("Thank you and have a nice day", TextToSpeech.QUEUE_FLUSH, null, "EndOfScript");
        }
        else if(pingingFor.getName().matches(new PingingFor_GetDirections_RoomB().getName()))
        {
            toSpeech.speak("Thank you Dan and have a nice day", TextToSpeech.QUEUE_FLUSH, null, "EndOfScript");
        }
        else if(pingingFor.getName().matches(pingingFor_PlumberIsThereGas.getName()))
        {
            if(result.matches("Yes"))
            {
                toSpeech.speak("Danger Do not Enter the Room.", TextToSpeech.QUEUE_FLUSH, null, "DoNotEnter");
                //startDialogAfterCurrentDialog(new PingingFor_TroublerStart());
            }
            else if(result.matches("No"))
            {
                toSpeech.speak("Very good. Please proceed with your issued work order and tag out when your work is completed.", TextToSpeech.QUEUE_FLUSH, null, "DoEnter");
                //startDialogAfterCurrentDialog(new PingingFor_TroublerStart());
            }
            readOutAlerts(); //resumes reading out alerts
        }
        else if (pingingFor.getName().matches(new PingingFor_IntialDescription().getName()))
        {
            Log.i("TTDemo", "PingingFor_IntialDescription: User has provided an Intial Description of the problem as: " + result);
            outputText.setText("Collecting Tags from Intial Description " + result);

            collectTagsFromString(result);

            collectTagsFromLocation();

            currentKeyword = getMostUsefulKeyword();
            if(potentialTroubleTasks.size() == 1)
            {
                currentFinalistIndex = 0;
                showImage(potentialTroubleTasks.get(currentFinalistIndex).getPromptImageID());
                startDialog(new PingingFor_MatchesTask(potentialTroubleTasks.get(currentFinalistIndex)));
            }
            else if(currentKeyword != null)
            {
                startDialog(new PingingFor_MatchesKeyword(currentKeyword));
            }
            else
            {
                startDialog(new PingingFor_YourOwnTask());
            }
        }
        else if (pingingFor.getName().matches(new PingingFor_YourOwnTask().getName()))
        {
            Log.i("TTDemo", "PingingFor_YourOwnTask: User has described the in their own words as: " + result);
            outputText.setText("Creating a new alert with the description: " + result);
            toSpeech.speak("Creating a new alert with the description: " + result, TextToSpeech.QUEUE_FLUSH, null, "Own Task Final Alert");
        }
        else if (pingingFor.getName().matches(currentMatchesOneOfKeywords.getName())) //MatchesOneOfKeywords: this dialog has the user say 1 tag out of a list of tags. Every tag on the list is mutually exclusive(e.g. no task contains more than 1 of these tags)
        {
            showImage(R.drawable.menu);
            Boolean aTagWasChosen = false;
            ArrayList<TroubleKeyword> tagsNotSelected = new ArrayList<TroubleKeyword>();
            for(String aPossibleResult: currentMatchesOneOfKeywords.getResponseKeywords())
            {
                if(aPossibleResult.matches(result))
                {
                    Log.i("TTDemo", "PingingFor_MatchesOneOfKeywords: User has selected tag " + aPossibleResult + " using " + result + " from a list of tags: " + currentMatchesOneOfKeywords.getResponseKeywords().toString());
                    updatePotentialTasks(findTagInList(aPossibleResult), true);
                    aTagWasChosen = true;
                }
                else
                {
                    tagsNotSelected.add(findTagInList(aPossibleResult));
                }
            }

            if(aTagWasChosen)
            {
                for (TroubleKeyword anUnChosenTag: tagsNotSelected)
                {
                    updatePotentialTasks(anUnChosenTag, false);
                }
            }
            //if no tag was chosen from the list then assume the user's response was don't know and mark no tasks as elimanated
            //TODO: consider whether or not to add these keywords as used or unused
            resolveUpdatedPotentialTasks();
        }
        //Put other dialog responses above this line, otherwise you risk a null pointer exception/array out of bounds exception
        //TODO: add error catching for these exceptions
        //========>Put new Dialog Response Handlers here
        // <========
        else if(pingingFor.getName().matches(new PingingFor_MatchesTask(potentialTroubleTasks.get(currentFinalistIndex)).getName())) //Finalist Stage: this dialog is run when all there are no unused tags that could narrow the number of potential tasks.
        {
            if(result.matches("Yes"))
            {
                Log.i("TTDemo", "PingingFor_MatchesTask: User has picked this task as correct: " + potentialTroubleTasks.get(currentFinalistIndex).getDescription());
                createNewAlert();
            }
            else if(result.matches("No"))
            {
                currentFinalistIndex++;
                if(currentFinalistIndex < potentialTroubleTasks.size())
                {
                    showImage(potentialTroubleTasks.get(currentFinalistIndex).getPromptImageID());
                    startDialog(new PingingFor_MatchesTask(potentialTroubleTasks.get(currentFinalistIndex)));
                }
                else
                {
                    startDialog(new PingingFor_YourOwnTask());
                }
            }

        }
        else if(pingingFor.getName().matches(new PingingFor_MatchesKeyword(currentKeyword).getName())) //caution: currentKeyword may be null if task picking has reached finalists stage
        {
            if(result.matches("Yes"))
            {
                updatePotentialTasks(currentKeyword, true);
            }
            else if(result.matches("No"))
            {
                updatePotentialTasks(currentKeyword, false);
            }
            else if(result.matches("Don't know"))
            {
                outputText.setText("Tag " + currentKeyword.getKeyword() + " is ambiguous.");
                usedKeywords.add(currentKeyword);
            }

            resolveUpdatedPotentialTasks();

        }
        else
        {
            Log.e("Response:", "No response setup for this intent: " + pingingFor.getName());
            debugText.setText("No response setup for this intent: " + pingingFor.getName());
        }
    }

//++++++++[/Recognition Other Code]


//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//++++++++++++++++++++++++++End of Voice Interface Code+++++++++++++++++++++++++++++
}


/*


 */