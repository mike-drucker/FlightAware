package com.drucker.flightaware;
import com.commonsware.cwac.camera.*;
import android.os.*;

public class FlightCameraFragment extends CameraFragment 
{

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		SimpleCameraHost.Builder builder= new SimpleCameraHost.Builder(new FlightCameraHost(getActivity()));
		setHost(builder.useFullBleedPreview(true).build());
	}
	
}
