package com.deveire.dev.instructacon.remastered.SpeechIntents;

import android.app.Activity;
import android.content.Context;
import android.widget.TextView;


import com.deveire.dev.instructacon.remastered.SpeechIntent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by owenryan on 20/04/2018.
 */

public class PingingFor_YesNo extends SpeechIntent
{
    public PingingFor_YesNo()
    {
        super("PingingFor_YesNo");
        setFillInIntent(false);
        setSpeechPrompt("You really should define this and not use the default one for this particular Speech Intent, you that know right?");
        HashMap<String, ArrayList<String>> responses = new HashMap<>();
        responses.put("Yes", SpeechIntent.compileSynonyms(new String[]{"ok", "yes", "okay", "correct"}));
        responses.put("No", SpeechIntent.compileSynonyms(new String[]{"no", "false", "incorrect", "not okay"}));
        setResponses(responses);
    }

    @Override
    public void getOutput(Context context, String keyword)
    {
        /*TextView outputText = (TextView) ((Activity)context).findViewById(R.id.outputText);
        switch (keyword)
        {
            case "Yes": outputText.setText("Yes"); break;
            case "No": outputText.setText("No"); break;
            default: outputText.setText("What? :" + keyword + " is not a keyword with an implimentation"); break;
        }*/
    }
}
