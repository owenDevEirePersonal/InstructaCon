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
        //setSpeechPrompt("Instructacon Troubler System Online, how can I help?");
        setSpeechPrompt(" Welcome. What can I do for you today?");
        HashMap<String, ArrayList<String>> responses = new HashMap<>();
        responses.put("Raise Trouble Ticket", SpeechIntent.compileSynonyms(new String[]{
                "Raise Trouble Ticket",
                "there is a problem",
                "there's a problem",
                "raise a ticket",
                "trouble ticket",
                "raise a trouble ticket"}));
        responses.put("Directions to room A", SpeechIntent.compileSynonyms(new String[]{
                "I need directions to room A",
                "How do i get to room A",
                "how do I get from here to room A",
                "where is room A", "way to room A",
                "directions to room A",
                "directions to meeting room A",
                "I would like directions to room A",
                "I would like directions to meeting room A",
                "meeting meeting room A",
                "I need directions to meeting room A",
                "How do i get to meeting room A",
                "how do I get from here to meeting room A",
                "where is meeting room A",
                "way to meeting room A"}));
        responses.put("Directions to room B", SpeechIntent.compileSynonyms(new String[]{
                "I need directions to room B",
                "How do i get to room B",
                "how do I get from here to room B",
                "where is room B", "way to room B",
                "directions to room B",
                "directions to meeting room B",
                "meeting meeting room B",
                "I would like directions to room B",
                "I would like directions to meeting room B",
                "I need directions to meeting room B",
                "How do i get to meeting room B",
                "how do I get from here to meeting room B",
                "where is meeting room B",
                "way to meeting room B"}));
        responses.put("Call me back", SpeechIntent.compileSynonyms(new String[]{"call me back", "call me", "hear back from"}));
        responses.put("Help", SpeechIntent.compileSynonyms(new String[]{"help", "how do you work", "how do I use you"}));
        responses.put("Nevermind", SpeechIntent.compileSynonyms(new String[]{"no", "false", "incorrect", "not okay", "nevermind", "forget it"}));
        setResponses(responses);
    }

}
