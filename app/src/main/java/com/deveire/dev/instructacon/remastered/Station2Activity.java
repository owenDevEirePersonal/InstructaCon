package com.deveire.dev.instructacon.remastered;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.deveire.dev.instructacon.R;
import com.deveire.dev.instructacon.remastered.SpeechIntents.PingingFor_Clarification;
import com.deveire.dev.instructacon.remastered.SpeechIntents.PingingFor_JanitorTroubleTicket1;
import com.deveire.dev.instructacon.remastered.SpeechIntents.PingingFor_JanitorTroubleTicket2;
import com.deveire.dev.instructacon.remastered.SpeechIntents.PingingFor_JanitorTroubleTicketLeak1;
import com.deveire.dev.instructacon.remastered.SpeechIntents.PingingFor_YesNo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class Station2Activity extends Activity implements RecognitionListener
{
    private Button debugButton;

    private SpeechRecognizer recog;
    private Intent recogIntent;
    private SpeechIntent pingingRecogFor;
    private SpeechIntent previousPingingRecogFor;
    private RecognitionListener recogListener;
    private TextToSpeech toSpeech;

    //[Experimental Recog instantly stopping BugFix Variables]
    private boolean recogIsRunning;
    private Timer recogDefibulatorTimer;
    private TimerTask recogDefibulatorTask;
    //will check to see if recogIsRunning and if not will destroy and instanciate recog, as recog sometimes kills itself silently
    //requiring a restart. This loop will continually kill and restart recog, preventing it from killing itself off.


    private EditText debugIDEditText;
    private EditText nameEditText;
    private ImageView adImageView;
    private ImageView inProgressImage;
    private ImageView jobFinishedImage;
    private ImageView leakInfoImage;
    private Button jobFinishedButton;

    final static int PAIR_READER_REQUESTCODE = 9;

    //[Offline Variables]
    private SharedPreferences savedData;

    private ArrayList<IDTag> allTags;

    private ArrayList<AlertData> allAlerts;

    private ArrayList<SignInRecord> allSignIns;

    private int signInsCount;
    private int tagsCount;
    private int alertsCount;
    //[/Offline Variables]

    private IDTag currentTag;

    private String UnrecognisedTroubleTicketText;

    private Timer adSwapTimer;
    private int currentAdIndex;

    private boolean displayingInProgress;
    private Timer stopInProgressTimer;

    private boolean displayingJobFinished;
    private Timer stopJobFinishedTimer;

    private boolean displayingLeakInfo;
    private Timer stopLeakInfoTimer;


    //[Retreive Alert Data Variables]
    private Boolean pingingServerFor_alertData;
    private TextView alertDataText;
    private TextView instructionsDataText;
    private String currentUID;
    private String currentStationID;
    //[/Retreive Alert Data Variables]

    PowerManager pm;
    PowerManager.WakeLock wl;

    private int scriptLine;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station2);

        setupSpeechRecognition();
        setupTextToSpeech();
        //setupTileScanner();


        nameEditText = (EditText) findViewById(R.id.nameEditText);

        adImageView = (ImageView) findViewById(R.id.addImageView);
        adImageView.setImageResource(R.drawable.drinkaware_ad);
        adImageView.setVisibility(View.VISIBLE);

        instructionsDataText = (TextView) findViewById(R.id.kegDataText);
        alertDataText = (TextView) findViewById(R.id.alertDataText);

        currentUID = "";
        currentStationID = "ceo's bathroom";
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
                            case 3: adImageView.setImageResource(R.drawable.stock_ad); currentAdIndex++; break;
                            case 4: adImageView.setImageResource(R.drawable.menu_ad); currentAdIndex = 1; break;
                        }
                    }
                });

            }
        }, 0, 6000);
        //++++[/Ad Swapping Setup]

        currentTag = new IDTag();

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
        wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
        wl.acquire();


        inProgressImage = (ImageView) findViewById(R.id.inProgressImage);
        inProgressImage.setVisibility(View.INVISIBLE);
        stopInProgressTimer = new Timer();

        //[Job Finished correction setup]
        jobFinishedImage = (ImageView) findViewById(R.id.jobFinishedImage);
        jobFinishedImage.setImageResource(R.drawable.jobsfinished2);
        jobFinishedImage.setVisibility(View.INVISIBLE);

        jobFinishedButton = (Button) findViewById(R.id.jobFinishedButton);
        jobFinishedButton.setVisibility(View.INVISIBLE);

        stopJobFinishedTimer = new Timer();
        //[End of Job Finished correction setup]

        scriptLine = 0;

        retrieveData();
        saveData();

        debugButton = (Button) findViewById(R.id.debugButton);
        debugButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Log.i("Offline check", "debugIDeditText = " + debugIDEditText.getText().toString());
                //currentTag = findTagFromID(debugIDEditText.getText().toString());

                //handleJanitorSwipe(new IDTag("Hugh man", "PlaceholderID", IDTag.tagtype_JANITOR));
                //handleSecuritySwipe(new IDTag("Hugh man", "PlaceholderID", IDTag.tagtype_SECURITY));
                //handleTechnicianClass1Swipe(new IDTag("Hugh man", "PlaceholderID", IDTag.tagtype_TECHNICIAN_CLASS_1));
                //handleTechnicianClass2Swipe(currentTag);
                swipeActionHandler(debugIDEditText.getText().toString());
            }
        });

        leakInfoImage = (ImageView) findViewById(R.id.leakInfoImage);
        leakInfoImage.setImageResource(R.drawable.leak);
        leakInfoImage.setVisibility(View.INVISIBLE);

        debugIDEditText = (EditText) findViewById(R.id.debugIDEditText);

        stopLeakInfoTimer = new Timer();
        stopInProgressTimer = new Timer();
        stopJobFinishedTimer = new Timer();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        /*hasState = true;

        final IntentFilter intentFilter = new IntentFilter();

        uidIsFound = false;
        hasSufferedAtLeastOneFailureToReadUID = true;
        tileReaderTimer = new Timer();
        connectToTileScanner();*/

        if(!wl.isHeld())
        {
            wl.acquire();
        }
    }

    @Override
    protected void onPause()
    {
        /*hasState = false;
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
        }*/

        adSwapTimer.cancel();
        adSwapTimer.purge();

        stopInProgressTimer.cancel();
        stopInProgressTimer.purge();

        stopJobFinishedTimer.cancel();
        stopJobFinishedTimer.purge();


        stopLeakInfoTimer.cancel();
        stopLeakInfoTimer.purge();

        super.onPause();
    }

    @Override
    protected void onStop()
    {
        toSpeech.stop();
        toSpeech.shutdown();

        wl.release();

        super.onStop();
    }


    //[Offline loading]
    private void retrieveData()
    {
        alertsCount = savedData.getInt("alertsCount", 0);
        tagsCount = savedData.getInt("tagsCount", 0);
        signInsCount = savedData.getInt("signInsCount", 0);
        Log.i("Offline", "Total number of alerts: " + alertsCount + " tags: " + tagsCount + " signins: " + signInsCount);
        allAlerts = new ArrayList<AlertData>();
        allTags = new ArrayList<IDTag>();
        allSignIns = new ArrayList<SignInRecord>();

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
            allSignIns.add(new SignInRecord(savedData.getString("signIns" + i, "ERROR")));
        }
    }

    private void saveData()
    {
        Log.i("Saving", "Saving Data in saveData()");

        int j = 0;
        for (IDTag aTag: allTags)
        {
            if(aTag.getTagID().matches(currentTag.getTagID()))
            {
                allTags.set(j, currentTag);
                break;
            }
            j++;
        }

        SharedPreferences.Editor edit = savedData.edit();
        alertsCount = allAlerts.size();
        tagsCount = allTags.size();
        signInsCount = allSignIns.size();
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

        for (int i = 0; i < allSignIns.size(); i++)
        {
            edit.putString("signIns" + i, allSignIns.get(i).serializeRecord());
        }

        edit.commit();
        Log.i("Offline Update", "Saved Data: alertCount: " + alertsCount + ", tagscount: " + tagsCount + ", signinscount: " + signInsCount);
        Log.i("Offline Update", "Saved Data: allalerts: " + allAlerts.size() + ", alltags: " + allTags.size() + ", allsignins: " + allSignIns.size());
    }

    private IDTag findTagFromID(String tagIDin)
    {
        Log.i("Offline Update", "finding row from ID:" + tagIDin + ", searching " + allTags.size() + " rows.");
        for (IDTag arow: allTags)
        {
            if(arow.getTagID().matches(tagIDin))
            {
                return arow;
            }
        }
        return null;
    }

    //[End of Offline loading]

    private void speakAlerts(IDTag tag)
    {
        boolean hasNewAlerts = false;
        String alertSpeechString = "You have new alerts: . . . ";
        String alertTextString = "You have new alerts: \n\n";
        for (AlertData anAlert: allAlerts)
        {
            if(anAlert.isActive() && anAlert.getType().matches(tag.getType()))
            {
                hasNewAlerts = true;
                alertSpeechString += anAlert.getAlertText() + " in " + anAlert.getStationID() + ". . .";
                alertTextString += anAlert.getAlertText() + " in " + anAlert.getStationID() + "\n\n";
                if(anAlert.getStationID().matches(currentStationID))
                {
                    anAlert.setActive(false);
                }
            }
        }

        saveData();

        if(!hasNewAlerts)
        {
            alertSpeechString = "You have no new alerts. . . ";
            alertTextString = "You have no new alerts.\n\n";
        }

        alertDataText.setText(alertTextString);
        toSpeech.speak(alertSpeechString, TextToSpeech.QUEUE_FLUSH, null, "AlertsEnd");
    }


    private void swipeActionHandler(String tagIDin)
    {
        retrieveData();


        IDTag tag = findTagFromID(tagIDin);
        if(tag != null)
        {
            currentTag = tag;
        }
        else
        {
            currentTag = new IDTag("Unknown Card with ID: " + tagIDin, tagIDin, IDTag.tagtype_UNDEFINED_TAG);
        }
        Calendar aCal = Calendar.getInstance();
        allSignIns.add(new SignInRecord(currentStationID, currentTag.serializeTag(), aCal.getTime()));
        saveData();

        switch (currentTag.getType())
        {
            case IDTag.tagtype_JANITOR: handleJanitorSwipe(currentTag); break;
            case IDTag.tagtype_SECURITY: handleSecuritySwipe(currentTag); break;
            case IDTag.tagtype_TECHNICIAN_CLASS_1: handleTechnicianClass1Swipe(currentTag); break;
            case IDTag.tagtype_TECHNICIAN_CLASS_2: handleTechnicianClass2Swipe(currentTag); break;
            default: Log.e("Swipe", "ERROR: UNIDENTIFIED CARD TYPE"); break;
        }
    }

    private void handleJanitorSwipe(IDTag tag)
    {
        if(displayingInProgress)
        {
            displayingInProgress = false;
            stopInProgressTimer.cancel();
            stopInProgressTimer.purge();
            displayAdImage();
        }
        else
        {
            adImageView.setVisibility(View.INVISIBLE);
            speakAlerts(tag);
            speakJanitorInstructions(tag);
            //startDialog(new PingingFor_JanitorTroubleTicket1());
        }
    }

    private void handleSecuritySwipe(IDTag tag)
    {
        adImageView.setVisibility(View.INVISIBLE);
        speakAlerts(tag);
        speakSecurityInstructions(tag);

    }

    private void handleTechnicianClass1Swipe(IDTag tag)
    {
        adImageView.setVisibility(View.INVISIBLE);
        speakAlerts(tag);
        speakTechnicianClass1Instructions(tag);
    }

    private void handleTechnicianClass2Swipe(IDTag tag)
    {
        adImageView.setVisibility(View.INVISIBLE);
        if(!tag.isOnJob())
        {
            currentTag.setIsOnJob(true);
            saveData();
            speakAlerts(tag);
            speakTechnicianClass2Instructions(tag);
        }
        else
        {
            displayJobFinishedImage();
        }
    }

    private void speakJanitorInstructions(IDTag tag)
    {
        String alertSpeechString = "Here are your instructions, Cleaner " + tag.getName() + ". . ";
        String alertTextString = "\nHere are your instructions, Cleaner " + tag.getName() + ":\n--------------------------------------------------------\n";
        alertSpeechString += " 1. Check slash change the toilet paper." + " . ";
        alertTextString += "\n1. Check/Change the toilet paper.\n";
        alertSpeechString += " 2. Mop the floor." + " . ";
        alertTextString += "\n2. Mop the floor.\n";
        alertSpeechString += " 3. Empty the Trash." + " . ";
        alertTextString += "\n3. Empty the Trash.\n";

        alertSpeechString += " End of Instructions. . ";

        instructionsDataText.setText(alertTextString);
        toSpeech.speak(alertSpeechString, TextToSpeech.QUEUE_ADD, null, "EndOfJanitorInstructions");
    }

    private void speakSecurityInstructions(IDTag tag)
    {
        String alertSpeechString = "Here are your instructions, Security Guard " + tag.getName() + ". . ";
        String alertTextString = "\nHere are your instructions, Security Guard " + tag.getName() + ":\n--------------------------------------------------------\n";
        alertSpeechString += " 1. Is the bathroom clean?" + " . ";
        alertTextString += "\n1. Is the bathroom clean?\n";
        alertSpeechString += " 2. Is the water running?" + " . ";
        alertTextString += "\n2. Is the water running?\n";
        alertSpeechString += " 3. Are any of the stalls locked?" + " . ";
        alertTextString += "\n3. Are any of the stalls locked?\n";

        alertSpeechString += " End of Instructions. . ";

        instructionsDataText.setText(alertTextString);
        toSpeech.speak(alertSpeechString, TextToSpeech.QUEUE_ADD, null, "EndOfSecurityInstructions");
    }

    private void speakTechnicianClass1Instructions(IDTag tag)
    {
        String alertSpeechString = "You are not Authorised to work on this device, Technician " + tag.getName() + ". . ";
        String alertTextString = "\nYou are not Authorised to work on this device, Technician " + tag.getName() + ":\n--------------------------------------------------------\n";

        alertSpeechString += " End of Instructions. . ";

        instructionsDataText.setText(alertTextString);
        toSpeech.speak(alertSpeechString, TextToSpeech.QUEUE_ADD, null, "EndOfTechnicianClass1Instructions");
    }

    private void speakTechnicianClass2Instructions(IDTag tag)
    {
        String alertSpeechString = "Here are your instructions, Technician " + tag.getName() + ". . ";
        String alertTextString = "\nHere are your instructions, Technician " + tag.getName() + ":\n--------------------------------------------------------\n";
        alertSpeechString += " 1. Remove the panel." + " . ";
        alertTextString += "\n1. Remove the panel.\n";
        alertSpeechString += " 2. Change the air pipe." + " . ";
        alertTextString += "\n2. Change the air pipe.\n";
        alertSpeechString += " 3. Replace the panel." + " . ";
        alertTextString += "\n3. Replace the panel.\n";

        alertSpeechString += " End of Instructions. . ";

        instructionsDataText.setText(alertTextString);
        toSpeech.speak(alertSpeechString, TextToSpeech.QUEUE_ADD, null, "EndOfTechnicianClass2Instructions");
    }


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
                        else if (utteranceId.matches("EndOfJanitorInstructions"))
                        {
                            startDialog(new PingingFor_JanitorTroubleTicket1());
                        }
                        else if(utteranceId.matches(new PingingFor_JanitorTroubleTicket1().getName()))
                        {
                            postSpeak_JanitorTroubleTicket1();

                        }
                        else if(utteranceId.matches(new PingingFor_JanitorTroubleTicket2().getName()))
                        {
                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    startRecogListening(new PingingFor_JanitorTroubleTicket2());
                                }
                            });
                        }
                        else if(utteranceId.matches(new PingingFor_JanitorTroubleTicketLeak1().getName()))
                        {
                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {

                                    startRecogListening(new PingingFor_JanitorTroubleTicketLeak1());
                                }
                            });
                        }
                        else if(utteranceId.matches("EndOfSecurityInstructions"))
                        {
                            displayAdImage();
                        }
                        else if(utteranceId.matches("EndOfTechnicianClass1Instructions"))
                        {
                            displayAdImage();
                        }
                        else if(utteranceId.matches("EndOfTechnicianClass2Instructions"))
                        {
                            displayInProgressImage();
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
        recog.stopListening();
    }

    @Override
    public void onError(int i)
    {
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
                    toSpeech.speak("No Response Detected, aborting.", TextToSpeech.QUEUE_FLUSH, null, "EndError");
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
        Log.i("Recog", "OnResults");
        ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
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
                prepareResponseFor(matches.get(0), pingingRecogFor);
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
        Log.i("Output", "sortAllPossibleResultsForAllMatches in: " + results.toString() + " for intent: " + pingingFor.getName());

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

        ArrayList<String> possibleKeywords = sortAllPossibleResultsForAllMatches(results, pingingFor);

        if (possibleKeywords.size() > 1)
        {
            Log.i("Output", "about to start pinging for Clarification with: " + possibleKeywords.toString());
            Log.i("Output", "about to start pinging for Clarification with: " + possibleKeywords.toString());
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
        }
    }

    //Call the Output method for the given intent
    private void prepareResponseFor(String result, SpeechIntent pingingFor)
    {
        Log.i("Output", "prepareResponseFor" + pingingFor.getName() + " with result: " + result);

        if (pingingFor.getName().matches(new PingingFor_YesNo().getName()))
        {
            pingingFor.getOutput(this, result);
        }
        else if (pingingFor.getName().matches(new PingingFor_JanitorTroubleTicket1().getName()))
        {
            if(result.matches(PingingFor_JanitorTroubleTicket1.Response_RaiseTroulbeTicket))
            {
                startDialog(new PingingFor_JanitorTroubleTicket2());
            }
        }
        else if(pingingFor.getName().matches(new PingingFor_JanitorTroubleTicket2().getName()))
        {
            prepareResponseForJanitorTroubleTicket2(result);
        }
        else if(pingingFor.getName().matches(new PingingFor_JanitorTroubleTicketLeak1().getName()))
        {
            prepareResponseForJanitorTroubleTicketLeak1(result);
        }
        else
        {
            Log.e("Response:", "No response setup for this intent: " + pingingFor.getName());
        }
    }

