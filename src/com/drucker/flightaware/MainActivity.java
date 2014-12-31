package com.drucker.flightaware;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.location.*;
import android.util.*;
import java.io.*;
import android.hardware.*;

public class MainActivity extends Activity
{
	//<uses-permission android:name="android.permission.INTERNET" />
	//<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
	//<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

	private LocationManager locationManager=null;
	private static Location location = null;
	private static LocationServer server = null;
	private static SensorManager sensorManager = null;
	private static Sensor geomagneticSensor = null;
	private static Sensor accelerometerSensor = null;
	private static float azimuthDegrees = 0;

	public static Location getLocation() {
		return location;
	}
	
	public static float getAzimuthDegrees() {
		return azimuthDegrees;
	}

	LocationListener onLocationChange= new LocationListener() {
		public void onLocationChanged(Location fix) {
			location = fix;
		}
		public void onProviderDisabled(String provider) {}
		public void onProviderEnabled(String provider) {}
		public void onStatusChanged(String provider, int status, Bundle extras) {}
	};
	
    SensorEventListener onSensorEventChange = new SensorEventListener() {
		float[] gravity;
		float[] geomagnetic;
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}
		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
				gravity = event.values;
			if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
				geomagnetic = event.values;
			if (gravity != null && geomagnetic != null) {
				float R[] = new float[9];
				float I[] = new float[9];
				boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
				if (success) {
					float orientation[] = new float[3];
					SensorManager.getOrientation(R, orientation);
					float azimuth = orientation[0]; // orientation contains: azimuth, pitch and roll
					azimuthDegrees = -azimut*360/(2*3.14159f);
				}
	    	}
		}
	};



	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		//setup sensors
		sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		sensorManager.registerListener(onSensorEventChange, accelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(onSensorEventChange, magnetometerSensor, SensorManager.SENSOR_DELAY_FASTEST);
        //setup gps
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,  100, 10, onLocationChange);
		locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,  100, 10, onLocationChange);
		//setup http server
		server= new LocationServer();
		try
		{
			server.start();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onDestroy()
	{
		server.stop();
		locationManager.removeUpdates(onLocationChange);
		super.onDestroy();
	}
}
