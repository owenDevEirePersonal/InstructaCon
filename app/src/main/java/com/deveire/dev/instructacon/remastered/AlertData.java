package com.deveire.dev.instructacon.remastered;

import android.util.Log;

import java.io.Serializable;

/**
 * Created by owenryan on 08/09/2017.
 */

public class AlertData
{
    private String stationID;
    private String alertText;
    private boolean isActive;
    private String type;

    public AlertData(String stationID, String alertText, boolean isActive, String type)
    {
        this.stationID = stationID;
        this.alertText = alertText;
        this.isActive = isActive;
        this.type = type;
    }

    public AlertData(String serializedAlert)
    {
        String trimmedSerializedTag = serializedAlert.substring(1, serializedAlert.length() -1);
        for (String aFieldPair: trimmedSerializedTag.split(",,,"))
        {
            String[] aPair = aFieldPair.split(":::");
            switch (aPair[0])
            {
                case "stationID": this.stationID = aPair[1]; break;
                case "alertBody": this.alertText = aPair[1]; break;
                case "type": this.type = aPair[1]; break;
                case "isActive": if(aPair[1].matches("true")){this.isActive = true;} else {this.isActive = false;} break;
            }
        }
        Log.i("Alerts", "DeSerialized AlertData:" + this.getStationID() + " " + this.getAlertText() + " " + this.getType() + " " + this.isActive());
    }

    public String getStationID()
    {
        return stationID;
    }

    public void setStationID(String stationID)
    {
        this.stationID = stationID;
    }

    public String getAlertText()
    {
        return alertText;
    }

    public void setAlertText(String alertText)
    {
        this.alertText = alertText;
    }

    public boolean isActive()
    {
        return isActive;
    }

    public void setActive(boolean active)
    {
        isActive = active;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String serialize()
    {
        String serialized = "[";
        serialized += "stationID:::" + this.getStationID();
        serialized += ",,,";
        serialized += "alertBody:::" + this.getAlertText();
        serialized += ",,,";
        serialized += "type:::" + this.getType();
        serialized += ",,,";
        if(isActive())
        {
            serialized += "isActive:::" + "true";
        }
        else
        {
            serialized += "isActive:::" + "false";
        }
        serialized += "]";
        Log.i("Alerts", "Serialized as: " + serialized);
        return serialized;
    }

}
