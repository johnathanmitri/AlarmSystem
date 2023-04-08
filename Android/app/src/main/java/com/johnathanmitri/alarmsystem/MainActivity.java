package com.johnathanmitri.alarmsystem;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.johnathanmitri.alarmsystem.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity
{
    //public WebSocket ws = null;
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    // Declare the launcher at the top of your Activity/Fragment:
    private final ActivityResultLauncher<String> notificationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted ->
            {
                if (isGranted)
                {
                    //TODO: IF USER NEWLY GRANTS PERMISSION, WE NEED TO UPDATE SERVER RECORDS, AND ENABLE NOTIFICATIONS!
                }
                else
                    Toast.makeText(getApplicationContext(), "Notifications will not be shown.", Toast.LENGTH_SHORT).show();
                (new WebsocketManager.connectAsyncTask()).execute(this);
            });

    private final ActivityResultLauncher<String> locationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted ->
    {
        if (isGranted)
        {
            //TODO: ?
        }
        else
            Toast.makeText(getApplicationContext(), "Please grant permission. App needs permission in order to check if device is connected to Local Network.", Toast.LENGTH_SHORT).show();
    });

    private void ensureLocationEnabled()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }


    private void ensureNotificationsEnabled()
    {
        // "Creating an existing notification channel with its original values performs no operation, so it's safe to call this code when starting an app."
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            CharSequence name = "Alarm messages";
            String description = "This shows when bad guys attempt to break in";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("AlarmChannel", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS); //websocket is connected in a callback function after user answers the permission prompt
            }
            else
            {
                (new WebsocketManager.connectAsyncTask()).execute(this); //if we already have permission, then just connect. nothing to wait for.
            }
        }
        else
        {
            (new WebsocketManager.connectAsyncTask()).execute(this); //if we dont need to request permission, then just connect. nothing to wait for.
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {

                // Do something in response to button click
                if (WebsocketManager.initialized)
                {
                    WebsocketManager.send("Message from Android!");
                }

                //Toast.makeText(MainActivity.this, "i dont do shit", Toast.LENGTH_SHORT).show();
            }
        });
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        ensureLocationEnabled();
        ensureNotificationsEnabled();  //this also calls WebsocketManager.connect() after checking notification permissions and stuff
    }

    @Override
    protected void onStop()
    {
        WebsocketManager.close();
        super.onStop();
    }

    @Override
    protected void onResume()
    {
        if (!WebsocketManager.isOpen())
            (new WebsocketManager.connectAsyncTask()).execute(this);
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp()
    {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

}



