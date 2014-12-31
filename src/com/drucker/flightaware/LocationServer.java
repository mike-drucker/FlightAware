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
	public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session)
	{
		switch(session.getUri())
		{
			case "/": case "/index": case "/index.html": case "/index.htm": return getAssetResource("index","text/html");
			case "/data": return getLocation();
			case "/jquery" : return getAssetResource("jquery","application/javascript");
			default: return null;
		}
	}
	
	private NanoHTTPD.Response getResource(String resourceName, String mimeType)
	{
		Resources r = context.getResources();
		int id = r.getIdentifier(resourceName,null,null);
		InputStream inputStream = r.openRawResource(id);
		return new NanoHTTPD.Response(Response.Status.OK, mimeType, new BufferedInputStream(inputStream));
	}
	
	private NanoHTTPD.Response getAssetResource(String resourceFileName, String mimeType)
	{
		InputStream inputStream = context.getAssets().open(resourceFileName);
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
