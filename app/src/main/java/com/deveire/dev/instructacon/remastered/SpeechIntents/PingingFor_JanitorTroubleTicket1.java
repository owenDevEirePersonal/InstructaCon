package com.deveire.dev.instructacon.remastered.SpeechIntents;

import com.deveire.dev.instructacon.remastered.SpeechIntent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by owenryan on 03/05/2018.
 */

public class PingingFor_JanitorTroubleTicket1 extends SpeechIntent
{

    public static final String Response_RaiseTroulbeTicket = "Raise Trouble Ticket";
    public static final String Response_No = "No";

    public PingingFor_JanitorTroubleTicket1()
    {
        super("PingingFor_JanitorTroubleTicket1");
        setFillInIntent(false);
        setSpeechPrompt("Is there anything vida can help you with?");
        HashMap<String, ArrayList<String>> responses = new HashMap<>();
        responses.put(Response_RaiseTroulbeTicket, SpeechIntent.compileSynonyms(new String[]{"raise trouble ticket", "trouble ticket", "register trouble ticket", "there's a problem", "there is a problem", "I have a problem"}));
        responses.put(Response_No, SpeechIntent.compileSynonyms(new String[]{"no", "false", "incorrect", "not okay", "it's fine", "I'm fine", "negative"}));
        setResponses(responses);
    }


}
