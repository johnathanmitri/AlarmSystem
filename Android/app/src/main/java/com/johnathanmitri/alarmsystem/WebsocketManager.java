package com.johnathanmitri.alarmsystem;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.johnathanmitri.alarmsystem.ui.home.HomeFragment;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class WebsocketManager
{
    private static WebSocketClient wsClient;
    //private static MainActivity mainActivity;
    private static Context mainActivityContext;
    public static HomeFragment homeFragment;

    private static ArrayList<ZoneEntryObj> orderedZoneArray = new ArrayList<ZoneEntryObj>();

    static boolean registered;
    static int androidId;

    private static boolean open = false; // whether or not we want it to be open.
    private static boolean connected = false; // whether or not the websocket is actually connected to the server
    private static boolean authenticated = false; // whether or not we have authenticated with the server
    public static boolean initialized = false; // whether or not we have initialized with the server


    public static class connectAsyncTask extends AsyncTask<Context, Integer, Integer>
    {
        @Override
        protected Integer doInBackground(Context... contexts)
        {
            mainActivityContext = contexts[0];
            open = true;
            connect();
            return 0;
        }
    }

    public static boolean isOpen()
    {
        return open; // this is whether we WANT it open, not whether the websocket actually IS open.
    }

    public static void send(String msg)
    {
        if (wsClient.isOpen())
            wsClient.send(msg);
    }

    public static void connect()
    {
        close();

        SharedPreferences regPrefs = mainActivityContext.getSharedPreferences("RegPrefs", Context.MODE_PRIVATE);
        registered = regPrefs.getBoolean("isRegistered", false);  //default value is false if this value does not exist
        androidId = regPrefs.getInt("androidId", -1);

        String nameToUseLAN = "192.168.254.143";
        String nameToUseWAN = Auth.getWanName();
        List possibleNetworkNames = Auth.getNetworkNames();

        String myPublicIP;
        String resolvedHostname;

        WifiManager wifiManager = (WifiManager) mainActivityContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        String ssid = info.getSSID();

        if (possibleNetworkNames.contains(ssid))
        {
            Log.d("WE ARE ON LOCAL NETWORK", "WE ARE ON LOCAL NETWORK!");
            openWebSocket("ws://" + nameToUseLAN + ":3820", false);
        }
        else
        {
            Log.d("WE ARE NOT ON LOCAL NETWORK", "WE ARE NOT ON LOCAL NETWORK!");
            openWebSocket("wss://" + nameToUseWAN + ":3819", true);
        }
/*
        try (java.util.Scanner s = new java.util.Scanner(new java.net.URL("https://checkip.amazonaws.com/").openStream(), "UTF-8").useDelimiter("\\n|\\A"))
        {
            myPublicIP = s.next();
            Log.d("MY PUBLIC IP: ", "My current IP address is: " + myPublicIP);
        }
        catch (java.io.IOException e)
        {
            e.printStackTrace();
            myPublicIP = "err";
        }

        InetAddress address = null;
        try
        {
            address = InetAddress.getByName(nameToUseWAN);
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
        }
        resolvedHostname = address.getHostAddress();
        Log.d("DNS HOSTNAME: ", "Host name is: " + resolvedHostname);

        if (myPublicIP.equals(resolvedHostname))
        {
            Log.d("WE ARE ON LOCAL NETWORK", "WE ARE ON LOCAL NETWORK!");
            openWebSocket("ws://" + nameToUseLAN + ":3820", false);
        }
        else
        {
            Log.d("WE ARE NOT ON LOCAL NETWORK", "WE ARE NOT ON LOCAL NETWORK!");
            openWebSocket("wss://" + nameToUseWAN + ":3819", true);
        }
*/

    }

    public static void close()
    {
        open = false;
        initialized = false;
        authenticated = false;
        if (wsClient != null)
            wsClient.close();
    }

    private static int openWebSocket(String url, boolean isSecure)
    {
        try
        {
            wsClient = new WebSocketClient(new URI(url))
            {
                @Override
                public void onOpen(ServerHandshake handshakeData)
                {
                    if (wsClient.isOpen())
                    {
                        open = true;
                        homeFragment.onWebsocketOpened();
                        JSONObject authentication = Auth.getAuth(isSecure);
                        try
                        {

                            wsClient.send(authentication.toString());

                            if (registered) // if registered
                            {
                                JSONObject init = new JSONObject();
                                init.put("intent", "connect");
                                init.put("androidId", androidId);
                                wsClient.send(init.toString());
                            }
                            else
                            {
                                FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>()
                                {
                                    @Override
                                    public void onComplete(@NonNull Task<String> task)
                                    {
                                        try
                                        {
                                            JSONObject init = new JSONObject();
                                            init.put("intent", "register");
                                            init.put("fcmToken", task.getResult());

                                            String test = Settings.Secure.getString(mainActivityContext.getContentResolver(), "bluetooth_name");
                                            init.put("deviceName", test);

                                            init.put("notifsEnabled", ContextCompat.checkSelfPermission(mainActivityContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED);

                                            wsClient.send(init.toString());
                                        }
                                        catch (JSONException e)
                                        {
                                            e.printStackTrace();
                                        }
                                    }
                                });

                            }

                        }
                        catch (JSONException e)
                        {
                            e.printStackTrace();
                        }

                    }
                }

                @Override
                public void onMessage(String messageStr)
                {
                    if (initialized && registered && authenticated)
                    {
                        Log.d("Message received!", messageStr);
                        try
                        {
                            JSONObject jsonData = new JSONObject(messageStr);
                            if (jsonData.getString("intent").equals("updateZones"))
                            {

                                    int zoneId = -1;  //default value of -1 means that no zoneId was sent by server.
                                    if (jsonData.has("zoneIdUpdated"))
                                        zoneId = jsonData.getInt("zoneIdUpdated");


                                    updateZones(jsonData.getJSONArray("zones"), zoneId);

                            }
                            else if (jsonData.getString("intent").equals("getZoneEventsResult"))
                            {
                                if (homeFragment != null && homeFragment.isVisible())
                                {
                                    String encodedBuffer = jsonData.getString("eventsBuffer");
                                    byte[] decodedBytes = Base64.decode(encodedBuffer, Base64.DEFAULT);
                                    homeFragment.getActivity().runOnUiThread(new Runnable()
                                    {
                                        public void run()
                                        {
                                            homeFragment.showEventsWindow(decodedBytes);
                                        }
                                    });
                                    //jsonData.
                                    //homeFragment.updateZones();
                                }
                            }
                        }
                        catch (JSONException e)
                        {
                            e.printStackTrace();
                        }
                    }
                    else
                    {
                        try
                        {
                            JSONObject jsonData = new JSONObject(messageStr);
                            if (!jsonData.getString("status").equals("success")) //if it is not successful
                            {
                                Log.e("ERROR: " + jsonData.getString("intent"), jsonData.getString("status"));
                            }
                            else if (!authenticated && jsonData.getString("intent").equals("auth") && jsonData.getString("status").equals("success"))
                            {
                                authenticated = true;
                            }
                            else if (!registered && jsonData.getString("intent").equals("register") && jsonData.getString("status").equals("success"))
                            {
                                Log.d("REGISTERED!!!!", "Registered with id: " + jsonData.getInt("androidId"));
                                SharedPreferences regPrefs = mainActivityContext.getSharedPreferences("RegPrefs", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = regPrefs.edit();
                                editor.putBoolean("isRegistered", true);
                                editor.putInt("androidId", jsonData.getInt("androidId"));
                                editor.apply();

                                initialized = true;
                                registered = true;
                            }
                            else if (!initialized && jsonData.getString("intent").equals("connect") && jsonData.getString("status").equals("success"))
                            {
                                Log.d("CONNECTED!!!!", "Connected with id: " + jsonData.getInt("androidId"));
                                initialized = true;
                            }
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                            return;
                        }
                    }

                }

                @Override
                public void onClose(int code, String reason, boolean remote)
                {
                    connected = false;
                    homeFragment.onWebsocketClosed();

                    if (open)
                        WebsocketManager.connect();
                }

                @Override
                public void onError(Exception ex)
                {
                    //connected = false;
                    //homeFragment.onWebsocketClosed();
                    Log.e("WEBSOCKET ERROR (void)", "");
                    ex.printStackTrace();

                    //if (open)
                    //    WebsocketManager.connect();
                }
            };
            wsClient.connect();
        }
        catch (URISyntaxException e)
        {
            Log.e("ERROR CONNECTING", "");
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    private static void updateZones(JSONArray jsonArray, int zoneId )
    {
        /*boolean wasNull = false;
        if (orderedZoneArray.isEmpty())
        {
            wasNull = true;
        }*/
        orderedZoneArray.clear();

        JSONObject[] jsonObjArray = new JSONObject[jsonArray.length()];
        //orderedZoneArray = new ZoneEntryObj[jsonArray.length()];
        try
        {
            int orderedCount = 0;
            for (int i = 0; i < jsonObjArray.length; i++)
            {
                JSONObject obj = jsonArray.getJSONObject(i);
                jsonObjArray[i] = obj;
                if (obj.getInt("state") == 0)
                {
                    //orderedZoneArray[orderedCount] = new ZoneEntryObj(obj.getString("name"), 0);
                    orderedZoneArray.add(new ZoneEntryObj(obj));
                    orderedCount++;
                }
            }
            for (int i = 0; i < jsonObjArray.length; i++)
            {
                if (jsonObjArray[i].getInt("state") == -1)
                {
                    //orderedZoneArray[orderedCount] = new ZoneEntryObj(jsonObjArray[i].getString("name"), -1);
                    orderedZoneArray.add(new ZoneEntryObj(jsonObjArray[i]));
                    orderedCount++;
                }
            }
            for (int i = 0; i < jsonObjArray.length; i++)
            {
                if (jsonObjArray[i].getInt("state") == 1)
                {
                    //orderedZoneArray[orderedCount] = new ZoneEntryObj(jsonObjArray[i].getString("name"), 1);
                    orderedZoneArray.add(new ZoneEntryObj(jsonObjArray[i]));
                    orderedCount++;
                }

            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return;
        }

        if (homeFragment != null && homeFragment.isVisible())
        {
            homeFragment.updateZoneList(orderedZoneArray);
        }


    }

}