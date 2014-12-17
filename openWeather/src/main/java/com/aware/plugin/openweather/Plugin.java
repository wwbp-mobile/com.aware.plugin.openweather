package com.aware.plugin.openweather;

import java.io.IOException;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Locations;
import com.aware.plugin.openweather.Provider.OpenWeather_Data;
import com.aware.providers.Locations_Provider.Locations_Data;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Http;

public class Plugin extends Aware_Plugin {
	
	/**
	 * Shared context: new OpenWeather data is available
	 */
	public static final String ACTION_AWARE_PLUGIN_OPENWEATHER = "ACTION_AWARE_PLUGIN_OPENWEATHER";
	
	/**
	 * Extra string: openweather<br/>
	 * JSONObject from OpenWeather<br/>
	 * http://bugs.openweathermap.org/projects/api/wiki/Weather_Data
	 */
	public static final String EXTRA_OPENWEATHER = "openweather";
	
	private static final String OPENWEATHER_API_URL = "http://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&lang=%s&units=%s";
	private static ContextProducer sContextProducer;
	private static JSONObject sOpenWeather;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		Intent aware_framework = new Intent(this, Aware.class);
		startService(aware_framework);
		
		Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN_OPENWEATHER, true);
		
		if( Aware.getSetting(getApplicationContext(), Settings.UNITS_PLUGIN_OPENWEATHER).length() == 0 ) {
			Aware.setSetting(getApplicationContext(), Settings.UNITS_PLUGIN_OPENWEATHER, "metric");
		}
		
		Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_LOCATION_NETWORK, true);
		Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_LOCATION_NETWORK, 60 * 60);
		Aware.setSetting(getApplicationContext(), Aware_Preferences.MIN_LOCATION_NETWORK_ACCURACY, 1000);
		Aware.setSetting(getApplicationContext(), Aware_Preferences.LOCATION_EXPIRATION_TIME, 60 * 60);
		
		Intent apply = new Intent(Aware.ACTION_AWARE_REFRESH);
		sendBroadcast(apply);
		
		TAG = "AWARE::OpenWeather";
		DEBUG = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEBUG_FLAG).equals("true");
		
		CONTEXT_PRODUCER = new ContextProducer() {
			@Override
			public void onContext() {
				Intent mOpenWeather = new Intent(ACTION_AWARE_PLUGIN_OPENWEATHER);
				mOpenWeather.putExtra(EXTRA_OPENWEATHER, sOpenWeather.toString());
				sendBroadcast(mOpenWeather);
			}
		};
		sContextProducer = CONTEXT_PRODUCER;
		
		DATABASE_TABLES = Provider.DATABASE_TABLES;
		TABLES_FIELDS = Provider.TABLES_FIELDS;
		CONTEXT_URIS = new Uri[]{ OpenWeather_Data.CONTENT_URI };
		
		IntentFilter filter = new IntentFilter(Locations.ACTION_AWARE_LOCATIONS);
		registerReceiver(sLocationListener, filter);
		
		getWeather(getApplicationContext());
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		getWeather(getApplicationContext());
		return START_STICKY;
	}
	
	private static LocationListener sLocationListener = new LocationListener(); 
	public static class LocationListener extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if( intent.getAction().equals(Locations.ACTION_AWARE_LOCATIONS) ) {
				getWeather(context);
			}
		}
	}
	
	protected static void getWeather(Context context) {
		Cursor current_location = context.getContentResolver().query(Locations_Data.CONTENT_URI, null, null, null, Locations_Data.TIMESTAMP + " DESC LIMIT 1");
		if( current_location != null && current_location.moveToFirst() ) {
			double latitude = current_location.getDouble(current_location.getColumnIndex(Locations_Data.LATITUDE));
			double longitude = current_location.getDouble(current_location.getColumnIndex(Locations_Data.LONGITUDE));
			
			Intent openWeatherService = new Intent(context, OpenWeather_Service.class);
			openWeatherService.putExtra("latitude", latitude);
			openWeatherService.putExtra("longitude", longitude);
			context.startService(openWeatherService);
		}
		if( current_location != null && ! current_location.isClosed() ) current_location.close();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(sLocationListener);

        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_LOCATION_NETWORK, false);
		Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN_OPENWEATHER, false);
		Intent apply = new Intent(Aware.ACTION_AWARE_REFRESH);
		sendBroadcast(apply);
	}
	
	/**
	 * Background service that will connect to OpenWeather API and fetch and store current weather conditions depending on the user's location
	 * @author dferreira
	 */
	public static class OpenWeather_Service extends IntentService {
		public OpenWeather_Service() {
			super("AWARE OpenWeather");
		}

		@Override
		protected void onHandleIntent(Intent intent) {
			double latitude = intent.getDoubleExtra("latitude", 0);
			double longitude = intent.getDoubleExtra("longitude", 0);
			
			if( latitude != 0 && longitude != 0 ) {
				Http httpObj = new Http();
				HttpResponse server_response = httpObj.dataGET(String.format(OPENWEATHER_API_URL, latitude, longitude, Locale.getDefault().getLanguage(), Aware.getSetting(getApplicationContext(), Settings.UNITS_PLUGIN_OPENWEATHER)), false);
				if( server_response != null && server_response.getStatusLine().getStatusCode() == 200) {
					try {
						JSONObject raw_data = new JSONObject( EntityUtils.toString(server_response.getEntity()) );
						
						JSONObject wind = raw_data.getJSONObject("wind");
						JSONObject weather_characteristics = raw_data.getJSONObject("main");
						JSONObject weather = raw_data.getJSONArray("weather").getJSONObject(0);
						JSONObject clouds = raw_data.getJSONObject("clouds");
						
						ContentValues weather_data = new ContentValues();
						weather_data.put(OpenWeather_Data.TIMESTAMP, System.currentTimeMillis());
						weather_data.put(OpenWeather_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
						weather_data.put(OpenWeather_Data.CITY, raw_data.getString("name"));
						weather_data.put(OpenWeather_Data.TEMPERATURE, weather_characteristics.getDouble("temp"));
						weather_data.put(OpenWeather_Data.TEMPERATURE_MAX, weather_characteristics.getDouble("temp_max"));
						weather_data.put(OpenWeather_Data.TEMPERATURE_MIN, weather_characteristics.getDouble("temp_min"));
						weather_data.put(OpenWeather_Data.UNITS, Aware.getSetting(getApplicationContext(), Settings.UNITS_PLUGIN_OPENWEATHER));
						weather_data.put(OpenWeather_Data.HUMIDITY, weather_characteristics.getDouble("humidity"));
						weather_data.put(OpenWeather_Data.PRESSURE, weather_characteristics.getDouble("pressure"));
						weather_data.put(OpenWeather_Data.WIND_SPEED, wind.getDouble("speed"));
						weather_data.put(OpenWeather_Data.WIND_DEGREES, wind.getDouble("deg"));
						weather_data.put(OpenWeather_Data.CLOUDINESS, clouds.getDouble("all"));
						weather_data.put(OpenWeather_Data.WEATHER_ICON_ID, weather.getInt("id"));
						weather_data.put(OpenWeather_Data.WEATHER_DESCRIPTION, weather.getString("main") + ": "+weather.getString("description"));
						
						getContentResolver().insert(OpenWeather_Data.CONTENT_URI, weather_data);
						
						sOpenWeather = raw_data;
						sContextProducer.onContext();
						
						if( DEBUG) Log.d(TAG, weather_data.toString());
						
					} catch (ParseException e) {
						if( DEBUG ) Log.d(TAG,"Error parsing JSON from server: " + e.getMessage());
					} catch (JSONException e) {
						if( DEBUG ) Log.d(TAG,"Error reading JSON: " + e.getMessage());
					} catch (IOException e) {
						if( DEBUG ) Log.d(TAG,"Error receiving JSON from server: " + e.getMessage());
					}
				}
			}
		}
	}
}
