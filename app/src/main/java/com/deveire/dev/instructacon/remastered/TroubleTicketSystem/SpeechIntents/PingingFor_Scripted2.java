package com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents;

import com.deveire.dev.instructacon.remastered.SpeechIntent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by owenryan on 12/07/2018.
 */

public class PingingFor_Scripted2 extends SpeechIntent
{
    //TODO: Destroy after demo

    public PingingFor_Scripted2()
    {
        super("PingingFor_Scripted2");
        setFillInIntent(false);
        setSpeechPrompt("Trouble Ticket 2 3 9 8 has been raised. Would you like to be kept informed of the ticket's progress?");
        HashMap<String, ArrayList<String>> responses = new HashMap<>();
        responses.put("Yes", SpeechIntent.compileSynonyms(new String[]{"ok", "yes", "okay", "correct", "yeah", "it does"}));
        responses.put("No", SpeechIntent.compileSynonyms(new String[]{"no", "false", "incorrect", "not okay", "it doesn't", "not"}));
        responses.put("Never mind", SpeechIntent.compileSynonyms(new String[]{"never mind", "abort", "end dialog", "forget it"}));
        setResponses(responses);
    }
}
