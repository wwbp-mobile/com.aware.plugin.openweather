package com.aware.plugin.openweather;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
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

    /**
     * How frequently we check the weather conditions
     */
    public static final String PLUGIN_OPENWEATHER_FREQUENCY = "plugin_openweather_frequency";
	
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
        EditTextPreference frequency = (EditTextPreference) findPreference(PLUGIN_OPENWEATHER_FREQUENCY);
        frequency.setText(Aware.getSetting(getApplicationContext(), PLUGIN_OPENWEATHER_FREQUENCY));
        frequency.setSummary("Every " + Aware.getSetting(getApplicationContext(), PLUGIN_OPENWEATHER_FREQUENCY) + " minute(s)");
	}
	
	@Override
	protected void onResume() {
		super.onResume();
        syncSettings();
	};

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Preference preference = findPreference(key);
		if( preference.getKey().equals(STATUS_PLUGIN_OPENWEATHER)) {
			boolean is_active = sharedPreferences.getBoolean(key, false);
			Aware.setSetting(getApplicationContext(), key, is_active);
			if( is_active ) {
				Aware.startPlugin(getApplicationContext(), "com.aware.plugin.openweather");
			} else {
				Aware.stopPlugin(getApplicationContext(), "com.aware.plugin.openweather");
			}	
		}
		if( preference.getKey().equals(UNITS_PLUGIN_OPENWEATHER)) {
			preference.setSummary(sharedPreferences.getString(key, "metric"));
			Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "metric"));
			Aware.startPlugin(getApplicationContext(), "com.aware.plugin.openweather");
		}
        if( preference.getKey().equals(PLUGIN_OPENWEATHER_FREQUENCY)) {
            preference.setSummary("Every " + sharedPreferences.getString(key,"30") + " minute(s)");
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "30"));
			Aware.startPlugin(getApplicationContext(), "com.aware.plugin.openweather");
        }
	}
}