//++++++++[End of Recognition Other Code]

//++++++++[Custom Responses]

    private void postSpeak_JanitorTroubleTicket1()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                displayInProgressImage();
                startRecogListening(new PingingFor_JanitorTroubleTicket1());
            }
        });
    }

    private void prepareResponseForJanitorTroubleTicket2(String result)
    {
        if(result.matches(PingingFor_JanitorTroubleTicket2.Response_Leak))
        {
            leakInfoImage.setVisibility(View.VISIBLE);
            displayingLeakInfo = true;
            stopLeakInfoTimer = new Timer();
            stopLeakInfoTimer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    if(displayingLeakInfo)
                    {
                        displayingLeakInfo = false;
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                leakInfoImage.setVisibility(View.INVISIBLE);
                            }
                        });

                    }
                }
            }, 30000); //after 1 minute, revert to ads.
            startDialog(new PingingFor_JanitorTroubleTicketLeak1());
        }
        else if(result.matches(PingingFor_JanitorTroubleTicket2.Response_Tile))
        {

        }
    }

    private void prepareResponseForJanitorTroubleTicketLeak1(String result)
    {
        if(displayingLeakInfo)
        {
            displayingLeakInfo = false;
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    leakInfoImage.setVisibility(View.INVISIBLE);
                }
            });

        }

        if(result.matches(PingingFor_JanitorTroubleTicketLeak1.Response_Toilet))
        {
            toSpeech.speak("Sounds like a leak in the toilet. Creating Trouble Ticket.", TextToSpeech.QUEUE_FLUSH, null, "JanitorCreatedLeakToiletTroubleTicket");
            allAlerts.add(new AlertData(currentStationID, "Leaking toilet", true, IDTag.tagtype_TECHNICIAN_CLASS_2));
            saveData();
        }
        else if(result.matches(PingingFor_JanitorTroubleTicketLeak1.Response_Ceiling))
        {
            toSpeech.speak("Sounds like a leaking ceiling. Creating Trouble Ticket.", TextToSpeech.QUEUE_FLUSH, null, "JanitorCreatedLeakCeilingTroubleTicket");
            allAlerts.add(new AlertData(currentStationID, "Leaking ceiling", true, IDTag.tagtype_TECHNICIAN_CLASS_2));
            saveData();
        }
        else if(result.matches(PingingFor_JanitorTroubleTicketLeak1.Response_HVAC))
        {
            toSpeech.speak("Sounds like a leak in the H Vac. Creating Trouble Ticket.", TextToSpeech.QUEUE_FLUSH, null, "JanitorCreatedLeakHvacTroubleTicket");
            allAlerts.add(new AlertData(currentStationID, "Leaking H VAC", true, IDTag.tagtype_TECHNICIAN_CLASS_2));
            saveData();
        }
        else if(result.matches(PingingFor_JanitorTroubleTicketLeak1.Response_Sink))
        {
            toSpeech.speak("Sounds like a leaking sink. Creating Trouble Ticket.", TextToSpeech.QUEUE_FLUSH, null, "JanitorCreatedLeakSinkTroubleTicket");
            allAlerts.add(new AlertData(currentStationID, "Leaking sink", true, IDTag.tagtype_TECHNICIAN_CLASS_2));
            saveData();
        }
    }
