package com.drucker.flightaware;
import com.drucker.flightaware.NanoHTTPD.*;
import android.location.*;
import org.json.*;
import android.util.*;
import android.content.res.*;
import java.io.*;
import android.content.*;

public class LocationServer extends NanoHTTPD
{
	private final String TAG = "LocationServer";
	private Context context = null;
	public LocationServer(Context context)
	{super(8080);this.context = context;}

	@Override
	public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
		//System.out.println(session.getUri());    
		
		switch(session.getUri())   {
			case "/": case "/index": case "/index.html": case "/index.htm": return getAssetResource("index","text/html");
			case "/data": return getData();
			case "/picture": return getPicture();
			case "/jquery" : return getAssetResource("jquery","application/javascript");
			default: return getRedirect("/");
		}
	}
	
	private NanoHTTPD.Response getAssetResource(String resourceFileName, String mimeType) {
		try {
			InputStream inputStream = context.getAssets().open(resourceFileName, AssetManager.ACCESS_RANDOM);
			return new NanoHTTPD.Response(Response.Status.OK, mimeType, inputStream);
		}
		catch (IOException e) {e.printStackTrace();Log.d(TAG,"IOException",e);}
		return null;
	}
	
	private NanoHTTPD.Response getPicture() {
		byte[] data = MainActivity.getPicture();
		MainActivity.setSensorRequested();
		if(data==null)
			return getAssetResource("blankjpg","image/jpeg");
		return new Response(Response.Status.OK,"image/jpeg", new ByteArrayInputStream(data));
	}
	
	private NanoHTTPD.Response getData() {
		MainActivity.setSensorRequested();
		Location location = MainActivity.getLocation();
		float azimuthDegrees = MainActivity.getAzimuthDegrees();
		String direction = MainActivity.getDirection();
		float pitchDegrees = MainActivity.getPitchDegrees();
		float rollDegrees = MainActivity.getRollDegrees();
		float gForce = MainActivity.getGForce();
		float barometricPressure = MainActivity.getBarometricPressure();
		JSONObject obj = new JSONObject();
		try {
			obj.accumulate("azi", azimuthDegrees);
			obj.accumulate("dir", direction);
			obj.accumulate("pch", pitchDegrees);
			obj.accumulate("rol", rollDegrees);
			obj.accumulate("g", gForce);
			obj.accumulate("ber", barometricPressure);
			if(location==null) {
				obj.accumulate("gpsErr","No Location Fix");
				return new Response(obj.toString());
			}
			obj.accumulate("lat",location.getLatitude());
			obj.accumulate("lon", location.getLatitude());
			obj.accumulate("alt", location.getAltitude());
			obj.accumulate("ber", location.getBearing());
			obj.accumulate("spd", location.getSpeed());
			return new Response(obj.toString());
		}
		catch (JSONException e) {
			Log.d(TAG,"JSONException",e);
			e.printStackTrace();
			return new Response(Response.Status.INTERNAL_ERROR, MIME_HTML, String.format("Failed to build data to send, '%1$s'", e.getMessage()));
		}
	}
	
	private static Response getRedirect(String uri) {
		Response r = new Response(Response.Status.REDIRECT,MIME_PLAINTEXT,"redirect to "+ uri);
		r.addHeader("location",uri);
		return r;
	}
	
}
