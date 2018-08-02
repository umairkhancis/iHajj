package com.hajjhackathon.ihajj.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hajjhackathon.ihajj.model.LocationPingModel;
import com.hajjhackthon.ihajj.BuildConfig;
import com.hajjhackthon.ihajj.R;
import java.text.DateFormat;
import java.util.*;

/**
 * Created by umair.khan on 8/2/18.
 */
public class LocationProviderActivity extends AppCompatActivity {

  private static final String TAG = LocationProviderActivity.class.getSimpleName();

  /**
   * Code used in requesting runtime permissions.
   */
  private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1234;

  /**
   * Constant used in the location settings dialog.
   */
  private static final int REQUEST_CHECK_SETTINGS = 0x1;

  /**
   * The desired interval for location updates. Inexact. Updates may be more or less frequent.
   */
  private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

  /**
   * The fastest rate for active location updates. Exact. Updates will never be more frequent
   * than this value.
   */
  private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
          UPDATE_INTERVAL_IN_MILLISECONDS / 2;

  // Keys for storing activity state in the Bundle.
  private final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
  private final static String KEY_LOCATION = "location";
  private final static String KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string";
  private static final String DB_REFERENCE = "hajjhackathon/iHajj";

  private static final LatLng[][] currentLocationsPool = {
          {
                  new LatLng(21.415228, 39.890545),
                  new LatLng(21.415777, 39.888764),
                  new LatLng(21.417045, 39.887487),
                  new LatLng(21.417749, 39.886645)
          },
          {
                  new LatLng(21.413258, 39.894379),
                  new LatLng(21.412709, 39.895755),
                  new LatLng(21.412549, 39.897557),
                  new LatLng(21.412639, 39.900143)
          },
          {
                  new LatLng(21.413019, 39.893523),
                  new LatLng(21.412040, 39.892847),
                  new LatLng(21.411031, 39.892268),
                  new LatLng(21.410202, 39.891721)
          },
          {
                  new LatLng(21.415060, 39.894766),
                  new LatLng(21.416239, 39.895431),
                  new LatLng(21.417487, 39.896279),
                  new LatLng(21.418875, 39.897137)
          },
  };

  /**
   * Provides access to the Fused Location Provider API.
   */
  private FusedLocationProviderClient mFusedLocationClient;

  /**
   * Provides access to the Location Settings API.
   */
  private SettingsClient mSettingsClient;

  /**
   * Stores parameters for requests to the FusedLocationProviderApi.
   */
  private LocationRequest mLocationRequest;

  /**
   * Stores the types of location services the client is interested in using. Used for checking
   * settings to determine if the device has optimal location settings.
   */
  private LocationSettingsRequest mLocationSettingsRequest;

  /**
   * Callback for Location events.
   */
  private LocationCallback mLocationCallback;

  /**
   * Represents a geographical location.
   */
  private Location mCurrentLocation;

  // UI Widgets.
  private Button mLocationReceiverButton;
  private Button mStopUpdatesButton;

  // Labels.
  private String mLatitudeLabel;
  private String mLongitudeLabel;
  private String mLastUpdateTimeLabel;

  /**
   * Tracks the status of the location updates request. Value changes when the user presses the
   * Start Updates and Stop Updates buttons.
   */
  private Boolean mRequestingLocationUpdates;

