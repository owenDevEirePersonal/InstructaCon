package com.deveire.dev.instructacon.remastered.TroubleTicketSystem;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by owenryan on 09/07/2018.
 */

public class TroubleKeyword
{
    private String keyword;
    private ArrayList<String> synomyns;
    private String promptQuestion;

    public TroubleKeyword(String keywordin, ArrayList<String> synomynsIn, String promptQuestionIn)
    {
        keyword = keywordin;
        synomyns = synomynsIn;
        promptQuestion = promptQuestionIn;
    }

    public TroubleKeyword(String keywordin, String[] synomynsIn, String promptQuestionIn)
    {
        keyword = keywordin;
        synomyns = new ArrayList<String>(Arrays.asList(synomynsIn));
        promptQuestion = promptQuestionIn;
    }

    public boolean matches(String keywordIn)
    {
        if(this.keyword.matches(keywordIn))
        {
            return true;
        }

        return false;
    }

    public boolean matches(TroubleKeyword keywordIn)
    {
        if(this.keyword.matches(keywordIn.getKeyword()))
        {
            return true;
        }

        return false;
    }


    public boolean matchesSynomyns(String keywordIn)
    {
        if(this.keyword.matches(keywordIn))
        {
            return true;
        }

        for (String aString: synomyns)
        {
            if(aString.matches(keywordIn))
            {
                return true;
            }
        }
        return false;
    }

    public String getKeyword()
    {
        return keyword;
    }

    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }

    public ArrayList<String> getSynomyns()
    {
        return synomyns;
    }

    public void setSynomyns(ArrayList<String> synomyns)
    {
        this.synomyns = synomyns;
    }

    public void addSynomyn(String newSynomyn)
    {
        this.synomyns.add(newSynomyn);
    }

    public String getPromptQuestion()
    {
        return promptQuestion;
    }

    public void setPromptQuestion(String promptQuestion)
    {
        this.promptQuestion = promptQuestion;
    }
}
