package com.deveire.dev.instructacon.remastered.SpeechIntents;

import android.content.Context;
import android.widget.Toast;

import com.deveire.dev.instructacon.remastered.SpeechIntent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by owenryan on 03/07/2018.
 */

public class PingingFor_Lockup2 extends SpeechIntent
{
    public static final String Response_Yes = "Yes";
    public static final String Response_No = "No";

    public PingingFor_Lockup2()
    {
        super("PingingFor_Lockup2");
        setFillInIntent(false);
        setSpeechPrompt("Are the lights off?");
        HashMap<String, ArrayList<String>> responses = new HashMap<>();
        responses.put(Response_Yes, SpeechIntent.compileSynonyms(new String[]{"ok", "yes", "okay", "correct", "it is"}));
        responses.put(Response_No, SpeechIntent.compileSynonyms(new String[]{"no", "false", "incorrect", "not okay", "it's not", "negative"}));
        setResponses(responses);
    }

    @Override
    public void getOutput(Context context, String keyword)
    {
        //ImageView outputText = (ImageView) ((Activity)context).findViewById(R.id.lockupImage);
        switch (keyword)
        {
            case "Yes": Toast.makeText(context, "Yes", Toast.LENGTH_LONG); break; //outputText.setImageResource(R.id.lockupImage1); break;
            case "No": Toast.makeText(context, "No", Toast.LENGTH_LONG); break; //outputText.setImageResource(R.id.DefaultImage); break;
            default: break;//outputText.setImageResource(R.id.DefaultImage); break;
        }
    }

}
