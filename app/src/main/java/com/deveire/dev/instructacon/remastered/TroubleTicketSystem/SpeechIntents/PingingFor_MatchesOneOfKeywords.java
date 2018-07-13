package com.deveire.dev.instructacon.remastered.TroubleTicketSystem.SpeechIntents;

import android.content.Context;

import com.deveire.dev.instructacon.remastered.SpeechIntent;
import com.deveire.dev.instructacon.remastered.TroubleTicketSystem.TroubleKeyword;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by owenryan on 20/04/2018.
 */

public class PingingFor_MatchesOneOfKeywords extends SpeechIntent
{
    public PingingFor_MatchesOneOfKeywords(TroubleKeyword[] keywordsToMatch, String startOfPromptQuestion)
    {
        super("PingingFor_OneOf_" + keywordsToMatch[0]);
        setFillInIntent(false);
        String fullPrompt = startOfPromptQuestion + " ";
        HashMap<String, ArrayList<String>> responses = new HashMap<>();
        int i = 0;
        for (TroubleKeyword aKeyword: keywordsToMatch)
        {
            if(i == keywordsToMatch.length - 1)
            {
                fullPrompt += aKeyword.getKeyword() + ". ";
                responses.put(aKeyword.getKeyword(), aKeyword.getSynomyns());
            }
            else
            {
                fullPrompt += aKeyword.getKeyword() + " or. ";
                responses.put(aKeyword.getKeyword(), aKeyword.getSynomyns());
            }
            i++;
        }
        setSpeechPrompt(fullPrompt);
        setResponses(responses);
    }

    public PingingFor_MatchesOneOfKeywords()
    {
        super("PingingFor_OneOf_NULL");
        setFillInIntent(false);
        HashMap<String, ArrayList<String>> responses = new HashMap<>();
        setSpeechPrompt("What are you doing this a placeholder constuctor for MtchesOneOfKeywords, you shouldn't be hearing this!");
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
