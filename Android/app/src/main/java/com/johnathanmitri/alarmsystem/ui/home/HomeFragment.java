package com.johnathanmitri.alarmsystem.ui.home;

import static com.johnathanmitri.alarmsystem.WebsocketManager.homeFragment;

import android.app.AlertDialog;
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
import com.johnathanmitri.alarmsystem.databinding.ZoneEventsPopupBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
    final SimpleDateFormat dateFormat = new SimpleDateFormat("M-dd-yyyy' at 'h:mm aa");;//SimpleDateFormat("h:mm aa");
    SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private zoneListAdapter<ZoneEntryObj> adapter;

    private static ArrayList<ZoneEntryObj> listViewSource = new ArrayList<ZoneEntryObj>();

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

    public void updateZoneList(ArrayList<ZoneEntryObj> orderedZoneArray)//JSONArray jsonArray, int zoneId)
    {
        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run()
            {
                listViewSource.clear();
                listViewSource.addAll(orderedZoneArray);
                refreshListView();
                // listView = ;

            }});
    }

    private void refreshListView()
    {
        if (adapter == null)
        {

            adapter = new zoneListAdapter(getContext(), android.R.layout.simple_list_item_1, listViewSource);
            binding.listView.setAdapter(adapter);
        }
        else   //we have already created the listView before
        {

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
    }

    public void onWebsocketOpened()
    {
        homeFragment.getActivity().runOnUiThread(new Runnable()
        {
            public void run()
            {
                binding.disconnectedOverlay.setVisibility(View.GONE);
            }
        });
    }

    public void onWebsocketClosed()
    {
        homeFragment.getActivity().runOnUiThread(new Runnable()
        {
            public void run()
            {
                binding.disconnectedOverlay.setVisibility(View.VISIBLE);
            }
        });
    }

    public void showEventsWindow(byte[] eventBytes)
    {
        final int EVENT_SIZE = 9; //each event is 9 bytes long

        ByteBuffer eventBuffer = ByteBuffer.wrap(eventBytes);
        eventBuffer.order(ByteOrder.LITTLE_ENDIAN);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        //View zoneEventsPopupView = getLayoutInflater().inflate(R.layout.zone_events_popup, null);
        ZoneEventsPopupBinding zoneEventsPopupBinding = ZoneEventsPopupBinding.inflate(getLayoutInflater());

        String text = "";

        //try
        //{
            /*
            for (int i = jsonArray.length() -1; i >= 0; i--)
            {
                JSONObject event = jsonArray.getJSONObject(i);*/
                //text += event.getString("name");
            //for (int i = eventBytes.length - EVENT_SIZE; i >= 0; i -= EVENT_SIZE)  //start at the last event and iterate backwards.
            //{
            while (eventBuffer.remaining() >= 9)
            {
                //JSONObject event = jsonArray.getJSONObject(i);
                //switch (event.getInt("state")
                String line = "";
                switch (eventBuffer.get()) // the first byte is the state, the next 8 is the timestamp
                {
                    case -1:
                        line += "Offline at ";
                        break;
                    case 0:
                        line += "Opened at ";
                        break;
                    case 1:
                        line += "Closed at ";
                        break;
                }

                Date time;
                try
                {
                    //time = isoFormat.parse(event.getString("timeStamp"))
                    long test = eventBuffer.getLong();
                    time = new Date(test);

                }
                catch (Exception e)
                {
                    time = null;
                }

                line += dateFormat.format(time) + "\n";
                text = line + text; //push them to the top, in order to reverse the list.
            }
        //}
        //catch (JSONException e) {}

        zoneEventsPopupBinding.textView2.setText(text);

        dialogBuilder.setView(zoneEventsPopupBinding.getRoot());

        AlertDialog dialog = dialogBuilder.create();

        dialog.show();

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



        homeFragment = this;

        //ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, testArr);



        return root;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        binding.disconnectedOverlay.setVisibility(WebsocketManager.isOpen() ? View.GONE : View.VISIBLE);

        if (WebsocketManager.isOpen())
            refreshListView();
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}