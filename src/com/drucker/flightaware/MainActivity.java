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
import java.util.*;
//import android.graphics.*;

public class MainActivity extends Activity implements SurfaceHolder.Callback
{

	@Override
	public void surfaceCreated(SurfaceHolder p1) {
		try {
			camera.setDisplayOrientation(90);
			camera.setPreviewDisplay(viewHolder);
			camera.startPreview();
			cameraTakingPicture = false;
			cameraTimer.scheduleAtFixedRate(sensorCheckTimer,0,SENSOR_INTERVAL);
		}
		catch (IOException e)   {e.printStackTrace();}
	}

	@Override
	public void surfaceChanged(SurfaceHolder p1, int p2, int p3, int p4){}

	@Override
	public void surfaceDestroyed(SurfaceHolder p1){}

	private final static String TAG = "MainActivity";
	private final static long SENSOR_SHUTDOWN_TIMEOUT = 1000 * 60 * 5; //5 min
	private final static long PICTURE_INTERVAL = 1000 * 1 * 1; //1 second
	private final static long SENSOR_INTERVAL = 1000 * 5 *1; //5 seconds
	private static Date lastSensorRequest = null;
	private static boolean sensorsRunning = false;
	private boolean cameraTakingPicture = true;
	private static Object sensorSetupSemaphore = new Object();
	

	private static LocationManager locationManager=null;
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
	
	private SurfaceView view = null;
	private SurfaceHolder viewHolder = null;
	
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
	
	public static String getDirection() {
		String directions[] = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
		return directions[(int)Math.round(( ((double)azimuthDegrees % 360) / 45)) % 8];
	}
	
	public static float getGForce() {
		return gForce;
	}
	
	public static byte[] getPicture() {
		return picture;
	}
	
	public static float getBarometricPressure() {
		return barometricPressure;
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
			if(cameraTakingPicture || shouldSensorsStop() || !sensorsRunning)
				return;
			cameraTakingPicture = true;
			camera.takePicture(null,null,null,onPictureCallback);
		}
	};
	
	Camera.PictureCallback onPictureCallback = new Camera.PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			picture = data;
			System.out.println("onPictureCallback");
			camera.startPreview();
			cameraTakingPicture = false;
		}
	};
	
	LocationListener onLocationChange= new LocationListener() {
		public void onLocationChanged(Location fix) {
			location = fix;
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
				barometricPressure = event.values[0];
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

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		cameraTimer = new Timer();
		final Button button = (Button) findViewById(R.id.button);
		button.setOnClickListener(new View.OnClickListener(){
				public void onClick(View v) {
					try {
						camera.takePicture(null,null,null,onPictureCallback);
					}catch(RuntimeException e){e.printStackTrace();System.err.println(e.getMessage());System.out.println("ok");}
		
				}
		});
		//setup camera
		camera = Camera.open(1);
		Camera.Parameters parameters = camera.getParameters();
		List sizes = parameters.getSupportedPictureSizes();
		//System.err.println(sizes.size());
		for (int i=0;i<sizes.size();i++){
			Camera.Size size = (Camera.Size) sizes.get(i);
			System.err.println("Supported Size: " + size.width + "×" + size.height);         
		}
	    List<String> focusModes = parameters.getSupportedFocusModes();
		for (int i=0;i<focusModes.size();i++){
			System.err.println("Supported Focus Modes: " + focusModes.get(i));         
		}
		List<int[]> fpsRangers = parameters.getSupportedPreviewFpsRange();
		for (int i=0;i<fpsRangers.size();i++){
			System.err.println("Supported Preview Frame Rates: " + fpsRangers.get(i)[0] +","+fpsRangers.get(i)[1]);         
		}
		parameters.setPictureSize(320,240);
		//parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
		parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
		parameters.setJpegQuality(CameraProfile.QUALITY_LOW);
		camera.setParameters(parameters);
		view = (SurfaceView) findViewById( R.id.surfaceView);
		//view = new SurfaceView(this);
		//view.setVisibility(view.INVISIBLE);
		viewHolder = view.getHolder();
		viewHolder.addCallback(this);
		viewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		//camera.takePicture(null,null,null,onPictureCallback);
		//setup http server
		server= new LocationServer(this);
		try {
			server.start();
		}
		catch (IOException e) {
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
		sensorManager.registerListener(onSensorEventChange, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
		sensorManager.registerListener(onSensorEventChange, geomagneticSensor, SensorManager.SENSOR_DELAY_GAME);
		sensorManager.registerListener(onSensorEventChange, pressureSensor, SensorManager.SENSOR_DELAY_GAME);
		//setup gps
		Looper.prepare();
		locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,  100, 10, onLocationChange);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,  100, 10, onLocationChange);
		cameraTimer.scheduleAtFixedRate(cameraTimerTask,0,PICTURE_INTERVAL);
	}
	
	
	private void stopSensors() {
		if(!sensorsRunning)
			return;
		locationManager.removeUpdates(onLocationChange);
		sensorManager.unregisterListener(onSensorEventChange);
		//camraTimerTask.cancel();
		//camera.release();
	}

	@Override
	public void onDestroy() {
		server.stop();
		//camera
		stopSensors();
		super.onDestroy();
	}
}
