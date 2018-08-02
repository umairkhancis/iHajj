package com.hajjhackathon.ihajj.activity;

import static com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_BLUE;
import static com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN;
import static com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED;
import static com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_VIOLET;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hajjhackathon.ihajj.model.LocationPingModel;
import com.hajjhackthon.ihajj.R;
import java.util.*;

public class LocationReceiverActivity extends AppCompatActivity implements OnMapReadyCallback {

  private GoogleMap mMap;
  private String TAG = LocationReceiverActivity.class.getSimpleName();
  private static final String DB_REFERENCE = "hajjhackathon/iHajj/locations";
  private List<String> relatives = Arrays
          .asList("userId-0", "userId-1", "userId-2", "userId-3");
  HashMap<String, String> userNameMap = new HashMap<String, String>() {{
    put("userId-0", "Umair");
    put("userId-1", "Haris");
    put("userId-2", "Shehroz");
    put("userId-3", "Asjad");
  }};

  HashMap<String, Float> markerIconMap = new HashMap<String, Float>() {{
    put("userId-0", HUE_VIOLET);
    put("userId-1", HUE_RED);
    put("userId-2", HUE_GREEN);
    put("userId-3", HUE_BLUE);
  }};

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.location_receiver_activity);

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
            .findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);
  }

  @Override
  public void onMapReady(GoogleMap googleMap) {
    mMap = googleMap;
    mMap.getUiSettings().setZoomControlsEnabled(true);
    mMap.getUiSettings().setZoomGesturesEnabled(true);
    mMap.getUiSettings().setMapToolbarEnabled(false);
    mMap.getUiSettings().setMyLocationButtonEnabled(false);

    readRealTimeLocation();
  }

  private void readRealTimeLocation() {
    ValueEventListener postListener = new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        LocationPingModel ping = dataSnapshot.getValue(LocationPingModel.class);
        Log.i(TAG, "ping: " + ping);
        String userName = userNameMap.get(ping.getDeviceId());
        float markerIconCode = markerIconMap.get(ping.getDeviceId());

        showMarker(ping.getLat(), ping.getLon(), userName, markerIconCode);
      }

      @Override
      public void onCancelled(DatabaseError databaseError) {
        // Getting Post failed, log a message
        Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
      }
    };

    for (String relative : relatives) {
      FirebaseDatabase.getInstance().getReference(DB_REFERENCE).child(relative)
              .addValueEventListener(postListener);
    }
  }

  private void showMarker(double lat, double lon, String username, float markerIconCode) {
    LatLng locationPing = new LatLng(lat, lon);
    mMap.addMarker(new MarkerOptions().position(locationPing).icon(BitmapDescriptorFactory.defaultMarker(markerIconCode)).title(username));

    CameraPosition cameraPosition = new CameraPosition.Builder()
            .target(new LatLng(lat, lon))
            .zoom(17)
            .tilt(30)
            .build();
    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
  }
}