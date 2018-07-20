package com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents;

import com.deveire.dev.instructacon.remastered.SpeechIntent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by owenryan on 12/07/2018.
 */

public class PingingFor_Scripted1 extends SpeechIntent
{
    //TODO: Destroy after demo

    public PingingFor_Scripted1()
    {
        super("PingingFor_Scripted1");
        setFillInIntent(false);
        setSpeechPrompt("You will receive an image of the sink on your phone or PC  in a few minutes. Please use your finger to circle the area you see the leak in.");
        HashMap<String, ArrayList<String>> responses = new HashMap<>();
        responses.put("Ok", SpeechIntent.compileSynonyms(new String[]{"Ok", "yes", "alright", "will do"}));
        responses.put("Never mind", SpeechIntent.compileSynonyms(new String[]{"never mind", "abort", "end dialog", "forget it"}));
        setResponses(responses);
    }
}
