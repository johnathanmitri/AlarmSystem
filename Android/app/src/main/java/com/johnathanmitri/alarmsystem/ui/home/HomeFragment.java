package com.johnathanmitri.alarmsystem.ui.home;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.johnathanmitri.alarmsystem.MainActivity;
import com.johnathanmitri.alarmsystem.R;
import com.johnathanmitri.alarmsystem.WebsocketManager;
import com.johnathanmitri.alarmsystem.databinding.FragmentHomeBinding;
import com.johnathanmitri.alarmsystem.databinding.ZoneEntryBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private ArrayList<ZoneEntryObj> orderedZoneArray = new ArrayList<ZoneEntryObj>();


    private zoneListAdapter<ZoneEntryObj> adapter;

    public class ZoneEntryObj
    {
        public int id;
        public String name;
        public boolean muted;
        public int state;

        public ZoneEntryObj(int i, String n, boolean m, int s)
        {
            id = i;
            name = n;
            muted = m;
            state = s;
        }

        public ZoneEntryObj(JSONObject obj) throws JSONException
        {
            id = obj.getInt("id");
            name = obj.getString("name");
            muted = obj.getBoolean("muted");
            state = obj.getInt("state");
        }
    }

    private class zoneListAdapter<T> extends ArrayAdapter<T>
    {
        public zoneListAdapter(@NonNull Context context, int resource, ArrayList<T> arrayList)
        {
            super(context, resource, arrayList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            //Button newBtn = new Button(getContext());
            //newBtn.setText("Button " + position);
            //this.getItem(position);

            // zoneEntry = new CardView(getContext());
            /*zoneEntry.setCardBackgroundColor(Color.CYAN);

            TextView text = new TextView(getContext());
            text.setText((String)this.getItem(position));
            zoneEntry.addView(text);*/

            ZoneEntryBinding zoneEntryBinding = ZoneEntryBinding.inflate(getActivity().getLayoutInflater());
                    //getActivity().getLayoutInflater().inflate(R.layout.zone_entry, parent, false);
            FrameLayout zoneEntry = zoneEntryBinding.getRoot(); //(FrameLayout)getActivity().getLayoutInflater().inflate(R.layout.zone_entry, parent, false);

            ZoneEntryObj entryObj = (ZoneEntryObj)this.getItem(position);

            if (true)
            {
                zoneEntryBinding.actionButton.setVisibility(View.VISIBLE);
                zoneEntryBinding.actionButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        try
                        {
                            JSONObject jsonMsg = new JSONObject();
                            jsonMsg.put("intent", "zoneAction");
                            jsonMsg.put("id", entryObj.id);
                            WebsocketManager.send(jsonMsg.toString());
                        } catch (JSONException e) {}
                    }
                });
            }

            zoneEntryBinding.muteZoneSwitch.setChecked(entryObj.muted);
            zoneEntryBinding.muteZoneSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Toast.makeText(getActivity().getApplicationContext(), String.format("Notifications will%s be shown for %s on all devices.", isChecked? " no longer": "", entryObj.name), Toast.LENGTH_SHORT).show();
                    entryObj.muted = isChecked;
                    try
                    {
                        JSONObject jsonMsg = new JSONObject();
                        jsonMsg.put("intent", "muteZone");
                        jsonMsg.put("id", entryObj.id);
                        jsonMsg.put("value", isChecked);
                        WebsocketManager.send(jsonMsg.toString());
                    } catch (JSONException e) {}
                }
            });
            //TextView zoneName = (TextView)leftLinearLayout.getChildAt(0);
            //TextView zoneState = (TextView)rightLinearLayout.getChildAt(0);



            zoneEntryBinding.zoneName.setText(entryObj.name);
            if (entryObj.state == -1)
            {
                zoneEntryBinding.cardView.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.OfflineGrey));
                zoneEntryBinding.zoneState.setText("Offline");
                //set text offline
            }
            else if (entryObj.state == 0)
            {
                zoneEntryBinding.cardView.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.OpenRed));
                zoneEntryBinding.zoneState.setText("Open");
                //set text open
            }
            else
            {
                zoneEntryBinding.cardView.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.ClosedBlue));
                zoneEntryBinding.zoneState.setText("Closed");
            }


            return zoneEntry;
        }
    }

    public void updateZones(JSONArray jsonArray, int zoneId)
    {
        boolean wasNull = false;
        if (orderedZoneArray.isEmpty())
        {
            wasNull = true;
        }
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
            ListView listView = (ListView) binding.getRoot().findViewById(R.id.listView);
            if (wasNull)
            {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter = new zoneListAdapter(getContext(), android.R.layout.simple_list_item_1, orderedZoneArray);
                        listView.setAdapter(adapter);
                    }
                });
            }
            else   //we have already created the listView before
            {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //if (zoneId == -1)
                            adapter.notifyDataSetChanged();
                            /*
                        else
                        {
                            Log.d("one row updated", "one row updated");
                            int start = listView.getFirstVisiblePosition();
                            for(int i=start, j=listView.getLastVisiblePosition();i<=j;i++)
                                if(((ZoneEntryObj)listView.getItemAtPosition(i)).id == zoneId)
                                {
                                    View view = listView.getChildAt(i-start);
                                    adapter.getView(i, view, listView);
                                    break;
                                }
                        }*/

                    }
                });

            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }



    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        //listView.requestLayout();
/*
//This is bullshit stuff i wrote to create test cases
        String[] testArr = new String[]{"Front Door", "Side Door", "Kitchen Door","Other Door","Garage Door","Kitchen Window","Bedroom Window","Garage Overhead Door","other", "other","other","other","other", "other","other","other"};
        String[] jsonArray = new String[testArr.length];
        for (int i = 0; i < testArr.length; i++)
        {
            try
            {
                JSONObject obj = new JSONObject();
                obj.put("zoneName", testArr[i]);

                obj.put("state", Math.random() >= 0.9? "Open":"Closed");
                jsonArray[i] = obj.toString();
            }
            catch (Exception e)
            {
            }
        }
//endBullshit*/



        WebsocketManager.homeFragment = this;

        //ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, testArr);



        return root;
    }




    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}