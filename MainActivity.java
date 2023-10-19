package com.example.gpsapp;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.FEATURE_SCREEN_LANDSCAPE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    LocationManager locationManager;
    LocationListener locationListener;
    Geocoder geocoder;
    List<Address> addresses;
    ArrayList<Location> locations;
    double dist;
    TextView latLon, address, distance;
    Instant starts, ends;
    JSONObject json;
    int count;
    long getTime;
    SupportMapFragment supportMapFragment;
    GoogleMap googleMap1;
    Spinner spinner;
    ArrayAdapter<String> adapter;
    ArrayList<String> strings;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getTime = 0;
        json = new JSONObject();
        latLon = findViewById(R.id.location);
        address = findViewById(R.id.addressText);
        distance = findViewById(R.id.distText);
        spinner = findViewById(R.id.spinner);

        latLon.setText("UPDATING");
        address.setText("UPDATING");
        distance.setText("UPDATING");

        dist = 0.0;
        count = 0;

        strings = new ArrayList<>();

        if (savedInstanceState != null) {
            dist = savedInstanceState.getDouble("dist");
            getTime = savedInstanceState.getLong("getTime");
            count = savedInstanceState.getInt("count");
            strings = savedInstanceState.getStringArrayList("strings");
            updateAdapter();
        } else {
            onRequestPermissionsResult(PERMISSION_GRANTED, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION}, new int[]{PERMISSION_GRANTED});
        }

        starts = Instant.now();
        locations = new ArrayList<>();
        geocoder = new Geocoder(getApplicationContext(), new Locale("US"));
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);

        supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        locationListener = new LocationListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onLocationChanged(@NonNull Location location) {
                latLon.setText("Latitude: " + location.getLatitude() + " \nLongitude: " + location.getLongitude());

                try {
                    addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (!addressToString(addresses.get(addresses.size() - 1)).equals(address.getText().toString()) && !address.getText().toString().equals("UPDATING")) {
                        count++;
                        ends = Instant.now();
                        if (count > 3) {
                            json.remove("address" + (count - 2));
                            json.remove("time" + (count - 2));
                        }
                        json.put("address" + count, address.getText().toString().substring(0, address.getText().toString().indexOf(",")));
                        json.put("time" + count, (Duration.between(starts, ends).toNanos() + getTime) / 1000000000);
                        starts = Instant.now();
                        strings.add(json.getString("address" + count) + ", " + json.get("time" + count));
                        updateAdapter();
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }

                if(addresses != null){
                    address.setText(addressToString(addresses.get(addresses.size() - 1)) + "");
                    updateMap(location.getLatitude(), location.getLongitude());
                }

                if (locations.size() == 0) {
                    locations.add(location);
                } else if (locations.size() == 1) {
                    if (location != locations.get(0)) {
                        locations.add(location);
                        dist += location.distanceTo(locations.get(locations.size() - 2));
                    }
                } else {
                    if (locations.get(locations.size() - 1) != locations.get(locations.size() - 2)) {
                        locations.add(location);
                        dist += locations.get(locations.size() - 2).distanceTo(location);
                    }
                }
                distance.setText(dist + "");
            }
        };

        while (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            continue;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 0, locationListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] value, int[] permissions) {

        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, value, permissions[0]);
            super.onRequestPermissionsResult(requestCode, value, permissions);
        }
    }

    public String addressToString(Address address) {
        return address.getAddressLine(0);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble("dist", dist);
        getTime = starts.getNano();
        outState.putLong("getTime", getTime);
        outState.putInt("count", count);
        outState.putStringArrayList("strings", strings);

        supportMapFragment.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(locationListener);
        supportMapFragment.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        supportMapFragment.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        supportMapFragment.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        supportMapFragment.onResume();
    }

    public void updateMap(double lat, double lon) {
        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                googleMap1 = googleMap;
                googleMap1.setBuildingsEnabled(true);
                googleMap1.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 18.0F));
                googleMap1.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title(addressToString(addresses.get(addresses.size()-1)))).showInfoWindow();
            }
        });

    }

    public void updateAdapter() {
        adapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, strings);
        spinner.setAdapter(adapter);
    }

}