package com.deveire.dev.instructacon;

/**
 * Created by owenryan on 08/09/2017.
 */

public class AlertsRow
{
    private String stationID;
    private String alert;
    private boolean isActive;
    private String type;

    public AlertsRow(String stationID, String alert, boolean isActive, String type)
    {
        this.stationID = stationID;
        this.alert = alert;
        this.isActive = isActive;
        this.type = type;
    }

    public String getStationID()
    {
        return stationID;
    }

    public void setStationID(String stationID)
    {
        this.stationID = stationID;
    }

    public String getAlert()
    {
        return alert;
    }

    public void setAlert(String alert)
    {
        this.alert = alert;
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
}
