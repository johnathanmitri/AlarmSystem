package com.johnathanmitri.alarmsystem.ui.home;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
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
import com.johnathanmitri.alarmsystem.ZoneEntryObj;
import com.johnathanmitri.alarmsystem.databinding.FragmentHomeBinding;
import com.johnathanmitri.alarmsystem.databinding.ZoneEntryBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;



    //final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat., Locale.ENGLISH);
    final SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm aa");

    private zoneListAdapter<ZoneEntryObj> adapter;



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

            //zoneEntryBinding.cardView.context

            zoneEntryBinding.cardView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener()
            {
                @Override
                public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
                {
                   // super.onCreateContextMenu(menu, v, menuInfo);
                    // you can set menu header with title icon etc
                    //menu.setHeaderTitle("Zone");
                    // add menu items
                    MenuItem zoneHistory = menu.add(0, v.getId(), 0, "View Zone History");
                    zoneHistory.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
                    {
                        @Override
                        public boolean onMenuItemClick(@NonNull MenuItem item)
                        {
                            try
                            {
                                JSONObject jsonMsg = new JSONObject();
                                jsonMsg.put("intent", "getZoneEvents");
                                jsonMsg.put("id", entryObj.id);
                                WebsocketManager.send(jsonMsg.toString());

                            } catch (JSONException e) {}
                            return true;
                        }
                    });
                    //menu.add(0, v.getId(), 0, "Gray");
                    //menu.add(0, v.getId(), 0, "Cyan");
                }
            });

            /*zoneEntryBinding.cardView.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v)
                {
                    Log.e("LONG CLICK", "LONG CLICK DETECTED");
                    return true;
                }
            });*/

            if (entryObj.userControl.equals("GarageDoor"))
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

            if (entryObj.timeStamp != null)
            {
                zoneEntryBinding.timeStamp.setText("Since " +  dateFormat.format(entryObj.timeStamp));
            }
            else
            {
                zoneEntryBinding.timeStamp.setText("Timestamp Error");
            }

            return zoneEntry;
        }
    }

    public void updateZones()//JSONArray jsonArray, int zoneId)
    {
            ListView listView = (ListView) binding.getRoot().findViewById(R.id.listView);
            if (adapter == null)
            {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter = new zoneListAdapter(getContext(), android.R.layout.simple_list_item_1, WebsocketManager.orderedZoneArray);
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
                        //adapter = new zoneListAdapter(getContext(), android.R.layout.simple_list_item_1, orderedZoneArray);
                        //listView.setAdapter(adapter);
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



    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        adapter = null;
        //Toast.makeText(getActivity().getApplicationContext(), "FRAGMENT CREATED", Toast.LENGTH_SHORT).show();

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
    public void onResume()
    {
        super.onResume();
        //adapter = new zoneListAdapter(getContext(), android.R.layout.simple_list_item_1, orderedZoneArray);
        //((ListView)binding.getRoot().findViewById(R.id.listView)).setAdapter(adapter);
        //adapter = new zoneListAdapter(getContext(), android.R.layout.simple_list_item_1, orderedZoneArray);
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}