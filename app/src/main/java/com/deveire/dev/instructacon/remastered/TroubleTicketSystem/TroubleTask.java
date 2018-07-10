package com.deveire.dev.instructacon.remastered.TroubleTicketSystem;

import java.util.ArrayList;

/**
 * Created by owenryan on 09/07/2018.
 */

public class TroubleTask
{
    private String Description;
    private String promptQuestion;
    private ArrayList<TroubleKeyword> tags;
    private String requirements;

    public TroubleTask(String description, String promptQuestion, ArrayList<TroubleKeyword> tags, String requirements)
    {
        Description = description;
        this.promptQuestion = promptQuestion;
        this.tags = tags;
        this.requirements = requirements;
    }

    public String getDescription()
    {
        return Description;
    }

    public void setDescription(String description)
    {
        Description = description;
    }

    public String getPromptQuestion()
    {
        return promptQuestion;
    }

    public void setPromptQuestion(String promptQuestion)
    {
        this.promptQuestion = promptQuestion;
    }

    public ArrayList<TroubleKeyword> getTags()
    {
        return tags;
    }

    public void setTags(ArrayList<TroubleKeyword> tags)
    {
        this.tags = tags;
    }

    public void addTag(TroubleKeyword newtag)
    {
        this.tags.add(newtag);
    }

    public String getRequirements()
    {
        return requirements;
    }

    public void setRequirements(String requirements)
    {
        this.requirements = requirements;
    }
}