  /**
   * Time when the location was updated represented as a String.
   */
  private String mLastUpdateTime;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.location_provider_activity);

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    // Locate the UI widgets.
    mLocationReceiverButton = findViewById(R.id.location_receiver_button);

    // Set labels.
    mLatitudeLabel = getResources().getString(R.string.latitude);
    mLongitudeLabel = getResources().getString(R.string.longitude);
    mLastUpdateTimeLabel = getResources().getString(R.string.last_update_time);

    mRequestingLocationUpdates = false;
    mLastUpdateTime = "";

    // Update values using data stored in the Bundle.
    updateValuesFromBundle(savedInstanceState);

    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    mSettingsClient = LocationServices.getSettingsClient(this);

    // Kick off the process of building the LocationCallback, LocationRequest, and
    // LocationSettingsRequest objects.
    createLocationCallback();
    createLocationRequest();
    buildLocationSettingsRequest();

    initializeLocationUpdates();
  }

  /**
   * Updates fields based on data stored in the bundle.
   *
   * @param savedInstanceState The activity state saved in the Bundle.
   */
  private void updateValuesFromBundle(Bundle savedInstanceState) {
    if (savedInstanceState != null) {
      // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
      // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
      if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
        mRequestingLocationUpdates = savedInstanceState.getBoolean(
                KEY_REQUESTING_LOCATION_UPDATES);
      }

      // Update the value of mCurrentLocation from the Bundle and update the UI to show the
      // correct latitude and longitude.
      if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
        // Since KEY_LOCATION was found in the Bundle, we can be sure that mCurrentLocation is not null.
        mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
      }

      // Update the value of mLastUpdateTime from the Bundle and update the UI.
      if (savedInstanceState.keySet().contains(KEY_LAST_UPDATED_TIME_STRING)) {
        mLastUpdateTime = savedInstanceState.getString(KEY_LAST_UPDATED_TIME_STRING);
      }
      updateClient();
    }
  }

  /**
   * Sets up the location request. Android has two location request settings:
   * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
   * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
   * the AndroidManifest.xml.
   * <p/>
   * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
   * interval (5 seconds), the Fused Location Provider API returns location updates that are
   * accurate to within a few feet.
   * <p/>
   * These settings are appropriate for mapping applications that show real-time location
   * updates.
   */
  private void createLocationRequest() {
    mLocationRequest = LocationRequest.create();

    // Sets the desired interval for active location updates. This interval is
    // inexact. You may not receive updates at all if no location sources are available, or
    // you may receive them slower than requested. You may also receive updates faster than
    // requested if other applications are requesting location at a faster interval.
    mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

    // Sets the fastest rate for active location updates. This interval is exact, and your
    // application will never receive updates faster than this value.
    mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
  }

  /**
   * Creates a callback for receiving location events.
   */
  private void createLocationCallback() {
    mLocationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        super.onLocationResult(locationResult);

        mCurrentLocation = locationResult.getLastLocation();
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        updateLocationDB();
      }
    };
  }

  /**
   * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
   * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
   * if a device has the needed location settings.
   */
  private void buildLocationSettingsRequest() {
    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
    builder.addLocationRequest(mLocationRequest);
    mLocationSettingsRequest = builder.build();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      // Check for the integer request code originally supplied to startResolutionForResult().
      case REQUEST_CHECK_SETTINGS:
        switch (resultCode) {
          case Activity.RESULT_OK:
            Log.i(TAG, "User agreed to make required location settings changes.");
            // Nothing to do. startLocationupdates() gets called in onResume again.
            break;
          case Activity.RESULT_CANCELED:
            Log.i(TAG, "User chose not to make required location settings changes.");
            mRequestingLocationUpdates = false;
            updateClient();
            break;
        }
        break;
    }
  }

  /**
   * Handles the Start Updates button and requests start of location updates. Does nothing if
   * updates have already been requested.
   */
  public void initializeLocationUpdates() {
    if (!mRequestingLocationUpdates) {
      mRequestingLocationUpdates = true;
      startLocationUpdates();
    }
  }

  /**
   * Handles the Receiver button, and sends the user to the map activity
   */
  public void receiverButtonHandler(View view) {
    Intent locationReceiverIntent = new Intent(this, LocationReceiverActivity.class);
    this.startActivity(locationReceiverIntent);
  }

  /**
   * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
   * runtime permission has been granted.
   */
  private void startLocationUpdates() {
    // Begin by checking if the device has the necessary location settings.
    mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
            .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
              @SuppressLint("MissingPermission")
              @Override
              public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                Log.i(TAG, "All location settings are satisfied.");

                // No-inspection MissingPermission
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback,
                        Looper.myLooper());

                updateClient();
              }
            })
            .addOnFailureListener(this, new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                  case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                            "location settings ");
                    try {
                      // Show the dialog by calling startResolutionForResult(), and check the
                      // result in onActivityResult().
                      ResolvableApiException rae = (ResolvableApiException) e;
                      rae.startResolutionForResult(LocationProviderActivity.this,
                              REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sie) {
                      Log.i(TAG, "PendingIntent unable to execute request.");
                    }
                    break;
                  case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                    String errorMessage = "Location settings are inadequate, and cannot be " +
                            "fixed here. Fix in Settings.";
                    Log.e(TAG, errorMessage);
                    Toast.makeText(LocationProviderActivity.this, errorMessage, Toast.LENGTH_LONG)
                            .show();
                    mRequestingLocationUpdates = false;
                }

                updateClient();
              }
            });
  }

  /**
   * Updates all UI fields.
   */
  private void updateClient() {
    updateLocationDB();
  }

  /**
   * Sets the value of the UI fields for the location latitude, longitude and last update time.
   */
  private void updateLocationDB() {
    if (mCurrentLocation != null) {
      int randomId = getRandomId();
      String deviceId = "userId-" + randomId;
      mCurrentLocation = getRandomCurrentLocation(randomId);
      LocationPingModel locationPingModel = new LocationPingModel(mCurrentLocation.getLatitude(),
              mCurrentLocation.getLongitude(), mCurrentLocation.getTime(), deviceId);

      // Logs
      Log.i(TAG, String.format(Locale.ENGLISH, "%s", deviceId));
      Log.i(TAG, String.format(Locale.ENGLISH, "%s: %f", mLatitudeLabel,
              mCurrentLocation.getLatitude()));
      Log.i(TAG, String.format(Locale.ENGLISH, "%s: %f", mLongitudeLabel,
              mCurrentLocation.getLongitude()));
      Log.i(TAG, String.format(Locale.ENGLISH, "%s: %s",
              mLastUpdateTimeLabel, mLastUpdateTime));

      // Write a message to the database
      DatabaseReference db = FirebaseDatabase.getInstance().getReference(DB_REFERENCE);
      db.child("locations").child(deviceId).setValue(locationPingModel);
    }
  }

  private Location getRandomCurrentLocation(int randomId) {
    LatLng randomCoordinates = currentLocationsPool[randomId][getRandomId()];
    mCurrentLocation.setLatitude(randomCoordinates.latitude);
    mCurrentLocation.setLongitude(randomCoordinates.longitude);

    return mCurrentLocation;
  }

  private int getRandomId() {
    Random random = new Random();
    int rand = random.nextInt(4);

    return rand;
  }

  /**
   * Removes location updates from the FusedLocationApi.
   */
  private void stopLocationUpdates() {
    if (!mRequestingLocationUpdates) {
      Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.");
      return;
    }

    // It is a good practice to remove location requests when the activity is in a paused or
    // stopped state. Doing so helps battery performance and is especially
    // recommended in applications that request frequent location updates.
    mFusedLocationClient.removeLocationUpdates(mLocationCallback)
            .addOnCompleteListener(this, new OnCompleteListener<Void>() {
              @Override
              public void onComplete(@NonNull Task<Void> task) {
                mRequestingLocationUpdates = false;
              }
            });
  }

  @Override
  public void onResume() {
    super.onResume();
    // Within {@code onPause()}, we remove location updates. Here, we resume receiving
    // location updates if the user has requested them.
    if (mRequestingLocationUpdates && checkPermissions()) {
      startLocationUpdates();
    } else if (!checkPermissions()) {
      requestPermissions();
    }

    updateClient();
  }

  @Override
  protected void onPause() {
    super.onPause();

    // Remove location updates to save battery.
    // TODO: needs to be removed before final demo
//    stopLocationUpdates();
  }

  /**
   * Stores activity data in the Bundle.
   */
  public void onSaveInstanceState(Bundle savedInstanceState) {
    savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates);
    savedInstanceState.putParcelable(KEY_LOCATION, mCurrentLocation);
    savedInstanceState.putString(KEY_LAST_UPDATED_TIME_STRING, mLastUpdateTime);
    super.onSaveInstanceState(savedInstanceState);
  }

  /**
   * Shows a {@link Snackbar}.
   *
   * @param mainTextStringId The id for the string resource for the Snackbar text.
   * @param actionStringId   The text of the action item.
   * @param listener         The listener associated with the Snackbar action.
   */
  private void showSnackbar(final int mainTextStringId, final int actionStringId,
                            View.OnClickListener listener) {
    Snackbar.make(
            findViewById(android.R.id.content),
            getString(mainTextStringId),
            Snackbar.LENGTH_INDEFINITE)
            .setAction(getString(actionStringId), listener).show();
  }

  /**
   * Return the current state of the permissions needed.
   */
  private boolean checkPermissions() {
    int permissionState = ActivityCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION);
    return permissionState == PackageManager.PERMISSION_GRANTED;
  }

  private void requestPermissions() {
    boolean shouldProvideRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);

    // Provide an additional rationale to the user. This would happen if the user denied the
    // request previously, but didn't check the "Don't ask again" checkbox.
    if (shouldProvideRationale) {
      Log.i(TAG, "Displaying permission rationale to provide additional context.");
      showSnackbar(R.string.permission_rationale,
              android.R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                  // Request permission
                  ActivityCompat.requestPermissions(LocationProviderActivity.this,
                          new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                          REQUEST_PERMISSIONS_REQUEST_CODE);
                }
              });
    } else {
      Log.i(TAG, "Requesting permission");
      // Request permission. It's possible this can be auto answered if device policy
      // sets the permission in a given state or the user denied the permission
      // previously and checked "Never ask again".
      ActivityCompat.requestPermissions(LocationProviderActivity.this,
              new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
              REQUEST_PERMISSIONS_REQUEST_CODE);
    }
  }

  /**
   * Callback received when a permissions request has been completed.
   */
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    Log.i(TAG, "onRequestPermissionResult");
    if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
      if (grantResults.length <= 0) {
        // If user interaction was interrupted, the permission request is cancelled and you
        // receive empty arrays.
        Log.i(TAG, "User interaction was cancelled.");
      } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        if (mRequestingLocationUpdates) {
          Log.i(TAG, "Permission granted, updates requested, starting location updates");
          startLocationUpdates();
        }
      } else {
        // Permission denied.

        // Notify the user via a SnackBar that they have rejected a core permission for the
        // app, which makes the Activity useless. In a real app, core permissions would
        // typically be best requested during a welcome-screen flow.

        // Additionally, it is important to remember that a permission might have been
        // rejected without asking the user for permission (device policy or "Never ask
        // again" prompts). Therefore, a user interface affordance is typically implemented
        // when permissions are denied. Otherwise, your app could appear unresponsive to
        // touches or interactions which have required permissions.
        showSnackbar(R.string.permission_denied_explanation,
                R.string.settings, new View.OnClickListener() {
                  @Override
                  public void onClick(View view) {
                    // Build intent that displays the App settings screen.
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                    intent.setData(uri);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                  }
                });
      }
    }
  }
}