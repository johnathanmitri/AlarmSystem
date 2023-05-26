package com.johnathanmitri.alarmsystem;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ZoneEventObj
{
    public int state;
    public Date timeStamp;

    public ZoneEventObj(int state, Long timestampMillis)
    {
        this.state = state;
        this.timeStamp = new Date(timestampMillis);
    }
}