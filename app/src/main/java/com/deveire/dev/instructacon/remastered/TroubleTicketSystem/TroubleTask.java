package com.deveire.dev.instructacon.remastered.TroubleTicketSystem;

import java.util.ArrayList;

/**
 * Created by owenryan on 09/07/2018.
 */

public class TroubleTask
{
    private String Description; //Description of the Task
    private String promptQuestion; //The question asked when determining if this is the intended task
    private ArrayList<TroubleKeyword> tags; //List of tags(keywords) associated with this task, e.g. Sink, Plumbing, Pipe, Water Leak
    private String requirements;    //A description of the materials and tools required to complete the task
    private int promptImageID; //id of the image resource to be displayed when the promptQuestion is asked
    private int troubleTicketImageID; //id of the image resource to be displayed when this task has been determined to be correct and the Description is being read out
    private String employeeTypeToResolve; //the string identifier of the type of employee who is responsible for performing this task
    private float estimatedDuration; //expressed in minutes, the estimated time the task should take to complete. A value of -1 means the duration is unknown.


    public TroubleTask(String description, String promptQuestion, ArrayList<TroubleKeyword> tags, String requirements, int promptImageIDin, int troubleTicketImageID)
    {
        this.Description = description;
        this.promptQuestion = promptQuestion;
        this.tags = tags;
        this.requirements = requirements;
        this.promptImageID = promptImageIDin;
        this.troubleTicketImageID = troubleTicketImageID;
        this.employeeTypeToResolve = "none";
        this.estimatedDuration = -1;
    }

    public TroubleTask(String description, String promptQuestion, ArrayList<TroubleKeyword> tags, String requirements, int promptImageIDin, int troubleTicketImageID, String employeeTypeToResolve, int estimatedDuration)
    {
        this.Description = description;
        this.promptQuestion = promptQuestion;
        this.tags = tags;
        this.requirements = requirements;
        this.promptImageID = promptImageIDin;
        this.troubleTicketImageID = troubleTicketImageID;
        this.employeeTypeToResolve = employeeTypeToResolve;
        this.estimatedDuration = estimatedDuration;
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

    public String getRequirements() { return requirements; }

    public void setRequirements(String requirements)
    {
        this.requirements = requirements;
    }

    public int getPromptImageID() {return promptImageID; }

    public void setPromptImageID(int promptImageID) {this.promptImageID = promptImageID;}

    public int getTroubleTicketImageID()
    {
        return troubleTicketImageID;
    }

    public void setTroubleTicketImageID(int troubleTicketImageID)
    {
        this.troubleTicketImageID = troubleTicketImageID;
    }

    public String getEmployeeTypeToResolve()
    {
        return employeeTypeToResolve;
    }

    public void setEmployeeTypeToResolve(String employeeTypeToResolve)
    {
        this.employeeTypeToResolve = employeeTypeToResolve;
    }

    public float getEstimatedDuration()
    {
        return estimatedDuration;
    }

    public void setEstimatedDuration(float estimatedDuration)
    {
        this.estimatedDuration = estimatedDuration;
    }
}
