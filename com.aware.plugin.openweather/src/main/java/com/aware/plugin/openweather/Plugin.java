package com.aware.plugin.openweather;

import android.Manifest;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.openweather.Provider.OpenWeather_Data;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Scheduler;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;

public class Plugin extends Aware_Plugin implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    /**
     * Shared context: new OpenWeather data is available
     */
    public static final String ACTION_AWARE_PLUGIN_OPENWEATHER = "ACTION_AWARE_PLUGIN_OPENWEATHER";

    /**
     * Extra string: openweather<br/>
     * JSONObject from OpenWeather<br/>
     */
    public static final String EXTRA_OPENWEATHER = "openweather";

    public static ContextProducer sContextProducer;
    public static ContentValues sOpenWeather;

    public static GoogleApiClient mGoogleApiClient;
    private final static LocationRequest locationRequest = new LocationRequest();
    private static PendingIntent pIntent;

    private static final String SCHEDULER_PLUGIN_OPENWEATHER = "scheduler_plugin_openweather";

    @Override
    public void onCreate() {
        super.onCreate();

        TAG = "AWARE: OpenWeather";

        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{OpenWeather_Data.CONTENT_URI};

        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                Intent mOpenWeather = new Intent(ACTION_AWARE_PLUGIN_OPENWEATHER);
                mOpenWeather.putExtra(EXTRA_OPENWEATHER, sOpenWeather);
                sendBroadcast(mOpenWeather);
            }
        };
        sContextProducer = CONTEXT_PRODUCER;

        //Permissions needed for our plugin
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (!is_google_services_available()) {
            if (DEBUG)
                Log.e(TAG, "Google Services Fused location are not available on this device");
            stopSelf();
        } else {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApiIfAvailable(LocationServices.API)
                    .build();

            Intent openWeatherIntent = new Intent(getApplicationContext(), OpenWeather_Service.class);
            pIntent = PendingIntent.getService(getApplicationContext(), 0, openWeatherIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (PERMISSIONS_OK) {

            DEBUG = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEBUG_FLAG).equals("true");

            Aware.setSetting(this, Settings.STATUS_PLUGIN_OPENWEATHER, true);
            if (Aware.getSetting(getApplicationContext(), Settings.UNITS_PLUGIN_OPENWEATHER).length() == 0)
                Aware.setSetting(getApplicationContext(), Settings.UNITS_PLUGIN_OPENWEATHER, "metric");

            if (Aware.getSetting(getApplicationContext(), Settings.PLUGIN_OPENWEATHER_FREQUENCY).length() == 0)
                Aware.setSetting(getApplicationContext(), Settings.PLUGIN_OPENWEATHER_FREQUENCY, 60);

            if (Aware.getSetting(getApplicationContext(), Settings.OPENWEATHER_API_KEY).length() == 0)
                Aware.setSetting(getApplicationContext(), Settings.OPENWEATHER_API_KEY, "ada11fb870974565377df238f3046aa9");

            if (mGoogleApiClient != null && !mGoogleApiClient.isConnected())
                mGoogleApiClient.connect();

            Aware.startAWARE(this);

            try {
                Scheduler.Schedule openweather = Scheduler.getSchedule(this, SCHEDULER_PLUGIN_OPENWEATHER);
                if (openweather == null || openweather.getInterval() != Long.parseLong(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_OPENWEATHER_FREQUENCY))) {
                    openweather = new Scheduler.Schedule(SCHEDULER_PLUGIN_OPENWEATHER);
                    openweather
                            .setInterval(Long.parseLong(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_OPENWEATHER_FREQUENCY)))
                            .setActionType(Scheduler.ACTION_TYPE_SERVICE)
                            .setActionClass(getPackageName() + "/" + OpenWeather_Service.class.getName());
                    Scheduler.saveSchedule(this, openweather);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Aware.setSetting(this, Settings.STATUS_PLUGIN_OPENWEATHER, false);

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, pIntent);
            mGoogleApiClient.disconnect();
        }

        Aware.stopAWARE(this);
    }

    private boolean is_google_services_available() {
        GoogleApiAvailability googleApi = GoogleApiAvailability.getInstance();
        int result = googleApi.isGooglePlayServicesAvailable(this);
        return (result == ConnectionResult.SUCCESS);
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (DEBUG)
            Log.i(TAG, "Connected to Google Fused Location API");

        locationRequest.setInterval(Long.parseLong(Aware.getSetting(this, Settings.PLUGIN_OPENWEATHER_FREQUENCY)) * 60 * 1000);
        locationRequest.setFastestInterval(Long.parseLong(Aware.getSetting(this, Settings.PLUGIN_OPENWEATHER_FREQUENCY)) * 60 * 1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);

        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, pIntent);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (DEBUG)
            Log.w(TAG, "Error connecting to Google Fused Location services, will try again in 5 minutes");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (DEBUG)
            Log.w(TAG, "Error connecting to Google Fused Location services, will try again in 5 minutes");
    }
}
