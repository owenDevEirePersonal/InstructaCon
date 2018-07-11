package com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents;

import com.deveire.dev.instructacon.remastered.SpeechIntent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by owenryan on 20/04/2018.
 */

public class PingingFor_IntialDescription extends SpeechIntent
{
    public PingingFor_IntialDescription()
    {
        super("PingingFor_IntialDescription");
        setFillInIntent(true);
        setSpeechPrompt("Raising Trouble Ticket. What seems to be the problem?");
        HashMap<String, ArrayList<String>> responses = new HashMap<>();
        setResponses(responses);
    }
}
