package com.crazystevenz.markerplacer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, SensorEventListener {

    String[] COLORS = new String[] {"Red", "Green", "Blue", "Yellow", "Magenta"};
    private GoogleMap mMap;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private SensorManager sensorManager;
    private Sensor temp;
    private float currentTempInCelsius;
    private List<MyMarker> mMyMarkers = new ArrayList<>();
    private Marker mSelectedMarker;
    private View mOverlayView;
    private TextInputEditText mDescriptionTextView;
    private AutoCompleteTextView mColorAutoCompleteTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);

        // Get an instance of the sensor service, and use that to get an instance of
        // a particular sensor.
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        temp = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

        clearDb(new Runnable() {
            @Override
            public void run() {
                // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
                mapFragment.getMapAsync(MapsActivity.this);

                setupOverlay();
            }
        });
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
        mMap.setOnMarkerClickListener(this);
        requestLocationUpdates();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        showOverlay(marker);
        mSelectedMarker = marker;
        return false;
    }

    private void setupOverlay() {
        mOverlayView = findViewById(R.id.overlay);
        mDescriptionTextView = findViewById(R.id.descriptionTextInputEditText);
        mColorAutoCompleteTextView = findViewById(R.id.filled_exposed_dropdown);

        // Set the X in the overlay to hide it when clicked
        ImageButton closeImageButton = mOverlayView.findViewById(R.id.close_image_button);
        closeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideOverlay();
            }
        });

        // Disables editing of dropdown values
        mColorAutoCompleteTextView.setInputType(0);

        // Source: https://material.io/develop/android/components/menu/
        // Color dropdown
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        getApplicationContext(),
                        R.layout.dropdown_menu_popup_item,
                        COLORS);
        mColorAutoCompleteTextView.setAdapter(adapter);
        mColorAutoCompleteTextView.setText(COLORS[0], false);

        Button saveButton = mOverlayView.findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveToDb();
            }
        });
    }

    private void showOverlay(final Marker marker) {
        // Update the overlay's info
        TextView titleTextView = findViewById(R.id.titleTextView);
        titleTextView.setText(marker.getTitle());
        mDescriptionTextView.setText(marker.getSnippet());

        mOverlayView.setVisibility(View.VISIBLE);

        // Wait for the overlay to update
        mOverlayView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {

                // Move the map up so the overlay doesn't hide the selected marker
                mMap.setPadding(0, 0, 0, findViewById(R.id.overlay).getHeight());

                // Source: https://stackoverflow.com/questions/13932441/android-google-maps-v2-set-zoom-level-for-mylocation
                // Move the camera to the user's location and zoom in
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 15.0f));
            }
        });
    }

    private void hideOverlay() {
        mOverlayView.setVisibility(View.GONE);
        // Reset the map offset
        mMap.setPadding(0, 0, 0, 0);
    }

    public void saveToDb() {
        for (int i = 0; i < mMyMarkers.size(); i++) {
            MyMarker myMarker = mMyMarkers.get(i);
            if (myMarker.equals(mSelectedMarker)) {

                // Gather and update the marker info
                Editable description = mDescriptionTextView.getText();
                myMarker.getMarker().setSnippet(description != null ? description.toString() : "");
                myMarker.setColor(mColorAutoCompleteTextView.getText().toString());
                setColor(myMarker.getMarker(), myMarker.getColor());
                myMarker.setSensorReading(currentTempInCelsius);

                // Update the database
                myMarker.getRef().update(
                        "description", myMarker.getMarker().getSnippet(),
                        "color", myMarker.getColor(),
                        "sensorReading", myMarker.getSensorReading()
                );
                break;
            }
        }
    }

    private void clearDb(final Runnable callback) {
        db.collection("markers")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(final QuerySnapshot queryDocumentSnapshots) {
                        List<Task<?>> tasks = new ArrayList<>(queryDocumentSnapshots.getDocuments().size());

                        for (DocumentSnapshot ds : queryDocumentSnapshots.getDocuments()) {
                            tasks.add(ds.getReference().delete());
                        }

                        Tasks.whenAllComplete(tasks)
                                .addOnSuccessListener(new OnSuccessListener<List<Task<?>>>() {
                                    @Override
                                    public void onSuccess(List<Task<?>> tasks) {
                                        callback.run();
                                    }
                        });
                    }
                });
    }

    private void setColor(Marker marker, String color) {
        switch (color) {
            case "Red": marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                break;
            case "Green": marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                break;
            case "Blue": marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                break;
            case "Yellow": marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                break;
            case "Magenta": marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                break;
        }
    }

    private void requestLocationUpdates() {
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

    // Source: https://developer.android.com/guide/topics/sensors/sensors_environment#java
    @Override
    public void onSensorChanged(SensorEvent event) {
        currentTempInCelsius = event.values[0];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        // Register a listener for the sensor
        super.onResume();
        sensorManager.registerListener(this, temp, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        // Unregister the sensor when the activity pauses
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {

            if (location != null) {
                try {
                    LatLng newMarkerPosition = new LatLng(location.getLatitude(), location.getLongitude());
                    Geocoder geo = new Geocoder(getApplicationContext(), Locale.getDefault());
                    List<Address> addresses = geo.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                    // Set position of the marker to the user's current location
                    MarkerOptions markerOptions = new MarkerOptions().position(newMarkerPosition);

                    // Keep the marker in a value so we can manipulate it later
                    Marker newMarker;
                    if (addresses.isEmpty()) {
                        newMarker = mMap.addMarker(markerOptions.title("No address name found"));
                    } else {
                        newMarker = mMap.addMarker(markerOptions
                                // Source: https://stackoverflow.com/questions/9270565/android-get-current-location-name
                                // Set the title of the marker to the full address of the location
                                .title(addresses.get(0).getAddressLine(0))
                        );
                    }

                    // Display the new marker's info
                    newMarker.showInfoWindow();
                    showOverlay(newMarker);

                    MyMarker myMarker = new MyMarker(newMarker);

                    // Add the new marker to the marker list so we can access it later
                    mMyMarkers.add(myMarker);
                    // Only store the last 5 markers
                    while (mMyMarkers.size() > 5) {
                        // Source: https://stackoverflow.com/questions/13692398/remove-a-marker-from-a-googlemap
                        // Delete the marker from the map
                        mMyMarkers.get(0).getMarker().remove();
                        // Remove it from the list
                        mMyMarkers.remove(0);
                    }

                    // Save marker info to Firebase
                    addToDb(myMarker);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onProviderDisabled(String arg0) {}

        @Override
        public void onProviderEnabled(String arg0) {}

        @Override
        public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}

        private void addToDb(MyMarker myMarker) {
            // Create a new marker database entry
            Map<String, Object> dbEntry = new HashMap<>();
            dbEntry.put("position", myMarker.getMarker().getPosition());
            dbEntry.put("description", "");
            dbEntry.put("color", COLORS[0]);

            // Source: https://firebase.google.com/docs/firestore/manage-data/add-data#java_14
            // Create a new document reference with an auto-generated ID
            DocumentReference newMarkerRef = db.collection("markers").document();

            // Add the data to the database using the reference
            newMarkerRef.set(dbEntry)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(getApplicationContext(),
                                    "DocumentSnapshot added successfully ",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext(),
                                    "Error adding document: " + e.getMessage(),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });

            // Add the new reference to the reference list so we can access it later
            myMarker.setRef(newMarkerRef);
            // Only store the last 5 references
            while (mMyMarkers.size() > 5) {
                // Delete the reference from the database
                mMyMarkers.get(0).getRef().delete();
                // Remove it from the list
                mMyMarkers.remove(0);
            }
        }
    }
}