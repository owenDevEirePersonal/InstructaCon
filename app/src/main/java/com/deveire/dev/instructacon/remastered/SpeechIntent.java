package com.deveire.dev.instructacon.remastered;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by owenryan on 20/04/2018.
 */

public abstract class SpeechIntent
{
    private String name;
    private Map<String, ArrayList<String>> responses;
    private boolean isFillInIntent;
    private String speechPrompt;

    public SpeechIntent(String name, boolean isFillInIntent)
    {
        this.name = name;
        this.isFillInIntent = isFillInIntent;
        responses = new HashMap<>();
    }

    public SpeechIntent(String name, HashMap responseMap)
    {
        this.name = name;
        this.isFillInIntent = false;
        responses = responseMap;
    }

    public SpeechIntent(String name)
    {
        this.name = name;
        this.isFillInIntent = false;
        responses = new HashMap<>();
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean isFillInIntent()
    {
        return isFillInIntent;
    }

    public void setFillInIntent(boolean fillInIntent)
    {
        isFillInIntent = fillInIntent;
    }

    public void setResponses(Map<String, ArrayList<String>> responses)
    {
        this.responses = responses;
    }

    public Map<String, ArrayList<String>> getResponses()
    {
        return responses;
    }

    public ArrayList<String> getResponseKeywords()
    {
        ArrayList<String> keywords = new ArrayList<String>(responses.keySet());
        return keywords;
    }

    public ArrayList<String> getResponseSynonyms(String keyword)
    {
        return responses.get(keyword);
    }

    //Synonyms are used to match to the keyword.
    // Note: the keyword itself is not used to make to the keyword unless the keyword is included as synonym
    public static ArrayList<String> compileSynonyms(String[] synonyms)
    {
        ArrayList<String> syn = new ArrayList<String>();
        for (String a: synonyms)
        {
            syn.add(a);
        }
        return syn;
    }

    //Get Output intended for returning the string of Speech output only, any ui alterations must be done in the activity calling getOutput
    // and not in the getOutput method itself
    public void getOutput(Context context, String keyword)
    {
       //"-Placeholder getOutput. You should not be seeing this-";
    }

    public String getSpeechPrompt()
    {
        return speechPrompt;
    }

    public void setSpeechPrompt(String speechPrompt)
    {
        this.speechPrompt = speechPrompt;
    }
}
