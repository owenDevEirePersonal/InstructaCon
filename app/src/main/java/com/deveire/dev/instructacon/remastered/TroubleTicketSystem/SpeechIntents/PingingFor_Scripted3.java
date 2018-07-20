package com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents;

import com.deveire.dev.instructacon.remastered.SpeechIntent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by owenryan on 12/07/2018.
 */

public class PingingFor_Scripted3 extends SpeechIntent
{
    //TODO: Destroy after demo

    public PingingFor_Scripted3(String description)
    {
        super("PingingFor_Scripted3");
        setFillInIntent(false);
        setSpeechPrompt(description);
        HashMap<String, ArrayList<String>> responses = new HashMap<>();
        responses.put("Yes", SpeechIntent.compileSynonyms(new String[]{"ok", "yes", "okay", "correct", "yeah", "it does"}));
        responses.put("No", SpeechIntent.compileSynonyms(new String[]{"no", "false", "incorrect", "not okay", "it doesn't", "not"}));
        responses.put("Never mind", SpeechIntent.compileSynonyms(new String[]{"never mind", "abort", "end dialog", "forget it"}));
        setResponses(responses);
    }
}
