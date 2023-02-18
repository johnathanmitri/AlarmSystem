package com.johnathanmitri.alarmsystem;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ZoneEntryObj
{
    public int id;
    public String name;
    public boolean muted;
    public int state;
    public String userControl;
    public Date timeStamp;

    public ZoneEntryObj(int i, String n, boolean m, int s, String u)
    {
        id = i;
        name = n;
        muted = m;
        state = s;
        userControl = u;
    }

    public ZoneEntryObj(JSONObject obj) throws JSONException
    {
        id = obj.getInt("id");
        name = obj.getString("name");
        muted = obj.getBoolean("muted");
        state = obj.getInt("state");
        userControl = obj.getString("userControl");
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        try
        {
            timeStamp = isoFormat.parse(obj.getString("timeStamp"));
        }
        catch (ParseException e)
        {
            timeStamp = null;
        }
        //timeStamp = LocalDateTime.parse(obj.getString("timeStamp"), DateTimeFormatter.ISO_DATE_TIME);
    }
}