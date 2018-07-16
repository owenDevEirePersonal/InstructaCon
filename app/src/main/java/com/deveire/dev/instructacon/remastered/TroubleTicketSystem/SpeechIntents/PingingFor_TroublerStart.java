package com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents;

import android.content.Context;

import com.deveire.dev.instructacon.remastered.SpeechIntent;
import com.deveire.dev.instructacon.remastered.TroubleTicketSystem.TroubleKeyword;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by owenryan on 20/04/2018.
 */

public class PingingFor_TroublerStart extends SpeechIntent
{
    public PingingFor_TroublerStart()
    {
        super("PingingFor_TroublerStart");
        setFillInIntent(false);
        setSpeechPrompt("Instructacon Troubler System Online, how can I help?");
        HashMap<String, ArrayList<String>> responses = new HashMap<>();
        responses.put("Raise Trouble Ticket", SpeechIntent.compileSynonyms(new String[]{"Raise Trouble Ticket", "there is a problem", "there's a problem", "raise a ticket", "trouble ticket", "raise a trouble ticket", "order a meal", "I'd like to order something", "meal", "order"}));
        responses.put("Directions to room 7", SpeechIntent.compileSynonyms(new String[]{"I need directions to room 7", "How do i get to room 7", "how do I get from here to room 7", "where is room 7", "way to room 7"}));
        responses.put("Nevermind", SpeechIntent.compileSynonyms(new String[]{"no", "false", "incorrect", "not okay", "nevermind", "forget it"}));
        setResponses(responses);
    }

}
