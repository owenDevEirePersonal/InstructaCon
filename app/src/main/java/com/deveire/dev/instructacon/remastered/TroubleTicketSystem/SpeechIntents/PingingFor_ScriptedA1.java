package com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents;

import android.content.Context;

import com.deveire.dev.instructacon.remastered.SpeechIntent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by owenryan on 20/04/2018.
 */

public class PingingFor_ScriptedA1 extends SpeechIntent
{
    public PingingFor_ScriptedA1()
    {
        super("PingingFor_ScriptedA1");
        setFillInIntent(false);
        setSpeechPrompt("Dan your request has been logged and our team member will call you back on 0872312567 within 15 minutes is this OK?");
        HashMap<String, ArrayList<String>> responses = new HashMap<>();
        responses.put("Yes", SpeechIntent.compileSynonyms(new String[]{"ok", "yes", "okay", "correct", "yeah", "it does"}));
        responses.put("No", SpeechIntent.compileSynonyms(new String[]{"no", "false", "incorrect", "not okay", "it doesn't", "not"}));
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
