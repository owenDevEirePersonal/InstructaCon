package com.deveire.dev.instructacon.remastered.SpeechIntents;

import android.content.Context;
import android.util.Log;


import com.deveire.dev.instructacon.remastered.SpeechIntent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by owenryan on 20/04/2018.
 */

public class PingingFor_Clarification extends SpeechIntent
{
    public PingingFor_Clarification(ArrayList<String> keywordsToDecideOn)
    {
        super("PingingFor_Clarification");
        setFillInIntent(false);
        String prompt = "I'm sorry but did you mean. ";
        boolean isFirstPrompt = true;
        for (String aKeyword: keywordsToDecideOn)
        {
            if(isFirstPrompt)
            {
                prompt += aKeyword;
                isFirstPrompt = false;
            }
            else
            {
                prompt += " or. " + aKeyword;
            }
        }
        setSpeechPrompt(prompt);
        HashMap<String, ArrayList<String>> responses = new HashMap<>();
        for (String aKeyword: keywordsToDecideOn)
        {
            responses.put(aKeyword, compileSynonyms(new String[]{aKeyword}));
        }
        setResponses(responses);
    }

    public PingingFor_Clarification()
    {
        super("PingingFor_Clarification");
        Log.i("WARNING:", "PingingFor_Clarification() should only be used to get the name of PingingFor-Clarification");
    }

    public void getOutput(Context context, String response)
    {
        switch (response)
        {
            default: Log.e("Output","WHAT! ERROR! You shouldn't call this, this is Clarification, you should define output outside of this Intent!");
        }
    }

}
