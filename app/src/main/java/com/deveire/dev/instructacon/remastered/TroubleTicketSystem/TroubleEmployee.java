package com.deveire.dev.instructacon.remastered.TroubleTicketSystem;

/**
 * Created by owenryan on 14/08/2018.
 */

public class TroubleEmployee
{
    private String name;
    private String tagUUID;
    private String type;

    public TroubleEmployee(String name, String tagUUID, String type)
    {
        this.name = name;
        this.tagUUID = tagUUID;
        this.type = type;
    }

    public TroubleEmployee(String tagUUID)
    {
        this.name = "Unknown Employee";
        this.tagUUID = tagUUID;
        this.type = "Unknown";
    }

    public TroubleEmployee()
    {
        this.name = "";
        this.tagUUID = "";
        this.type = "None";
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getTagUUID()
    {
        return tagUUID;
    }

    public void setTagUUID(String tagUUID)
    {
        this.tagUUID = tagUUID;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }
}
