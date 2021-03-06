package org.gicentre.utils.gui;


import java.applet.Applet;

import processing.core.PApplet;
import processing.core.PConstants;

/**Draws an animated graphic indicting something is in progress. Calling draw() draws one
 * frame and advance the animation frame by one. Can only be used for processes that are NOT in
 * Processing's "animation thread".
 * 
 * Can be used with ThreadedGraphicsBuffer
 * 
 * @author Aidan Slingsby, giCentre, City University London.
 * @version 1.0, August 2011 
 *
 */

/* This file is part of giCentre utilities library. gicentre.utils is free software: you can 
 * redistribute it and/or modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * gicentre.utils is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this
 * source code (see COPYING.LESSER included with this source code). If not, see 
 * http://www.gnu.org/licenses/.
 */

public class BusyIcon{

	int startColour=150;
	int numElements=12;
	int colourIncrement=(255-150)/(numElements-1);
	
	/**Constructor, takes no arguments
	 * 
	 */
	public BusyIcon(){
	}
	

	/**Draws one frame to the applet, then advance the frame counter. Animation is achieved
	 * through multiple calls to this
	 * 
	 * @param applet The sketch to draw to
	 * @param x  Centred at this X screen location  
	 * @param y  Centred at this Y screen location
	 * @param width  width in pixels
	 */
	public void draw(PApplet applet, float x, float y, float width) {
		x-=width/2;
		y-=width/2;
		applet.pushMatrix();
		applet.pushStyle();
		
		applet.fill(255,50);
		applet.noStroke();
		applet.ellipseMode(PConstants.CORNER);
		applet.strokeWeight(width/14f);
		applet.ellipse(x, y, width, width);
		applet.strokeCap(PConstants.ROUND);
		applet.translate(x+width/2, y+width/2);
		int colour=startColour;
		for (int i=0;i<numElements;i++) {
			applet.rotate((2*(float)Math.PI)/numElements);
			applet.stroke(colour);
			applet.line(width/3, width/3, width/8,width/8);
			colour+=colourIncrement;
			if (colour>255)
				colour=150;
		}
		applet.popMatrix();
		applet.popStyle();
		startColour++;
		if (startColour>255)
			startColour=150;
	}
}
