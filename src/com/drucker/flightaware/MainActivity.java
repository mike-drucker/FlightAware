package com.drucker.flightaware;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.location.*;
import android.util.*;
import java.io.*;
import android.hardware.*;
import android.media.*;

public class MainActivity extends Activity
{
	private final String TAG = "MainActivity";
	private final long SENSOR_SHUTDOWN_TIMEOUT = 1000 * 60 * 5; //5 min
	private final long PICTURE_INTERVAL = 1000 * 1 * 1; //1 second
	private static Date lastSensorRequest = null;
	private static bool sensorsRunning = false;
	private static Object sensorSetupSemaphore = new Object();
	

	private LocationManager locationManager=null;
	private static Location location = null;
	private static LocationServer server = null;
	private static SensorManager sensorManager = null;
	private static Sensor geomagneticSensor = null;
	private static Sensor accelerometerSensor = null;
	private static Sensor pressureSensor = null;
	private static Camera camera = null;
	private static Timer cameraTimer = null;
	private static float azimuthDegrees = 0;
	private static float pitchDegrees = 0;
	private static float rollDegrees = 0;
	private static float gForce = 0;
	private static float barometricPressure = 0;
	private static byte[] picture = null;
	
	public static Location getLocation() {
		return location;
	}
	
	public static float getAzimuthDegrees() {
		return azimuthDegrees;
	}
	
	public static float getRollDegrees() {
		return rollDegrees;
	}
	
	public static float getPitchDegrees() {
		return pitchDegrees;
	}
	
	public static String getDirection()
	{
		String directions[] = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
		return directions[(int)Math.round(( ((double)azimuthDegrees % 360) / 45)) % 8];
	}
	
	public static float getGForce() {
		return gForce;
	}
	
	public static byte[] getPicture()
	{
		return picture;
	}
	
	public static float getBarometricPressure() {
		return barometricPressure;
	}
	
	public static void setSensorRequested() {
		lastSensorRequest = new Date();
		if(!sensorsRunning)
		{
			synchronized(sensorSetupSemaphore){
				startSensors();
				sensorsRunning = true;
			}
		}
	}
	
	public static bool shouldSensorsStop() {
		if(lastSensorRequest == null)
			return true;
		long now = (new Date()).getTime();
		return (now - lastSensorRequest.getTime()) > SENSOR_SHUTDOWN_TIMEOUT;
	}
	
	TimerTask cameraTimerTask = new TimerTask() {
		public void run()
		{
			if(shouldSensorsStop() || !sensorsRunning)
				return;
			camera.takePicture(null,null,null,onPictureCallback);
		}
	}
	
	Camera.PictureCallback onPictureCallback = new Camera.PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			picture = data;
		}
	}
	
	LocationListener onLocationChange= new LocationListener() {
		public void onLocationChanged(Location fix) {
			location = fix;
			if(sensorsRunning && shouldSensorsStop())
			{
				synchronized(sensorSetupSemaphore){
					stopSensors();
					sensorsRunning = false;
				}
			}
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
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				gravity = event.values;
				float x = (int)(event.values[SensorManager.DATA_X]);
				float y = (int)(event.values[SensorManager.DATA_Y]);
				float z = (int)(event.values[SensorManager.DATA_Z]);
				gForce = (float) (Math.sqrt(x*x+y*y+z*z)/SensorManager.GRAVITY_EARTH);
			}
			if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
				geomagnetic = event.values;
			if (event.sensor.getType() == Sensor.TYPE_PRESSURE)
			{
				barometricPressure = event.values[0];
			}
			
			if ((event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD || event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) && gravity != null && geomagnetic != null) {
				float R[] = new float[9];
				float I[] = new float[9];
				boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
				if (success) {
					float orientation[] = new float[3];
					SensorManager.getOrientation(R, orientation);
					azimuthDegrees = calculateAzimuthDegrees(orientation[0]);
					pitchDegrees = (float) Math.toDegrees(orientation[1]);
					rollDegrees = (float) Math.toDegrees(orientation[2]);
				}
			}
			if(sensorsRunning && shouldSensorsStop())
			{
				synchronized(sensorSetupSemaphore){
					stopSensors();
					sensorsRunning = false;
				}
			}
		}
	};
	
	private float calculateAzimuthDegrees(float azimuthRadians) {
	    // orientation contains: azimuth, pitch and roll
		azimuthRadians = -azimuthRadians*360/(2*3.14159f);
		//correct for negative western directions
		if(azimuthRadians < 0)
			azimuthRadians = azimuthRadians + 360;
		return azimuthRadians;
	}



	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		parameters.setPictureSize(600,400);
		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
		parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
		camera.setParameters(parameters);
		//setup http server
		server= new LocationServer(this);
		try
		{
			server.start();
		}
		catch (IOException e)
		{
			Log.d(TAG,"IOException",e);
			e.printStackTrace();
		}
	}
	
	private void startSensors() {
		if(sensorsRunning)
			return;
		//setup sensors
		sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		geomagneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
		sensorManager.registerListener(onSensorEventChange, accelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(onSensorEventChange, geomagneticSensor, SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(onSensorEventChange, pressureSensor, SensorManager.SENSOR_DELAY_FASTEST);
		//setup gps
		locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,  100, 10, onLocationChange);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,  100, 10, onLocationChange);
		camera = Camera.open();
		Camera.Parameters parameters = camera.getParameters();
		cameraTimer = new Timer();
		cameraTimer.scheduleAtFixedRate(cameraTimerTask,0,PICTURE_INTERVAL);
		parameters.setJpegQuality(CameraProfile.QUALITY_LOW);
	}
	
	
	private void stopSensors() {
		if(!sensorsRunning)
			return;
		locationManager.removeUpdates(onLocationChange);
		sensorManager.unregisterListener(onSensorEventChange);
		cameraTimerTask.cancel();
		camera.release();
	}

	@Override
	public void onDestroy()
	{
		server.stop();
		stopSensors();
		super.onDestroy();
	}
}
