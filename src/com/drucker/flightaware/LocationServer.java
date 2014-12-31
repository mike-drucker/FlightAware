package com.drucker.flightaware;
import com.drucker.flightaware.NanoHTTPD.*;
import android.location.*;
import org.json.*;

public class LocationServer extends NanoHTTPD
{
	public LocationServer()
	{super(8080);}

	@Override
	public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session)
	{
		Location location = MainActivity.getLocation();
		float azimuthDegrees = MainActivity.getAzimuthDegrees();
		if(location==null)
			return new Response(Response.Status.NO_CONTENT, MIME_HTML,"No Location Fix");
		JSONObject obj = new JSONObject();
		try {
			obj.accumulate("lat",location.getLatitude());
			obj.accumulate("lon", location.getLatitude());
			obj.accumulate("alt", location.getAltitude());
			obj.accumulate("ber", location.getBearing());
			obj.accumulate("spd", location.getSpeed());
			obj.accumulate("azi", azimuthDegrees);
			return new Response(obj.toString());
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return new Response(Response.Status.INTERNAL_ERROR, MIME_HTML, String.format("Failed to build data to send, '%1$s'", e.getMessage()));
		}
	}
	
}
