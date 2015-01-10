package com.drucker.flightaware;
import android.location.*;

public class SensorStatus extends Object {
	public Location location = null;
	public float azimuthDegrees = 0;
	public float pitchDegrees = 0;
	public float rollDegrees = 0;
	public float barometricPressure = 0;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	
}
