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
	private static java.lang.Integer satellites = null;
	private static LocationServer server = null;
	private static SensorManager sensorManager = null;

	public static Location getLocation() {
		return location;
	}
	public static java.lang.Integer getSatellites() {
		return satellites;
	}

	LocationListener onLocationChange=new LocationListener() {
		public void onLocationChanged(Location fix) {
			location = fix;
		}
		public void onProviderDisabled(String provider) {}
		public void onProviderEnabled(String provider) {}
		public void onStatusChanged(String provider, int status, Bundle extras) {}
			//satellites = new  java.lang.Integer(extras.getInt("satellites"));
	};



	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		locationManager=(LocationManager)getSystemService(LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,  100, 10, onLocationChange);
		server= new LocationServer();
		try
		{
			server.start();
		}
		catch (IOException e)
		{}
	}

	@Override
	public void onDestroy()
	{
		server.stop();
		locationManager.removeUpdates(onLocationChange);
		super.onDestroy();
	}
}
