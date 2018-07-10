package com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents;

import android.content.Context;

import com.deveire.dev.instructacon.remastered.SpeechIntent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by owenryan on 20/04/2018.
 */

public class PingingFor_YourOwnTask extends SpeechIntent
{
    public PingingFor_YourOwnTask()
    {
        super("PingingFor_YourOwnTask");
        setFillInIntent(true);
        setSpeechPrompt("I'm sorry but I was unable to find any task that matches the problem as you described it, I will now record your problem for human review, please describe the problem in full after the tone.");
        HashMap<String, ArrayList<String>> responses = new HashMap<>();
        setResponses(responses);
    }
}
