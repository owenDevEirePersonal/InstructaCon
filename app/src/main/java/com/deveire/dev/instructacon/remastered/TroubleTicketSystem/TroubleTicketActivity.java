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
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
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
import com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents.PingingFor_Scripted1;
import com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents.PingingFor_Scripted2;
import com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents.PingingFor_Scripted3;
import com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents.PingingFor_ScriptedA1;
import com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents.PingingFor_ScriptedB1;
import com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents.PingingFor_ScriptedC1;
import com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents.PingingFor_TroublerStart;
import com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents.PingingFor_YourOwnTask;

import java.io.Flushable;
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

    PowerManager pm;
    PowerManager.WakeLock wl;

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

    private PingingFor_YesNo pingingFor_isAFishYesNo;

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


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trouble_ticket);

        debugText = (TextView) findViewById(R.id.debugText);
        outputText = (TextView) findViewById(R.id.outputText);

        coverImage = (ImageView) findViewById(R.id.coverImage);

        imageTimer = new Timer();

        setupSpeechRecognition();
        recogTimeoutTimer = new Timer();
        setupTextToSpeech();

        pingingFor_isAFishYesNo = new PingingFor_YesNo();
        pingingFor_isAFishYesNo.setSpeechPrompt("Is that a fish");

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
        wl.acquire();

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

    private void swipeActionHandler(String id)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                //hideImage();
                debugText.setText("Card Swiped, begining");
                Log.i("TTDemo", debugText.getText().toString());
                //startDialog(pingingFor_isAFishYesNo);
                showImage(R.drawable.menu);
                startDialog(new PingingFor_TroublerStart());
            }
        });

        //start Dialog here
    }

    private void showImage(int resID)
    {
        coverImage.setImageResource(resID);
        coverImage.setVisibility(View.VISIBLE);

        imageTimer.cancel();
        imageTimer.purge();
    }

    private void showImage(int resID, int timeInMicroseconds)
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
        }, timeInMicroseconds);
    }

    private void showImage(int resID, int timeInMicroseconds, int resID2)
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
        }, timeInMicroseconds);
    }

    private void hideImage()
    {
        coverImage.setVisibility(View.INVISIBLE);
        imageTimer.cancel();
        imageTimer.purge();
    }

