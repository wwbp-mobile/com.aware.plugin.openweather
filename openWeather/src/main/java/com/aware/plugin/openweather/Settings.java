package com.aware.plugin.openweather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.aware.Aware;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	
	/**
	 * State
	 */
	public static final String STATUS_PLUGIN_OPENWEATHER = "status_plugin_openweather";
	
	/**
	 * Measurement units 
	 */
	public static final String UNITS_PLUGIN_OPENWEATHER = "units_plugin_openweather";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		syncSettings();
	}
	
	private void syncSettings() {
		CheckBoxPreference check = (CheckBoxPreference) findPreference(STATUS_PLUGIN_OPENWEATHER);
		check.setChecked(Aware.getSetting(getApplicationContext(), STATUS_PLUGIN_OPENWEATHER).equals("true"));
		ListPreference units = (ListPreference) findPreference(UNITS_PLUGIN_OPENWEATHER);
		units.setSummary( Aware.getSetting(getApplicationContext(), UNITS_PLUGIN_OPENWEATHER) );
	}
	
	@Override
	protected void onResume() {
		super.onResume();
        syncSettings();
	};

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Preference preference = (Preference) findPreference(key);
		if( preference.getKey().equals(STATUS_PLUGIN_OPENWEATHER)) {
			boolean is_active = sharedPreferences.getBoolean(key, false);
			Aware.setSetting(getApplicationContext(), key, is_active);
			if( is_active ) {
				Aware.startPlugin(getApplicationContext(), getPackageName());
			} else {
				Aware.stopPlugin(getApplicationContext(), getPackageName());
			}	
		}
		if( preference.getKey().equals(UNITS_PLUGIN_OPENWEATHER)) {
			preference.setSummary(sharedPreferences.getString(key, "metric"));
			Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "metric"));
		}
		Intent apply = new Intent(Aware.ACTION_AWARE_REFRESH);
		sendBroadcast(apply);
	}
}
