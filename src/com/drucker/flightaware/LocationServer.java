package com.drucker.flightaware;
import com.drucker.flightaware.NanoHTTPD.*;
import android.location.*;
import org.json.*;
import android.util.*;

public class LocationServer extends NanoHTTPD
{
	private final String TAG = "LocationServer";
	
	public LocationServer()
	{super(8080);}

	@Override
	public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session)
	{
		switch(session.getUri())
		{
			case "/", "/index","/index.html","/index.htm": return getResource("index","text/html");
			case "/data": return getLocation();
			case "/jquery" : return getResource("jquery","application/javascript");
			case return null;
		}
	}
	
	private NanoHTTPD.Response getResource(String resourceName, string mimeType)
	{
		Resources r = getResources();
		int id = r.getIdentifier(resourceName);
		InputStream inputStream = r.openRawResource(id);
		return new NanoHTTPD.Response(Response.Status.OK, mimeType, inputStream);
	}
	
	private NanoHTTPD.Response getLocation()
	{
		Location location = MainActivity.getLocation();
		float azimuthDegrees = MainActivity.getAzimuthDegrees();
		String direction = MainActivity.getDirection();
		JSONObject obj = new JSONObject();
		try {
			obj.accumulate("azi", azimuthDegrees);
			obj.accumulate("dir", direction);
			if(location==null)
			{
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
		catch (JSONException e)
		{
			Log.d(TAG,"JSONException",e);
			e.printStackTrace();
			return new Response(Response.Status.INTERNAL_ERROR, MIME_HTML, String.format("Failed to build data to send, '%1$s'", e.getMessage()));
		}
	}
	
}
