package com.deveire.dev.instructacon;

/**
 * Created by owenryan on 08/09/2017.
 */

public class TagsRow
{
    private String name;
    private String tagID;
    private String type;

    public TagsRow(String name, String tagID, String type)
    {
        this.name = name;
        this.tagID = tagID;
        this.type = type;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getTagID()
    {
        return tagID;
    }

    public void setTagID(String tagID)
    {
        this.tagID = tagID;
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
