package com.deveire.dev.instructacon.remastered.TroubleTicketSystem;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by owenryan on 09/07/2018.
 */

public class TroubleKeyword
{
    private String keyword; //Keyword
    private ArrayList<String> synonyms; //Synonyms for
    private String promptQuestion;

    public TroubleKeyword(String keywordin, ArrayList<String> synonymsIn, String promptQuestionIn)
    {
        keyword = keywordin;
        synonyms = synonymsIn;
        promptQuestion = promptQuestionIn;
    }

    public TroubleKeyword(String keywordin, String[] synonymsIn, String promptQuestionIn)
    {
        keyword = keywordin;
        synonyms = new ArrayList<String>(Arrays.asList(synonymsIn));
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

        for (String aString: synonyms)
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

    public ArrayList<String> getSynonyms()
    {
        return synonyms;
    }

    public void setSynonyms(ArrayList<String> synonyms)
    {
        this.synonyms = synonyms;
    }

    public void addSynomyn(String newSynomyn)
    {
        this.synonyms.add(newSynomyn);
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
