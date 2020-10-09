package com.example.gmapcurrentlocation;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    Button requestionlocation,removelocation;
    MyBackgroundService  mService =null;
    boolean mBound = false;
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    private boolean connected1;
    private  final ServiceConnection mServiceConnection =new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

        MyBackgroundService.LocalBinder binder=(MyBackgroundService.LocalBinder)service;
        mService=binder.getService();
        mBound=true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

        mService =null;
        mBound=false;
    }
};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




        ConnectivityManager connectivityManager = (ConnectivityManager) MainActivity.this.getSystemService(MainActivity.this.CONNECTIVITY_SERVICE);
        if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            //we are connected to a network
            connected1 = true;
        } else {
            connected1 = false;
        }

        if (!connected1) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Turn on mobile data");
            builder.setMessage("To use this application you must need to data connection")
                    .setCancelable(false).setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final Intent intent = getIntent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    finish();
                    overridePendingTransition(0, 0);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                }
            })
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            MainActivity.this.finish();
                        }
                    });

            AlertDialog alert = builder.create();
            alert.show();
        }








        displayLocationSettingsRequest();




        Dexter.withActivity(this)
                .withPermissions(Arrays.asList(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) ).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                requestionlocation=   findViewById(R.id.updatelocation);
                        removelocation = findViewById(R.id.removeupdate);


                        requestionlocation.setOnClickListener(new View.OnClickListener() {
                            @Override
                                   public void onClick(View v) {
                                       mService.requestLocationUpdate();
                                   }
                               });
                               removelocation.setOnClickListener(new View.OnClickListener() {
                                   @Override
                                   public void onClick(View v) {
                                       mService.removeLocationUpdates();
                                   }
                               });
                               setButtonState(Common.requestLocationUpdates(MainActivity.this));
                               bindService(new Intent(MainActivity.this,
                                       MyBackgroundService.class),
                                       mServiceConnection,
                                       Context.BIND_AUTO_CREATE
                                       );
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

            }
        }).check();


      }

    @Override
    protected void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        if (mBound)
        {
            unbindService(mServiceConnection);
            mBound=false;
        }
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

        EventBus.getDefault().unregister(this);

        super.onStop();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Common.KEY_REQUESTING_LOCATION_UPDATE)) {

            setButtonState(sharedPreferences.getBoolean(Common.KEY_REQUESTING_LOCATION_UPDATE,false));

        }
    }

    private void setButtonState(boolean isRequstEnabled) {

        if (isRequstEnabled)
        {
            requestionlocation.setEnabled(false);
            removelocation.setEnabled(true);
        }
        else
        {
            requestionlocation.setEnabled(true);
            removelocation.setEnabled(false);
        }


    }



    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    public void onListenLocation(SendLocationToActivity event)
    {
        String finaladdress = null;
        Address finalad;
        double lat=1;
        double lng=2;
        if (event!=null)



            lat  = event.getLocation().getLatitude();
         lng =event.getLocation().getLongitude();

        try {
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.ENGLISH);

            List<Address> ad = geocoder.getFromLocation(lat, lng, 10);
            finalad = ad.get(0);
            finaladdress = finalad.getAddressLine(0);
        } catch (IOException e) {
            e.printStackTrace();
        }




        {
            String data = new StringBuilder()
                    .append(event.getLocation().getLatitude())
                    .append("/")
                    .append(event.getLocation().getLongitude())
                    .append("/Your add-")
                    .append(finaladdress)
                    .toString();

            Toast.makeText(mService, data, Toast.LENGTH_SHORT).show();

        }

    }



    public void displayLocationSettingsRequest (){
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(MainActivity.this)
                .addApi(LocationServices.API).build();
        googleApiClient.connect();

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000 / 2);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // Log.i(TAG, "All location settings are satisfied.");

                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult((Activity) MainActivity.this, REQUEST_CHECK_SETTINGS);

                        } catch (IntentSender.SendIntentException e) {
                            //  Log.i(TAG, "PendingIntent unable to execute request.");
                        }

                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        //  Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        //  Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to upgrade location settings ");

                        break;
                }
            }
        });

    }

}