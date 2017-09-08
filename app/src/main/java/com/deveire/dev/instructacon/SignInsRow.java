package com.deveire.dev.instructacon;

import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by owenryan on 08/09/2017.
 */

public class SignInsRow
{
    String stationID;
    String tagID;
    Date timestamp;

    public SignInsRow(String stationID, String tagID, String timestamp)
    {
        this.stationID = stationID;
        this.tagID = tagID;
        setTimestamp(timestamp);
    }

    public String getStationID()
    {
        return stationID;
    }

    public void setStationID(String stationID)
    {
        this.stationID = stationID;
    }

    public String getTagID()
    {
        return tagID;
    }

    public void setTagID(String tagID)
    {
        this.tagID = tagID;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(String timestamp)
    {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try
        {
            this.timestamp = dateFormat.parse(timestamp);
        }
        catch (ParseException e)
        {
            Log.e("offline Load", "ParseException in SignInsRow: " + e.toString() + "\n Using timestamp:" + timestamp);
        }
    }

    public void setTimestamp(Date timestamp)
    {
        this.timestamp = timestamp;
    }
}
