package org.gicentre.tests;

import org.gicentre.utils.gui.BusyIcon;

import processing.core.PApplet;

public class BusyIconTest extends PApplet{
	BusyIcon busyIcon;
	
	public void setup(){
		busyIcon=new BusyIcon();
	}
	
	public void draw(){
		busyIcon.draw(this, width/2, height/2,30);
	}
}