//+++++++++++++++++++++++++++++++Troubler Code++++++++++++++++++++++++++++++++++++++
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    private void setupTroubler()
    {
        deviceLocationName = "Men's Bathroom";

        allTroubleTasks = new ArrayList<TroubleTask>();
        potentialTroubleTasks = new ArrayList<TroubleTask>();
        eliminatedTroubleTasks = new ArrayList<TroubleTask>();

        allKnownKeywords = new ArrayList<TroubleKeyword>();
        usedKeywords = new ArrayList<TroubleKeyword>();

        currentFinalistIndex = 0;

        currentMatchesOneOfKeywords = new PingingFor_MatchesOneOfKeywords();

        ArrayList<TroubleKeyword> newTaskTags = new ArrayList<TroubleKeyword>();
        /*newTaskTags.add(new TroubleKeyword("Milk", new String[]{"Milk", "Cow Liquid", "Cream"}, "Does your meal use any milk?"));
        newTaskTags.add(new TroubleKeyword("Eggs", new String[]{"Eggs", "egg", "eggwhites"}, "Does your meal use eggs?"));
        newTaskTags.add(new TroubleKeyword("Bacon", new String[]{"Bacon", "rashers", "pork"}, "Does your meal use bacon or other pork products?"));
        allTroubleTasks.add(new TroubleTask("Cook 1 serving of Meal type A", "Do you wish to order Meal Type A?", newTaskTags, "1 Frying Pan, 3 eggs, 4 strips of bacon and a half litre of milk"));
        addToKnownKeywords(new TroubleTask("Cook 1 serving of Meal type A", "Do you wish to order Meal Type A?", newTaskTags, "1 Frying Pan, 3 eggs, 4 strips of bacon and a half litre of milk"));

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(new TroubleKeyword("Milk", new String[]{"Milk", "Cow Liquid", "Cream"}, "Does you meal use any milk?"));
        newTaskTags.add(new TroubleKeyword("Eggs", new String[]{"Eggs", "egg", "eggwhites"}, "Does your meal use eggs?"));
        newTaskTags.add(new TroubleKeyword("Pancakes", new String[]{"Pancakes", "flapjacks", "waffles"}, "Does your meal include pancakes, panacakes or whatever you call them?"));
        allTroubleTasks.add(new TroubleTask("Cook 1 serving of Meal type B", "Do you wish to order Meal Type B?", newTaskTags, "1 Frying Pan, 3 eggs, 1 jug of pancake mix and a half litre of milk"));
        addToKnownKeywords(new TroubleTask("Cook 1 serving of Meal type B", "Do you wish to order Meal Type B?", newTaskTags, "1 Frying Pan, 3 eggs, 1 jug of pancake mix and a half litre of milk"));

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(new TroubleKeyword("fish", new String[]{"fish", "cod", "sea meat"}, "Does your meal use some kind of fish?"));
        newTaskTags.add(new TroubleKeyword("Eggs", new String[]{"Eggs", "egg", "eggwhites"}, "Does your meal use eggs?"));
        newTaskTags.add(new TroubleKeyword("Beans", new String[]{"beans"}, "Does your meal include beans, baked or otherwise?"));
        allTroubleTasks.add(new TroubleTask("Cook 1 serving of Meal type C", "Do you wish to order Meal Type C?", newTaskTags, "1 Frying Pan, 3 eggs, 1 codfish and half a tin of beans"));
        addToKnownKeywords(new TroubleTask("Cook 1 serving of Meal type C", "Do you wish to order Meal Type C?", newTaskTags, "1 Frying Pan, 3 eggs, 1 codfish and half a tin of beans"));

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(new TroubleKeyword("fish", new String[]{"fish", "cod", "sea meat"}, "Does your meal use some kind of fish?"));
        newTaskTags.add(new TroubleKeyword("Bacon", new String[]{"Bacon", "rashers", "pork"}, "Does your meal use bacon or other pork products?"));
        allTroubleTasks.add(new TroubleTask("Cook 1 serving of Meal type D", "Do you wish to order Meal Type D?", newTaskTags, "1 Frying Pan, 1 codfish and 5 strips of bacon"));
        addToKnownKeywords(new TroubleTask("Cook 1 serving of Meal type D", "Do you wish to order Meal Type D?", newTaskTags, "1 Frying Pan, 1 codfish and 5 strips of bacon"));

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(new TroubleKeyword("Beans", new String[]{"beans"}, "Does your meal include beans, baked or otherwise?"));
        newTaskTags.add(new TroubleKeyword("Cheese", new String[]{"Cheese"}, "Does your meal include cheese or other fermented non-meat, non-manure bovine product?"));
        allTroubleTasks.add(new TroubleTask("Cook 1 serving of Meal type E", "Do you wish to order Meal Type E?", newTaskTags, "1 Pot, 47 Cheese wheels, 1 tin of beans, 5 salt pile and Alchemy Skill of 25 or greater"));
        addToKnownKeywords(new TroubleTask("Cook 1 serving of Meal type E", "Do you wish to order Meal Type E?", newTaskTags, "1 Pot, 47 Cheese wheels, 1 tin of beans, 5 salt pile and Alchemy Skill of 25 or greater"));

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(new TroubleKeyword("Milk", new String[]{"Milk", "Cow Liquid", "Cream"}, "Does you meal use any milk?"));
        newTaskTags.add(new TroubleKeyword("Eggs", new String[]{"Eggs", "egg", "eggwhites"}, "Does your meal use eggs?"));
        newTaskTags.add(new TroubleKeyword("Cheese", new String[]{"Cheese"}, "Does your meal include cheese or other fermented non-meat, non-manure bovine product?"));
        allTroubleTasks.add(new TroubleTask("Cook 1 serving of Meal type F", "Do you wish to order Meal Type F?", newTaskTags, "1 Pot, 49 and a half Cheese wheels, 2 eggs, 5 buckets of milk and knowledge of the Whirlwind Sprint Shout"));
        addToKnownKeywords(new TroubleTask("Cook 1 serving of Meal type F", "Do you wish to order Meal Type F?", newTaskTags, "1 Pot, 49 and a half Cheese wheels, 2 eggs, 5 buckets of milk and knowledge of the Whirlwind Sprint Shout"));

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(new TroubleKeyword("Beans", new String[]{"beans"}, "Does your meal include beans, baked or otherwise?"));
        newTaskTags.add(new TroubleKeyword("Eggs", new String[]{"Eggs", "egg", "eggwhites"}, "Does your meal use eggs?"));
        newTaskTags.add(new TroubleKeyword("fish", new String[]{"fish", "cod", "sea meat"}, "Does your meal use some kind of fish?"));
        allTroubleTasks.add(new TroubleTask("Cook 1 serving of Meal type G", "Do you wish to order Meal Type G?", newTaskTags, "1 Pot, 1 Herring, red for preferance, 8 eggs and a large tin of beans"));
        addToKnownKeywords(new TroubleTask("Cook 1 serving of Meal type G", "Do you wish to order Meal Type G?", newTaskTags, "1 Pot, 1 Herring, red for preferance, 8 eggs and a large tin of beans"));
        */

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(new TroubleKeyword("Sink", new String[]{"Sink"}, "Is the problem with the Sink?"));
        newTaskTags.add(new TroubleKeyword("Bathroom", new String[]{"Bathroom", "Restroom"}, "Is the problem in a bathroom?"));
        allTroubleTasks.add(new TroubleTask("Fix leaking Sink in Gents Washroom", "Is the issue related to a leak of water from the Sink in the west wing Men's Washroom?", newTaskTags, "1 Plumber, some sealant"));
        addToKnownKeywords(new TroubleTask("Fix leaking Sink in Gents Washroom", "Is the issue related to a leak of water from the Sink in the west wing Men's Washroom?", newTaskTags, "1 Plumber, some sealant"));

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(new TroubleKeyword("Hvac", new String[]{"Hvac", "H vac", "ceiling vent"}, "Is the problem with the H Vac?"));
        newTaskTags.add(new TroubleKeyword("Bathroom", new String[]{"Bathroom", "Restroom"}, "Is the problem in a bathroom?"));
        allTroubleTasks.add(new TroubleTask("Fix leaking H Vac in Gents Washroom", "Is the issue related to a leak of water from the H Vac unit in the ceiling of the west wing Men's Washroom?", newTaskTags, "1 Plumber, some sealant"));
        addToKnownKeywords(new TroubleTask("Fix leaking H Vac in Gents Washroom", "Is the issue related to a leak of water from the H Vac unit in the ceiling of the west wing Men's Washroom?", newTaskTags, "1 Plumber, some sealant"));

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(new TroubleKeyword("Urinal", new String[]{"Urinal"}, "Is the problem with a Urinal?"));
        newTaskTags.add(new TroubleKeyword("Bathroom", new String[]{"Bathroom", "Restroom"}, "Is the problem in a bathroom?"));
        allTroubleTasks.add(new TroubleTask("Fix leaking Urinal", "Is the issue related to a leak of water from a Urinal in the west wing Men's Washroom?", newTaskTags, "1 Plumber, some sealant"));
        addToKnownKeywords(new TroubleTask("Fix leaking Urinal", "Is the issue related to a leak of water from a Urinal in the west wing Men's Washroom?", newTaskTags, "1 Plumber, some sealant"));

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(new TroubleKeyword("Ceiling", new String[]{"Ceiling"}, "Is the problem with the Ceiling?"));
        newTaskTags.add(new TroubleKeyword("Bathroom", new String[]{"Bathroom", "Restroom"}, "Is the problem in a bathroom?"));
        allTroubleTasks.add(new TroubleTask("Fix leaking ceiling", "Is the issue related to a leak of water coming from ceiling of the west wing Men's Washroom?", newTaskTags, "1 Plumber, some sealant"));
        addToKnownKeywords(new TroubleTask("Fix leaking ceiling", "Is the issue related to a leak of water coming from ceiling of the west wing Men's Washroom?", newTaskTags, "1 Plumber, some sealant"));

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(new TroubleKeyword("Toilet", new String[]{"Toilet"}, "Is the problem with a Toilet?"));
        newTaskTags.add(new TroubleKeyword("Bathroom", new String[]{"Bathroom", "Restroom"}, "Is the problem in a bathroom?"));
        allTroubleTasks.add(new TroubleTask("Fix leaking Toilet", "Is the issue related to a leak of water from a Toilet in the west wing Men's Washroom?", newTaskTags, "1 Plumber, some sealant"));
        addToKnownKeywords(new TroubleTask("Fix leaking Toilet", "Is the issue related to a leak of water from a Toilet in the west wing Men's Washroom?", newTaskTags, "1 Plumber, some sealant"));

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(new TroubleKeyword("WHB", new String[]{"WHB"}, "Is the problem with the WHB Unit?"));
        newTaskTags.add(new TroubleKeyword("Bathroom", new String[]{"Bathroom", "Restroom"}, "Is the problem in a bathroom?"));
        allTroubleTasks.add(new TroubleTask("Fix leaking WHB", "Is the issue related to a leak of water from the WHB unit in the west wing Men's Washroom?", newTaskTags, "1 Plumber, some sealant"));
        addToKnownKeywords(new TroubleTask("Fix leaking WHB", "Is the issue related to a leak of water from the WHB unit in the west wing Men's Washroom?", newTaskTags, "1 Plumber, some sealant"));

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(new TroubleKeyword("Fan Assisted Heater", new String[]{"Fan Assisted Heater", "Fan assisted radiator", "heater"}, "Is the problem with the fan assisted heater?"));
        newTaskTags.add(new TroubleKeyword("Bathroom", new String[]{"Bathroom", "Restroom"}, "Is the problem in a bathroom?"));
        allTroubleTasks.add(new TroubleTask("Fix malfunctioning Fan Assisted Heater", "Is the issue related to a leak of water from a fan assisted heater in the west wing Men's Washroom?", newTaskTags, "1 Plumber, some sealant, 1 Electrican, Large amount of spare parts"));
        addToKnownKeywords(new TroubleTask("Fix malfunctioning Fan Assisted Heater", "Is the issue related to a leak of water from a fan assisted heater in the west wing Men's Washroom?", newTaskTags, "1 Plumber, some sealant, 1 Electrican, Large amount of spare parts"));

        newTaskTags = new ArrayList<TroubleKeyword>();
        newTaskTags.add(new TroubleKeyword("Plug", new String[]{"Plug"}, "Is the problem with a plug socket?"));
        allTroubleTasks.add(new TroubleTask("Fix damage electrical socket", "Is an electrical socket broken?", newTaskTags, "1 Electrician"));
        addToKnownKeywords(new TroubleTask("Fix damage electrical socket", "Is an electrical socket broken?", newTaskTags, "1 Electrician"));


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
        for (TroubleKeyword aTag: allKnownKeywords)
        {
            if(result.contains(aTag.getKeyword()))
            {
                updatePotentialTasks(aTag, true);
                tagsFound += " " + aTag.getKeyword();
            }
            else
            {
                for (String aSynomyn: aTag.getSynomyns())
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
        Log.i("TTDemo", "Tags found from initial description: " + tagsFound);
    }

    private void collectTagsFromLocation()
    {
        Log.i("TTDemo", "Collecting tags from location: " + deviceLocationName);
        switch (deviceLocationName)
        {
            case "Men's Bathroom":  updatePotentialTasks(findTagInList("Eggs"), true); break;
            default: break;
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
        /*toSpeech.speak("Creating New Alert: " + potentialTroubleTasks.get(currentFinalistIndex).getDescription() + ". And the requirements are as follows: " + potentialTroubleTasks.get(currentFinalistIndex).getRequirements(), TextToSpeech.QUEUE_FLUSH, null, "Final Alert Creation");
        outputText.setText("Creating New Alert: " + potentialTroubleTasks.get(currentFinalistIndex).getDescription() + " \n\n And the requirements are as follows: " + potentialTroubleTasks.get(currentFinalistIndex).getRequirements());
        showImage(R.drawable.inprogress_ad2, 10000);*/
        startDialog(new PingingFor_Scripted3(potentialTroubleTasks.get(currentFinalistIndex).getPromptQuestion()));
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
        Log.i("Speech", "Starting Dialog with textToSpeech for intent: " + intent.getName());
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
                        Log.i("Speech", utteranceId + " DONE!");
                        if (utteranceId.matches(new PingingFor_Clarification().getName()))
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
                        else if(utteranceId.matches(new PingingFor_Scripted1().getName()))
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
                        else if(utteranceId.matches(new PingingFor_Scripted2().getName()))
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
                        else if(utteranceId.matches(new PingingFor_Scripted3("").getName()))
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
                        else if(utteranceId.matches(new PingingFor_ScriptedA1().getName()))
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
                        else if(utteranceId.matches(new PingingFor_ScriptedB1().getName()))
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
                        else if(utteranceId.matches(new PingingFor_ScriptedC1().getName()))
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
                        else if(utteranceId.matches(pingingFor_isAFishYesNo.getName()))
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
                        else if(utteranceId.matches(new PingingFor_TroublerStart().getName()))
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
                        else if(utteranceId.matches(new PingingFor_IntialDescription().getName()))
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
                        else if(utteranceId.matches(new PingingFor_YourOwnTask().getName()))
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
                        else if(utteranceId.matches(currentMatchesOneOfKeywords.getName()))
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
                        else if(utteranceId.matches(new PingingFor_MatchesTask(potentialTroubleTasks.get(currentFinalistIndex)).getName()))
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
                        else if(utteranceId.matches(new PingingFor_MatchesKeyword(currentKeyword).getName())) //caution: currentKeyword may be null if task picking has reached finalists stage
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
        Toast.makeText(getApplicationContext(), "End of Speech", Toast.LENGTH_LONG).show();
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
                    debugText.setText("No Acceptable Response Detected aborting.");
                    showImage(R.drawable.menu);
                    toSpeech.speak("No Acceptable Response Detected, aborting.", TextToSpeech.QUEUE_FLUSH, null, "EndError");
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

        if (pingingFor.getName().matches(pingingFor_isAFishYesNo.getName()))
        {
            if(result.matches("Yes"))
            {
                toSpeech.speak("Thats a fish", TextToSpeech.QUEUE_FLUSH, null, "ThatsAFish");
                outputText.setText("Thats a fish");
            }
            else if (result.matches("No"))
            {
                toSpeech.speak("That's no fish, its a crusteacan", TextToSpeech.QUEUE_FLUSH, null, "ThatsNoFish");
                outputText.setText("That's no fish, its a crusteacan");
            }

        }
        else if (pingingFor.getName().matches(new PingingFor_TroublerStart().getName()))
        {
            if(result.matches("Raise Trouble Ticket"))
            {
                outputText.setText("Raising Trouble Ticket");
                potentialTroubleTasks = allTroubleTasks;
                eliminatedTroubleTasks = new ArrayList<TroubleTask>();
                usedKeywords = new ArrayList<TroubleKeyword>();
                startDialog(new PingingFor_IntialDescription());
            }
            else if(result.matches("Directions to room 7"))
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        showImage(R.drawable.meeting_amy_map, 10000, R.drawable.menu);
                        startDialog(new PingingFor_ScriptedB1());
                    }
                });

            }
            else if(result.matches("Directions to room 6"))
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        showImage(R.drawable.meeting_amy_map_2, 10000, R.drawable.menu);
                        startDialog(new PingingFor_ScriptedC1());
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
                        startDialog(new PingingFor_ScriptedA1());
                    }
                });
            }
            else if(result.matches("Help"))
            {
                toSpeech.speak("To use me, swipe again and use one of the following commands. . " +
                                "If there is a problem and you wish to raise a trouble ticket, use the command. Raise Trouble Ticket. . " +
                                "If you want directions to a room, use the command. Give me directions to room 7. For example. . " +
                                "If you want a staff member to contact you, use the command. Call me back. . ", TextToSpeech.QUEUE_FLUSH, null, "HelpSpeech");
                toSpeech.speak("Thank you Dan and have a nice day", TextToSpeech.QUEUE_ADD, null, "EndOfScript");
            }
        }
        else if(pingingFor.getName().matches(new PingingFor_Scripted1().getName()))
        {
            showImage(R.drawable.out_of_order, 10000, R.drawable.menu);
            startDialog(new PingingFor_Scripted2());
        }
        else if(pingingFor.getName().matches(new PingingFor_Scripted2().getName()))
        {
            toSpeech.speak("Thank you Dan and have a nice day", TextToSpeech.QUEUE_FLUSH, null, "EndOfScript");
            showImage(R.drawable.plumber_phone_alert_1);
        }
        else if(pingingFor.getName().matches(new PingingFor_Scripted3("").getName()))
        {
            if(result.matches("Yes"))
            {
                startDialog(new PingingFor_Scripted1());
            }
            else if(result.matches("No"))
            {

            }
        }
        else if(pingingFor.getName().matches(new PingingFor_ScriptedA1().getName()))
        {
            toSpeech.speak("Thank you Dan and have a nice day", TextToSpeech.QUEUE_FLUSH, null, "EndOfScript");
        }
        else if(pingingFor.getName().matches(new PingingFor_ScriptedB1().getName()))
        {
            toSpeech.speak("Thank you Dan and have a nice day", TextToSpeech.QUEUE_FLUSH, null, "EndOfScript");
        }
        else if(pingingFor.getName().matches(new PingingFor_ScriptedC1().getName()))
        {
            toSpeech.speak("Thank you Dan and have a nice day", TextToSpeech.QUEUE_FLUSH, null, "EndOfScript");
        }
        else if (pingingFor.getName().matches(new PingingFor_IntialDescription().getName()))
        {
            outputText.setText("Collecting Tags from Intial Description " + result);

            collectTagsFromString(result);

            collectTagsFromLocation();

            /*currentKeyword = getMostUsefulKeyword();
            if(currentKeyword != null)
            {
                startDialog(new PingingFor_MatchesKeyword(currentKeyword));
            }
            else
            {
                currentFinalistIndex = 0;
                startDialog(new PingingFor_MatchesTask(potentialTroubleTasks.get(currentFinalistIndex)));
            }*/

            //script part
            showImage(R.drawable.elements);
            currentMatchesOneOfKeywords = new PingingFor_MatchesOneOfKeywords(new TroubleKeyword[]{findTagInList("Hvac"), findTagInList("Urinal"), findTagInList("Ceiling"), findTagInList("Toilet"), findTagInList("Sink"), findTagInList("WHB"), findTagInList("Fan Assisted Heater")}, "What is leaking?");
            startDialog(currentMatchesOneOfKeywords);
        }
        else if (pingingFor.getName().matches(new PingingFor_YourOwnTask().getName()))
        {
            outputText.setText("Creating a new alert with the description: " + result);
            toSpeech.speak("Creating a new alert with the description: " + result, TextToSpeech.QUEUE_FLUSH, null, "Own Task Final Alert");
        }
        else if (pingingFor.getName().matches(currentMatchesOneOfKeywords.getName()))
        {
            showImage(R.drawable.menu);
            for(String aPossibleResult: currentMatchesOneOfKeywords.getResponseKeywords())
            {
                if(aPossibleResult.matches(result))
                {
                    updatePotentialTasks(findTagInList(aPossibleResult), true);
                }
                else
                {
                    updatePotentialTasks(findTagInList(aPossibleResult), false);
                }
            }


            if(potentialTroubleTasks.size() == 1)
            {
                createNewAlert();
                //startDialogAfterCurrentDialog(new PingingFor_Scripted1());
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
                    startDialog(new PingingFor_MatchesTask(potentialTroubleTasks.get(currentFinalistIndex)));
                }
            }
        }
        else if(pingingFor.getName().matches(new PingingFor_MatchesTask(potentialTroubleTasks.get(currentFinalistIndex)).getName()))
        {
            if(result.matches("Yes"))
            {
                createNewAlert();
                //startDialogAfterCurrentDialog(new PingingFor_Scripted1());
            }
            else if(result.matches("No"))
            {
                currentFinalistIndex++;
                if(currentFinalistIndex < potentialTroubleTasks.size())
                {
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

            if(potentialTroubleTasks.size() == 1)
            {
                createNewAlert();
                startDialogAfterCurrentDialog(new PingingFor_Scripted1());

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
                    startDialog(new PingingFor_MatchesTask(potentialTroubleTasks.get(currentFinalistIndex)));
                }
            }

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