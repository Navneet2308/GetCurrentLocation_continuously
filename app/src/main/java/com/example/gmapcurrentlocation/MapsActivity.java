package com.example.gmapcurrentlocation;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    ImageView cureentlocation;
    CameraUpdate cameraUpdate = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        cureentlocation=findViewById(R.id.cureentlocation);
        mapFragment.getMapAsync(this);


    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(24.2658, 80.7624);

        mMap.addMarker(new MarkerOptions().position(sydney).title("MAiahr"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));





        cureentlocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                    LatLng latLng = new LatLng(23.2599,77.4126);
                    cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
                mMap.addMarker(new MarkerOptions().position(latLng).title("my Location"));
                    googleMap.animateCamera(cameraUpdate);


            }
        });





    }



}