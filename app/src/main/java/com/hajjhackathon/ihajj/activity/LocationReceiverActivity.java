package com.hajjhackathon.ihajj.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hajjhackathon.ihajj.model.LocationPingModel;
import com.hajjhackthon.ihajj.R;
import java.util.*;

public class LocationReceiverActivity extends FragmentActivity implements OnMapReadyCallback {

  private GoogleMap mMap;
  private String TAG = LocationReceiverActivity.class.getSimpleName();
  private static final String DB_REFERENCE = "hajjhackathon/iHajj/locations";
  private List<String> relatives = Arrays.asList("userId-1", "userId-2", "userId-3", "userId-4", "userId-5");

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.location_receiver_activity);
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
            .findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);
  }

  @Override
  public void onMapReady(GoogleMap googleMap) {
    mMap = googleMap;

    readRealTimeLocation();
  }

  private void readRealTimeLocation() {
    ValueEventListener postListener = new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        LocationPingModel ping = dataSnapshot.getValue(LocationPingModel.class);
        showMarker(ping.getLat(), ping.getLon(), ping.getDeviceId());
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

  private void showMarker(double lat, double lang, String username) {
    LatLng locationPing = new LatLng(lat, lang);
    mMap.addMarker(new MarkerOptions().position(locationPing).title(username));
  }
}