package com.johnathanmitri.alarmsystem;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.johnathanmitri.alarmsystem.ui.home.HomeFragment;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

public class WebsocketManager
{
    private static WebSocketClient wsClient;
    //private static MainActivity mainActivity;
    private static Context mainActivityContext;
    public static HomeFragment homeFragment;

    static boolean registered;
    static int androidId;

    public static boolean open = false;
    public static boolean initialized = false;
    static boolean authenticated = false;

    public static class connectAsyncTask extends AsyncTask<Context, Integer, Integer>
    {
        @Override
        protected Integer doInBackground(Context... contexts)
        {
            mainActivityContext = contexts[0];
            connect();
            return 0;
        }
    }

    public static void send(String msg)
    {
        wsClient.send(msg);
    }

    public static void connect()
    {
        open = true;
        initialized = false;
        authenticated = false;

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
        String ssid  = info.getSSID();

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

    public static void closeWebSocket()
    {
        wsClient.close();
        open = false;
        initialized = false;
        authenticated = false;
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
                                if (homeFragment != null && homeFragment.isVisible())
                                {
                                    int zoneId = -1;  //default value of -1 means that no zoneId was sent by server.
                                    if (jsonData.has("zoneIdUpdated"))
                                        zoneId = jsonData.getInt("zoneIdUpdated");
                                    homeFragment.updateZones(jsonData.getJSONArray("zones"), zoneId);
                                }
                            }
                        }
                        catch (Exception e)
                        {

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

                }

                @Override
                public void onError(Exception ex)
                {
                    Log.e("ERROR CONNECTING (void)", "");
                    ex.printStackTrace();
                }
            };
            wsClient.connect();
        }
        catch (Exception e)
        {
            Log.e("ERROR CONNECTING", "");
            e.printStackTrace();
            return -1;
        }
        return 0;
    }
}