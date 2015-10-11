package com.emtronics.quadbotdroid;

public class ControlsState {
	public int leftX;
	public int leftY;
	public int rightX;
	public int rightY;
	public int velocity;

	@Override
	public String toString() {
		return "ControlsState [leftX=" + leftX + ", leftY=" + leftY
				+ ", rightX=" + rightX + ", rightY=" + rightY + ", velocity="
				+ velocity + "]";
	}

}
