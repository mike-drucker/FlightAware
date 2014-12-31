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
		Location location = MainActivity.getLocation();
		float azimuthDegrees = MainActivity.getAzimuthDegrees();
		JSONObject obj = new JSONObject();
		try {
			obj.accumulate("azi", azimuthDegrees);
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