//++++++++[End of Custom Responses]


//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//++++++++++++++++++++++++++End of Voice Interface Code+++++++++++++++++++++++++++++


    //[Misc Methods]
    private void displayAdImage()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                inProgressImage.setVisibility(View.INVISIBLE);
                jobFinishedImage.setVisibility(View.INVISIBLE);
                jobFinishedButton.setVisibility(View.INVISIBLE);
                adImageView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void displayInProgressImage()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                adImageView.setVisibility(View.INVISIBLE);
                jobFinishedImage.setVisibility(View.INVISIBLE);
                jobFinishedButton.setVisibility(View.INVISIBLE);
                inProgressImage.setVisibility(View.VISIBLE);
            }
        });
        displayingInProgress = true;
        stopInProgressTimer = new Timer();
        stopInProgressTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                if (displayingInProgress)
                {
                    displayingInProgress = false;
                    displayAdImage();
                }
            }
        }, 60000); //after 1 minute, revert to ads.
    }

    private void displayJobFinishedImage()
    {
        adImageView.setVisibility(View.INVISIBLE);
        jobFinishedImage.setVisibility(View.VISIBLE);
        jobFinishedButton.setVisibility(View.VISIBLE);
        jobFinishedButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                displayingJobFinished = false;
                displayAdImage();
            }
        });
        inProgressImage.setVisibility(View.INVISIBLE);
        displayingJobFinished = true;
        stopJobFinishedTimer = new Timer();
        stopJobFinishedTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                if (displayingJobFinished)
                {
                    displayingJobFinished = false;
                    displayAdImage();
                    currentTag.setIsOnJob(false);
                    saveData();
                }
            }
        }, 30000); //after 1 minute, revert to ads.
    }
    //[End of Misc Methods]
}
