package com.drucker.flightaware;
import com.commonsware.cwac.camera.SimpleCameraHost;
import com.commonsware.cwac.camera.*;
import android.content.*;
import android.app.*;
import android.hardware.Camera.*;
import android.hardware.*;
import java.util.*;
import android.media.*;

public class FlightCameraHost extends SimpleCameraHost
{
	private Context context;
	public static byte[] data = null;

	public FlightCameraHost(Context context) {
		super(context);
		this.context = context;
	}

	@Override
	public Camera.Parameters adjustPictureParameters(PictureTransaction xact, Camera.Parameters parameters)
	{
		// TODO: Implement this method
	    List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
		for(Camera.Size size : sizes) {
			System.err.println("Supported Size: " + size.width + "×" + size.height);
			parameters.setPictureSize(size.width, size.height);
			if(size.width < 320)
				break;
		}
		List<String> focusModes = parameters.getSupportedFocusModes();
		if(focusModes.contains("infinity"))
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
		else if(focusModes.contains("fixed"))
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
		for (int i=0;i<focusModes.size();i++){
			System.err.println("Supported Focus Modes: " + focusModes.get(i));         
		}
		//use lowest supported preview framerate
		List<int[]> fpsRanges = parameters.getSupportedPreviewFpsRange();
		//parameters.setPreviewFrameRate(fpsRanges.get(Camera.Parameters.PREVIEW_FPS_MIN_INDEX)[0]);
		for (int i=0;i<fpsRanges.size();i++){
			System.err.println("Supported Preview Frame Rates: " + fpsRanges.get(i)[0] +","+fpsRanges.get(i)[1]);         
		}
		List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
		for (int i=0;i<previewSizes.size();i++){
			System.err.println("Supported Preview Sizes: " + previewSizes.get(i).width + "×" +previewSizes.get(i).width);         
		}
		//parameters.setPreviewSize(previewSizes.get(previewSizes.size()-1).width, previewSizes.get(previewSizes.size()-1).height);
		//parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
		parameters.setJpegQuality(CameraProfile.QUALITY_LOW);
		return super.adjustPictureParameters(xact, parameters);
	}

	

	@Override
	public void saveImage(PictureTransaction xact, byte[] image)
	{
		//super.saveImage(xact, image);
		data = image;
	}
}
