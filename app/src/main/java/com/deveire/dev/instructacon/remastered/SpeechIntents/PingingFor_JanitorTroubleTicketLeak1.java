package com.deveire.dev.instructacon.remastered.SpeechIntents;

import com.deveire.dev.instructacon.remastered.SpeechIntent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by owenryan on 03/05/2018.
 */

public class PingingFor_JanitorTroubleTicketLeak1 extends SpeechIntent
{

    public static final String Response_Toilet = "Toilet";
    public static final String Response_Sink = "Sink";
    public static final String Response_HVAC = "Hvac";
    public static final String Response_Ceiling = "Ceiling";

    public PingingFor_JanitorTroubleTicketLeak1()
    {
        super("PingingFor_JanitorTroubleTicketLeak1");
        setFillInIntent(false);
        setSpeechPrompt("Sounds like a leak. Is the leak in a Toilet. a Sink. the ceiling. or a H Vac?");
        HashMap<String, ArrayList<String>> responses = new HashMap<>();
        responses.put(Response_Toilet, SpeechIntent.compileSynonyms(new String[]{"Toilet", "Cistern", "Crapper"}));
        responses.put(Response_Sink, SpeechIntent.compileSynonyms(new String[]{"Sink", "Wash Basin"}));
        responses.put(Response_HVAC, SpeechIntent.compileSynonyms(new String[]{"H vac", "Hvac"}));
        responses.put(Response_Ceiling, SpeechIntent.compileSynonyms(new String[]{"Ceiling", "roof"}));
        setResponses(responses);
    }


}
