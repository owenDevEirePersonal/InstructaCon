package com.deveire.dev.instructacon.remastered;

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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.deveire.dev.instructacon.R;
import com.deveire.dev.instructacon.remastered.SpeechIntents.PingingFor_Clarification;
import com.deveire.dev.instructacon.remastered.SpeechIntents.PingingFor_YesNo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class PillsActivity extends Activity implements RecognitionListener
{
    private TextView debugText;
    private TextView outputText;

    private NfcAdapter nfcAdapt;

    private ImageView coverImage;

    private Button resetButton;

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

    private PingingFor_YesNo pingingFor_isPillYesNo;
    private int progressCount;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pills);

        debugText = (TextView) findViewById(R.id.debugText);
        outputText = (TextView) findViewById(R.id.outputText);

        resetButton = (Button) findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                progressCount = 0;
                showImage(R.drawable.menu);
                pingingFor_isPillYesNo.setSpeechPrompt("Have you taken your 1st Blue Generika pill?");
            }
        });

        coverImage = (ImageView) findViewById(R.id.coverImage);

        imageTimer = new Timer();

        setupSpeechRecognition();
        recogTimeoutTimer = new Timer();
        setupTextToSpeech();

        progressCount = 0;
        pingingFor_isPillYesNo = new PingingFor_YesNo();
        pingingFor_isPillYesNo.setSpeechPrompt("Have you taken your 1st Blue Generika pill?");

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
        wl.acquire();

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
                //startDialog(pingingFor_isPillYesNo);
                if(progressCount < 3)
                {
                    startDialog(pingingFor_isPillYesNo);
                }
                else
                {
                    toSpeech.speak("Congratulations you have taken all your pills for today", TextToSpeech.QUEUE_FLUSH, null, "Thats all the fish");
                }
            }
        });

        //start Dialog here
    }

    private void showImage(int resID)
    {
        Log.i("Pills", "Changing image");
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
                        if(utteranceId.matches(pingingFor_isPillYesNo.getName()))
                        {
                            switch (progressCount)
                            {
                                case 0:

                                    runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            Log.i("Pills", "case 0");
                                            showImage(R.drawable.pills1);
                                        }
                                    });
                                    break;
                                case 1:

                                    runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            Log.i("Pills", "case 1");
                                            showImage(R.drawable.pills2);
                                        }
                                    });
                                    break;
                                case 2:

                                    runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            Log.i("Pills", "case 2");
                                            showImage(R.drawable.pills3);
                                        }
                                    });
                                    break;
                            }
                        }
                        else if(progressCount == 3)
                        {
                            progressCount ++;
                            toSpeech.speak("Congratulations you have taken all your pills for today", TextToSpeech.QUEUE_FLUSH, null, "Thats all the fish");

                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    Log.i("Pills", "case 3");
                                    showImage(R.drawable.menu);
                                }
                            });
                        }
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
                        else if(utteranceId.matches(pingingFor_isPillYesNo.getName()))
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

        if (pingingFor.getName().matches(pingingFor_isPillYesNo.getName()))
        {
            if(progressCount < 3)
            {
                if (result.matches("Yes"))
                {
                    switch (progressCount)
                    {
                        case 0:
                            pingingFor_isPillYesNo.setSpeechPrompt("Have you taken your 2nd Blue Generika Pill?");
                            progressCount++;
                            startDialog(pingingFor_isPillYesNo);
                            break;
                        case 1:
                            pingingFor_isPillYesNo.setSpeechPrompt("Have you taken your Red Non Existan Tablet?");
                            progressCount++;
                            startDialog(pingingFor_isPillYesNo);
                            break;
                        case 2:
                            toSpeech.speak("Congratulations you have taken all your pills for today", TextToSpeech.QUEUE_FLUSH, null, "Thats all the fish");
                            progressCount++;
                            break;
                    }

                }
                else if (result.matches("No"))
                {
                    toSpeech.speak("Please take your pill and swipe again", TextToSpeech.QUEUE_FLUSH, null, "ThatsNoFish");
                    outputText.setText("Please take your pill and swipe again");
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
