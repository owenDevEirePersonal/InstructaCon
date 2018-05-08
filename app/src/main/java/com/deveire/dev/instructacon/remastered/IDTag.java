package com.deveire.dev.instructacon.remastered;

import android.util.Log;

/**
 * Created by owenryan on 08/09/2017.
 */

public class IDTag
{
    public static final String tagtype_UNDEFINED_TAG = "Undefined";
    public static final String tagtype_JANITOR = "Janitor";
    public static final String tagtype_SECURITY = "Security";
    public static final String tagtype_TECHNICIAN_CLASS_1 = "Technician Class 1";
    public static final String tagtype_TECHNICIAN_CLASS_2 = "Technician Class 2";

    private String name;
    private String tagID;
    private String type;
    private boolean isOnJob;

    public IDTag(String name, String tagID, String type)
    {
        this.name = name;
        this.tagID = tagID;
        this.type = type;
        this.isOnJob = false;
    }

    public IDTag(String serializedTag)
    {
        String trimmedSerializedTag = serializedTag.substring(1, serializedTag.length() -1);
        for (String aFieldPair: trimmedSerializedTag.split(",,,"))
        {
            String[] aPair = aFieldPair.split(":::");
            switch (aPair[0])
            {
                case "name": this.name = aPair[1]; break;
                case "tagID": this.tagID = aPair[1]; break;
                case "type": this.type = aPair[1]; break;
                case "isOnJob": if(aPair[1].matches("true")){this.isOnJob = true;} else {this.isOnJob = false;} break;
            }
        }
        Log.i("Tags", "DeSerialized TagsRow:" + this.name + " " + this.tagID + " " + this.type + " " + this.isOnJob);
    }

    public IDTag()
    {
        this.name = "Undefined Name";
        this.tagID = tagtype_UNDEFINED_TAG;
        this.tagID = "Undefined tag ID";
        this.isOnJob = false;
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

    public boolean isOnJob()
    {
        return this.isOnJob;
    }

    public void setIsOnJob(boolean onJob)
    {
        this.isOnJob = onJob;
        Log.i("OnTheJob", "TagsRow:" + tagID + " " + name + " " + type + " OnJob is now: " + isOnJob);
    }

    public String serializeTag()
    {
        String serialized = "[";
        serialized += "name:::" + this.getName();
        serialized += ",,,";
        serialized += "tagID:::" + this.getTagID();
        serialized += ",,,";
        serialized += "type:::" + this.getType();
        serialized += ",,,";
        if(isOnJob())
        {
            serialized += "isOnJob:::" + "true";
        }
        else
        {
            serialized += "isOnJob:::" + "false";
        }
        serialized += "]";
        Log.i("Tags", "Serialized as: " + serialized);
        return serialized;
    }

}