package com.aware.plugin.openweather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.aware.plugin.openweather.Provider.OpenWeather_Data;
import com.aware.ui.Stream_UI;
import com.aware.utils.IContextCard;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ContextCard implements IContextCard {

    private int refresh_interval = 1 * 1000;

    private Handler uiRefresher = new Handler(Looper.getMainLooper());
    private Runnable uiChanger = new Runnable() {
        @Override
        public void run() {
            //Modify card's content here once it's initialized
            if( card != null ) {
                Calendar cal = Calendar.getInstance(Locale.getDefault());
                cal.setTimeInMillis(System.currentTimeMillis());
                int current_hour = cal.get(Calendar.HOUR_OF_DAY);
                boolean is_daytime = ( current_hour >= 8 && current_hour <= 18 );

                Cursor latest_weather = sContext.getContentResolver().query( OpenWeather_Data.CONTENT_URI, null, null, null, OpenWeather_Data.TIMESTAMP + " DESC LIMIT 1" );
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
                    weather_min_temp.setText(sContext.getResources().getString(R.string.label_minimum) + String.format(" %.1f",latest_weather.getDouble(latest_weather.getColumnIndex(OpenWeather_Data.TEMPERATURE_MIN))));
                    weather_max_temp.setText(sContext.getResources().getString(R.string.label_maximum) + String.format(" %.1f",latest_weather.getDouble(latest_weather.getColumnIndex(OpenWeather_Data.TEMPERATURE_MAX))));
                    weather_pressure.setText(latest_weather.getDouble(latest_weather.getColumnIndex(OpenWeather_Data.PRESSURE))+ " hPa");
                    weather_humidity.setText(latest_weather.getInt(latest_weather.getColumnIndex(OpenWeather_Data.HUMIDITY)) + " %");
                    weather_cloudiness.setText(latest_weather.getInt(latest_weather.getColumnIndex(OpenWeather_Data.CLOUDINESS)) + " %");
                    weather_wind.setText(latest_weather.getFloat(latest_weather.getColumnIndex(OpenWeather_Data.WIND_SPEED)) + " m/s");
                    weather_wind_degrees.setText(latest_weather.getInt(latest_weather.getColumnIndex(OpenWeather_Data.WIND_DEGREES)) + "º");
                    weather_rain.setText(latest_weather.getInt(latest_weather.getColumnIndex(OpenWeather_Data.RAIN)) + " mm");
                    weather_snow.setText(latest_weather.getInt(latest_weather.getColumnIndex(OpenWeather_Data.SNOW)) + " mm");
                    sunrise.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(latest_weather.getInt(latest_weather.getColumnIndex(OpenWeather_Data.SUNRISE)) * 1000L)));
                    sunset.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(latest_weather.getInt(latest_weather.getColumnIndex(OpenWeather_Data.SUNSET)) * 1000L)));

                }
                if( latest_weather != null && ! latest_weather.isClosed() ) latest_weather.close();
            }

            //Reset timer and schedule the next card refresh
            uiRefresher.postDelayed(uiChanger, refresh_interval);
        }
    };
    
    /**
     * Constructor for Stream reflection
     */
    public ContextCard(){}
    
    private Context sContext;
    private LayoutInflater sInflater;
    
    private View card;
    private ImageView weather_icon; 
    private TextView weather_city;
    private TextView weather_description;
    private TextView weather_temperature;
    private TextView weather_max_temp;
    private TextView weather_min_temp;
    private TextView weather_pressure;
    private TextView weather_humidity;
    private TextView weather_cloudiness;
    private TextView weather_wind;
    private TextView weather_wind_degrees;
    private TextView weather_rain;
    private TextView weather_snow;
    private TextView sunrise;
    private TextView sunset;
    
	public View getContextCard(Context context) {
		sContext = context;
        sInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //Tell Android that you'll monitor the stream statuses
        IntentFilter filter = new IntentFilter();
        filter.addAction(Stream_UI.ACTION_AWARE_STREAM_OPEN);
        filter.addAction(Stream_UI.ACTION_AWARE_STREAM_CLOSED);
        context.registerReceiver(streamObs, filter);

        card = sInflater.inflate(R.layout.layout, null);
		
		weather_icon = (ImageView) card.findViewById(R.id.icon_weather);
		weather_city = (TextView) card.findViewById(R.id.weather_city);
		weather_description = (TextView) card.findViewById(R.id.weather_description);
		weather_temperature = (TextView) card.findViewById(R.id.weather_temperature);
		weather_max_temp = (TextView) card.findViewById(R.id.weather_max_temp);
		weather_min_temp = (TextView) card.findViewById(R.id.weather_min_temp);
		weather_pressure = (TextView) card.findViewById(R.id.weather_pressure);
		weather_humidity = (TextView) card.findViewById(R.id.weather_humidity);
		weather_cloudiness = (TextView) card.findViewById(R.id.weather_cloudiness);
		weather_wind = (TextView) card.findViewById(R.id.weather_wind);
		weather_wind_degrees = (TextView) card.findViewById(R.id.weather_wind_degrees);
        weather_rain = (TextView) card.findViewById(R.id.rain);
        weather_snow = (TextView) card.findViewById(R.id.snow);
        sunrise = (TextView) card.findViewById(R.id.sunrise);
        sunset = (TextView) card.findViewById(R.id.sunset);
		
		uiRefresher.post(uiChanger);
		
		return card;
	}

    //This is a BroadcastReceiver that keeps track of stream status. Used to stop the refresh when user leaves the stream and restart again otherwise
    private StreamObs streamObs = new StreamObs();
    public class StreamObs extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if( intent.getAction().equals(Stream_UI.ACTION_AWARE_STREAM_OPEN) ) {
                //start refreshing when user enters the stream
                uiRefresher.post(uiChanger);
            }
            if( intent.getAction().equals(Stream_UI.ACTION_AWARE_STREAM_CLOSED) ) {
                //stop refreshing when user leaves the stream
                uiRefresher.removeCallbacks(uiChanger);
                uiRefresher.removeCallbacksAndMessages(null);
            }
        }
    }
}
