package com.example.gmapcurrentlocation;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Common {
    public static final String KEY_REQUESTING_LOCATION_UPDATE = "LocationUpdate";

    public static String getLocation(Location mLocation) {

        return mLocation==null?"UnKnown Location" : new StringBuilder()
                .append(mLocation.getLatitude())
                .append("/")
                .append(mLocation.getLatitude())
                .toString();
    }

    public static CharSequence getLocationTitle(MyBackgroundService myBackgroundService,Location mLocation) {
        String finaladdress;
        Address finalad;
        double lat = mLocation.getLatitude();
        double lng = mLocation.getLongitude();

        try {
            Geocoder geocoder = new Geocoder(myBackgroundService, Locale.ENGLISH);

            List<Address> ad = geocoder.getFromLocation(lat, lng, 10);
            finalad = ad.get(0);
            finaladdress = finalad.getAddressLine(0);
            return  String.format(finaladdress);
        } catch (IOException e) {
            e.printStackTrace();
        }





        return  String.format("your Location");

    }

    public static void setRequestLocationUpdates(Context context, boolean value) {

        PreferenceManager.
                getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_LOCATION_UPDATE,value)
                .apply();
    }

    public static boolean requestLocationUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_LOCATION_UPDATE,false);
    }
}
