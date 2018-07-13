package com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents;

import android.content.Context;

import com.deveire.dev.instructacon.remastered.SpeechIntent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by owenryan on 20/04/2018.
 */

public class PingingFor_ScriptedB1 extends SpeechIntent
{
    public PingingFor_ScriptedB1()
    {
        super("PingingFor_ScriptedB1");
        setFillInIntent(false);
        setSpeechPrompt("To get to meeting Room 7, head south and take the first right turn, then the 1st left turn, meeting room 7 will be the first door on the left. Can I be of any more assistance?");
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
