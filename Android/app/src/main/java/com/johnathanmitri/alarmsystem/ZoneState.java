package com.johnathanmitri.alarmsystem;

public class ZoneState
{
    public static int OFFLINE = -1;
    public static int OPEN = 0;
    public static int CLOSED = 1;

    public static String getStateStringPresentTense(int state)
    {
        switch (state)
        {
            case -1:
                return "Offline";
            case 0:
                return "Open";
            case 1:
                return "Closed";
        }
        return "Invalid Zone State";
    }
    public static String getStateStringPastTense(int state)
    {
        switch (state)
        {
            case -1:
                return "Offline";
            case 0:
                return "Opened";
            case 1:
                return "Closed";
        }
        return "Invalid Zone State";
    }
}
