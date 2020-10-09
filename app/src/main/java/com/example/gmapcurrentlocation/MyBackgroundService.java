package com.example.gmapcurrentlocation;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.greenrobot.eventbus.EventBus;

import java.security.Provider;

public class MyBackgroundService extends Service {
    private static final String CHANNEL_ID = "mu_channel";

    private static final long UPDATE_INTERVAL_IN_MIL = 15000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MUL = UPDATE_INTERVAL_IN_MIL / 2;
    private static final int NOTI_ID = 1223;
    private static final String EXTRA_STARTED_FROM_NOTIFICATION = "com.example.gmapcurrentlocation" +
            ".started_from_notification";
    private boolean mChangingConfiguration = false;
    private NotificationManager mNotificationManager;

    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Handler mServicehander;
    private Location mLocation;


    public MyBackgroundService() {

    }

    @Override
    public void onCreate() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());

            }
        };

        createLocationRequest();
        getLcatLocation();

        HandlerThread handlerThread=new HandlerThread("Navneet");
        handlerThread.start();
        mServicehander = new Handler(handlerThread.getLooper());
        mNotificationManager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel mChannel=new NotificationChannel(CHANNEL_ID,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT);

            mNotificationManager.createNotificationChannel(mChannel);
        }

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION,false);
        if (startedFromNotification)
        {

            removeLocationUpdates();
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration=true;
    }

    public void removeLocationUpdates() {
        try{

            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            Common.setRequestLocationUpdates(this,false);
            stopSelf();

        } catch (SecurityException e) {
            Common.setRequestLocationUpdates(this,true);
            Log.e("Mrssage",e.getMessage());
        }
    }

    private void getLcatLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            fusedLocationProviderClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {

                            if (task.isSuccessful() && task.getResult() != null)
                            {
                                mLocation = task.getResult();

                            }

                            else
                            {
                                Log.e("Message","Failed to get Location");

                            }

                        }
                    });
        } catch (SecurityException ex) {
            Log.e("Message","Loast Location permission...",ex);
        }

    }

    private void createLocationRequest() {
        locationRequest=new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MIL);

        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }

    private void onNewLocation(Location lastLocation) {
        mLocation = lastLocation;
        EventBus.getDefault().postSticky(new SendLocationToActivity(mLocation));


        //update notification content if running as a foreground service

        if (serviceisRunningInForeGround(this))
        {
            mNotificationManager.notify(NOTI_ID, (Notification) getNotification());

        }



    }

    private Object getNotification() {
        Intent intent=new Intent(this,MyBackgroundService.class);
        String text=Common.getLocation(mLocation);

        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION,true);
        PendingIntent servicePendingIntent = PendingIntent.getService(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent activityPendingIntent =PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),0);

        NotificationCompat.Builder builder=new NotificationCompat.Builder(this)
                .addAction(R.drawable.ic_baseline_location_searching_24,"Launch",activityPendingIntent)
                .addAction(R.drawable.ic_baseline_close_24,"Remove",servicePendingIntent)
                .setContentText(text)
                .setContentTitle(Common.getLocationTitle(this,mLocation))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(text)
                .setWhen(System.currentTimeMillis());

        //here set the channel id for Android o

        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O)
        {

            builder.setChannelId(CHANNEL_ID);

        }

        return  builder.build();


    }

    private boolean serviceisRunningInForeGround(Context context) {
        ActivityManager manager=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service:manager.getRunningServices(Integer.MAX_VALUE))
            if (getClass().getName().equals(service.service.getClassName()))
                if (service.foreground)
                    return true;
        return false;
    }

    public void requestLocationUpdate() {
        Common.setRequestLocationUpdates(this,true);
        startService(new Intent(getApplicationContext(),MyBackgroundService.class));

         try {
             fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper());


         } catch (SecurityException e) {
             e.printStackTrace();
         }

    }

    public class LocalBinder extends Binder {
        MyBackgroundService getService() {
            return MyBackgroundService.this;
        }
    }

    private final  IBinder mBinder = new LocalBinder();



    public IBinder onBind(Intent intent) {
        stopForeground(true);
        mChangingConfiguration= false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        stopForeground(true);
        mChangingConfiguration=false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (!mChangingConfiguration && Common.requestLocationUpdates(this))
            startForeground(NOTI_ID, (Notification) getNotification());
        return true;
    }


    @Override
    public void onDestroy() {
        mServicehander.removeCallbacks(null);
        super.onDestroy();
    }
}
