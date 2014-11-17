package com.aware.plugin.openweather;

import java.util.Calendar;
import java.util.Locale;

import com.aware.plugin.openweather.Provider.OpenWeather_Data;
import com.aware.utils.IContextCard;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ContextCard implements IContextCard {

    /**
     * Constructor for Stream reflection
     */
    public ContextCard(){}

	public View getContextCard(Context context) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View mInflated = (View) inflater.inflate(R.layout.layout, null);
		
		ImageView weather_icon = (ImageView) mInflated.findViewById(R.id.icon_weather);
		TextView weather_city = (TextView) mInflated.findViewById(R.id.weather_city);
		TextView weather_description = (TextView) mInflated.findViewById(R.id.weather_description);
		TextView weather_temperature = (TextView) mInflated.findViewById(R.id.weather_temperature);
		TextView weather_max_temp = (TextView) mInflated.findViewById(R.id.weather_max_temp);
		TextView weather_min_temp = (TextView) mInflated.findViewById(R.id.weather_min_temp);
		TextView weather_pressure = (TextView) mInflated.findViewById(R.id.weather_pressure);
		TextView weather_humidity = (TextView) mInflated.findViewById(R.id.weather_humidity);
		TextView weather_cloudiness = (TextView) mInflated.findViewById(R.id.weather_cloudiness);
		TextView weather_wind = (TextView) mInflated.findViewById(R.id.weather_wind);
		TextView weather_wind_degrees = (TextView) mInflated.findViewById(R.id.weather_wind_degrees);
		
		Calendar cal = Calendar.getInstance(Locale.getDefault());
		cal.setTimeInMillis(System.currentTimeMillis());
		int current_hour = cal.get(Calendar.HOUR_OF_DAY);
		boolean is_daytime = ( current_hour >= 8 && current_hour <= 18 );
		
		Cursor latest_weather = context.getContentResolver().query( OpenWeather_Data.CONTENT_URI, null, null, null, OpenWeather_Data.TIMESTAMP + " DESC LIMIT 1" );
		if( latest_weather != null && latest_weather.moveToFirst() ) {
			
			int weather_id = latest_weather.getInt(latest_weather.getColumnIndex(OpenWeather_Data.WEATHER_ICON_ID));
			if( weather_id >= 200 && weather_id <= 232 ) {
				weather_icon.setImageResource(R.drawable.ic_weather_thunderstorm);
			} else if( weather_id >= 300 && weather_id <= 321 ) {
				weather_icon.setImageResource(R.drawable.ic_weather_drizzle);
			} else if( weather_id >= 500 && weather_id <= 531 ) {
				weather_icon.setImageResource(R.drawable.ic_weather_rain);
			} else if( weather_id >= 600 && weather_id <= 622 ) {
				weather_icon.setImageResource(R.drawable.ic_weather_snow);
			} else if( weather_id == 906 ) {
				weather_icon.setImageResource(R.drawable.ic_weather_hail);
			} else if( weather_id >= 701 && weather_id <= 781 ) {
				if( is_daytime ) {
					weather_icon.setImageResource(R.drawable.ic_weather_fog_day);
				} else {
					weather_icon.setImageResource(R.drawable.ic_weather_fog_night);
				}
			} else if( weather_id >= 801 && weather_id <= 803 ) {
				weather_icon.setImageResource(R.drawable.ic_weather_cloudy);
			} else {
				if( is_daytime ) {
					weather_icon.setImageResource(R.drawable.ic_weather_clear_day);
				} else {
					weather_icon.setImageResource(R.drawable.ic_weather_clear_night);
				}
			}
			weather_city.setText(latest_weather.getString(latest_weather.getColumnIndex(OpenWeather_Data.CITY)));
			weather_description.setText(latest_weather.getString(latest_weather.getColumnIndex(OpenWeather_Data.WEATHER_DESCRIPTION)));
			weather_temperature.setText(String.format("%.1f",latest_weather.getDouble(latest_weather.getColumnIndex(OpenWeather_Data.TEMPERATURE))) + "º");
			weather_min_temp.setText(context.getResources().getString(R.string.label_minimum) + String.format(" %.1f",latest_weather.getDouble(latest_weather.getColumnIndex(OpenWeather_Data.TEMPERATURE_MIN))));
			weather_max_temp.setText(context.getResources().getString(R.string.label_maximum) + String.format(" %.1f",latest_weather.getDouble(latest_weather.getColumnIndex(OpenWeather_Data.TEMPERATURE_MAX))));
			weather_pressure.setText(latest_weather.getDouble(latest_weather.getColumnIndex(OpenWeather_Data.PRESSURE))+ " hPa");
			weather_humidity.setText(latest_weather.getInt(latest_weather.getColumnIndex(OpenWeather_Data.HUMIDITY)) + " %");
			weather_cloudiness.setText(latest_weather.getInt(latest_weather.getColumnIndex(OpenWeather_Data.CLOUDINESS)) + " %");
			weather_wind.setText(latest_weather.getFloat(latest_weather.getColumnIndex(OpenWeather_Data.WIND_SPEED)) + " m/s");
			weather_wind_degrees.setText(latest_weather.getInt(latest_weather.getColumnIndex(OpenWeather_Data.WIND_DEGREES)) + "º");
		}
		if( latest_weather != null && ! latest_weather.isClosed() ) latest_weather.close();
		
		return mInflated;
	}
}
