package com.drucker.flightaware;
import android.location.*;

public class SensorStatus extends Object {
	public Location location = null;
	public float azimuthDegrees = 0;
	public float pitchDegrees = 0;
	public float rollDegrees = 0;
	public float barometricPressure = 0;

	@Override
	public Object clone() {
		SensorStatus copy = new SensorStatus();
		copy.azimuthDegrees = this.azimuthDegrees;
		copy.barometricPressure = this.barometricPressure;
		copy.location = new Location(this.location);
		copy.pitchDegrees = this.pitchDegrees;
		copy.rollDegrees = this.rollDegrees;
		return copy;
	}
	
}
