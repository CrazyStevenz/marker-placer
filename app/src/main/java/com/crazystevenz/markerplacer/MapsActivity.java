package com.crazystevenz.markerplacer;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private List<Marker> mMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Source: https://sites.google.com/site/androidhowto/how-to-1/get-notified-when-location-changes
        // The minimum time (in miliseconds) the system will wait until checking if the location changed
        int minTime = 1000;
        // The minimum distance (in meters) traveled until you will be notified
        float minDistance = 1;
        // Create a new instance of the location listener
        MyLocationListener myLocListener = new MyLocationListener();
        // Get the location manager from the system
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Get the criteria you would like to use
        Criteria criteria = new Criteria();
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setSpeedRequired(false);
        // Get the best provider from the criteria specified, and false to say it can turn the provider on if it isn't already
        String bestProvider = locationManager.getBestProvider(criteria, false);
        // Request location updates
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
        ) {
            return;
        }
        locationManager.requestLocationUpdates(bestProvider, minTime, minDistance, myLocListener);
    }

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {

            if (location != null) {
                try {
                    LatLng newMarkerPosition = new LatLng(location.getLatitude(), location.getLongitude());
                    Geocoder geo = new Geocoder(getApplicationContext(), Locale.getDefault());
                    List<Address> addresses = geo.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                    // Keep the marker in a value so we can manipulate it later
                    Marker newMarker = mMap.addMarker(new MarkerOptions()
                            // Set position of the marker to the user's current location
                            .position(newMarkerPosition)

                            // Source: https://stackoverflow.com/questions/9270565/android-get-current-location-name
                            // Set the title of the marker to the full address of the location
                            .title(addresses.get(0).getAddressLine(0))
                    );

                    // Show its title after it's been created
                    newMarker.showInfoWindow();

                    // Add the new marker to the marker list so we can access it later
                    mMarkers.add(newMarker);
                    // Only store the last 5 markers
                    while (mMarkers.size() > 5) {
                        // Delete the marker from the map
                        mMarkers.get(0).remove();
                        // Remove it from the list
                        mMarkers.remove(0);
                    }

                    // Source: https://stackoverflow.com/questions/13932441/android-google-maps-v2-set-zoom-level-for-mylocation
                    // Move the camera to the user's location and zoom in
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newMarkerPosition, 15.0f));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onProviderDisabled(String arg0) {
        }

        @Override
        public void onProviderEnabled(String arg0) {
        }

        @Override
        public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
        }
    }
}