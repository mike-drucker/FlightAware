package com.drucker.flightaware;

import android.app.*;
import android.hardware.*;
import android.location.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.commonsware.cwac.camera.*;
import java.io.*;
import java.util.*;
import android.text.*;
import android.content.*;
import android.graphics.*;

public class MainActivity extends Activity
{
	
	private final static String TAG = "MainActivity";
	private final static String DIRECTIONS[] = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
	private final static long SENSOR_SHUTDOWN_TIMEOUT = 1000 * 60 * 5; //5 min
	private final static long PICTURE_INTERVAL = 1000 * 1 * 1; //1 second
	private final static long SENSOR_INTERVAL = 1000 * 5 *1; //5 seconds
	private static Date lastSensorRequest = null;
	private static boolean sensorsRunning = false;
	private static Object sensorSetupSemaphore = new Object();
	

	private static LocationManager locationManager=null;
	
	private static LocationServer server = null;
	private static SensorManager sensorManager = null;
	private static Sensor geomagneticSensor = null;
	private static Sensor accelerometerSensor = null;
	private static Sensor pressureSensor = null;
	private static Timer cameraTimer = null;
    private final static SensorStatus s = new SensorStatus();
	private static SensorStatus home;
	private static float gForce = 0;
	CameraFragment fragment = null;

	public static SensorStatus getHome() {return home;}
	public static Location getLocation() {return s.location;}
	public static float getAzimuthDegrees() {return s.azimuthDegrees;}
	public static float getRollDegrees() {return s.rollDegrees;}
	public static float getPitchDegrees() {return s.pitchDegrees;}
	
	public static String getDirection() {
		return DIRECTIONS[(int)Math.round(( ((double)s.azimuthDegrees % 360) / 45)) % 8];
	}
	
	public static float getGForce() {
		return gForce;
	}
	
	public static byte[] getPicture() {
		if(FlightCameraHost.data == null)
			return null;
		return FlightCameraHost.data.clone();
	}
	
	public static float getBarometricPressure() {
		return s.barometricPressure;
	}
	
	public static void setSensorRequested() {
		lastSensorRequest = new Date();
	}
	
	public static boolean shouldSensorsStop() {
		if(lastSensorRequest == null)
			return true;
		long now = (new Date()).getTime();
		return (now - lastSensorRequest.getTime()) > SENSOR_SHUTDOWN_TIMEOUT;
	}
	
	TimerTask sensorCheckTimer = new TimerTask() {
		public void run() {
			if(!sensorsRunning && !shouldSensorsStop()) {
				synchronized(sensorSetupSemaphore){
					startSensors();
					sensorsRunning = true;
				}
			}
		}
	};
	
	TimerTask cameraTimerTask = new TimerTask() {
		public void run() {
			if( shouldSensorsStop() || !sensorsRunning)
				return;
			try
			{
				fragment.takePicture();
			}
			catch (IllegalStateException e)
			{/*do nothing*/}
		}
	};
	
	
	LocationListener onLocationChange= new LocationListener() {
		public void onLocationChanged(Location fix) {
			Button button = (Button) findViewById(R.id.setHome);
			button.setEnabled(true);
			s.location = fix;
			if(sensorsRunning && shouldSensorsStop()) {
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
				s.barometricPressure = event.values[0];
			if ((event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD || event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) && gravity != null && geomagnetic != null) {
				float R[] = new float[9];
				float I[] = new float[9];
				boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
				if (success) {
					float orientation[] = new float[3];
					SensorManager.getOrientation(R, orientation);
					s.azimuthDegrees = calculateAzimuthDegrees(orientation[0]);
					s.pitchDegrees = (float) Math.toDegrees(orientation[1]);
					s.rollDegrees = (float) Math.toDegrees(orientation[2]);
				}
			}
			if(sensorsRunning && shouldSensorsStop()) {
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

	private void setHome()
	{
		if(home == null) {
			internalSetHome();
			return;
		}
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setCancelable(true);
		alert.setTitle("Change home. Are you sure?");
		alert.setNegativeButton("No",new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int whichButton) {}});
		alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int whichButton) {
											if(whichButton==AlertDialog.BUTTON_POSITIVE) internalSetHome();
										}
									});
		AlertDialog dialog=alert.show();
	}
	
	private void internalSetHome() {
		home = (SensorStatus) s.clone();
		Button button = (Button) findViewById(R.id.setHome);
		if(home != null) 
			button.setBackgroundColor(Color.RED);
		else
			button.setBackgroundColor(Color.GREEN);
	}
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		System.err.println("---------------------------------------");
		fragment = (CameraFragment) this.getFragmentManager().findFragmentById(R.id.camera_preview);
		cameraTimer = new Timer();
		Button button = (Button) findViewById(R.id.button);
		button.setOnClickListener(new View.OnClickListener(){
				public void onClick(View v) {
					try {
						fragment.takePicture();
					}catch(RuntimeException e){e.printStackTrace();System.err.println(e.getMessage());System.out.println("ok");}
				}
		});
		button = (Button) findViewById(R.id.setHome);
		button.setOnClickListener(new View.OnClickListener(){
				public void onClick(View v) {
				    setHome();
				}
			});
		//start camera timer
		cameraTimer.scheduleAtFixedRate(cameraTimerTask,0,PICTURE_INTERVAL);
		//start sensor timer
		cameraTimer.scheduleAtFixedRate(sensorCheckTimer,0,SENSOR_INTERVAL);
		//setup http server
		server= new LocationServer(this);
		try {
			server.start();
		}
		catch (IOException e) {
			Log.d(TAG,"IOException",e);
			e.printStackTrace();
		}
		
		//setup gps
		locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,  100, 0, onLocationChange,Looper.myLooper());
	}
	
	private String getCameraSetup()
	{
		StringBuilder sb = new StringBuilder();
//		Camera.Parameters p = camera.getParameters();
//		sb.append(" flash:");
//		sb.append(p.getFlashMode());
//		sb.append(" focus:");
//		sb.append(p.getFocusMode());
//		sb.append(" quality:");
//		sb.append(p.getJpegQuality());
//		sb.append(" picture:");
//		sb.append(p.getPictureSize().width);
//		sb.append("×");
//		sb.append(p.getPictureSize().height);
//		sb.append(" prevew:");
//		sb.append(p.getPreviewFrameRate());
//		sb.append(" preview:");
//		sb.append(p.getPreviewSize().width);
//		sb.append("×");
//		sb.append(p.getPreviewSize().height);
		return sb.toString();
	}
	
	private void startSensors() {
		if(sensorsRunning)
			return;
		//setup sensors
		sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		geomagneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
		sensorManager.registerListener(onSensorEventChange, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
		sensorManager.registerListener(onSensorEventChange, geomagneticSensor, SensorManager.SENSOR_DELAY_GAME);
		sensorManager.registerListener(onSensorEventChange, pressureSensor, SensorManager.SENSOR_DELAY_GAME);
	}
	
	
	private void stopSensors() {
		if(!sensorsRunning)
			return;
		locationManager.removeUpdates(onLocationChange);
		sensorManager.unregisterListener(onSensorEventChange);
		//camraTimerTask.cancel();
	}

	@Override
	public void onDestroy() {
		server.stop();
		stopSensors();
		super.onDestroy();
	}
}
