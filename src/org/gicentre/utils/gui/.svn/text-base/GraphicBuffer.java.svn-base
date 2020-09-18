package org.gicentre.utils.gui;


import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import org.gicentre.utils.move.ZoomPanListener;
import org.gicentre.utils.move.ZoomPan;
import org.gicentre.utils.move.ZoomPanState;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PGraphicsJava2D;
import processing.core.PImage;
import processing.core.PVector;

/**Graphic Buffer
 * 
 * Easy-to-use offscreen buffer for use with complex graphics that take a long time to draw.
 * This class can capture standard Processing drawing methods to an off-screen bitmap buffer
 * which only needs to be updated when something changes.
 * 
 * To use, simply enclose normal Processing drawing code between startCapture() and stopCapture(),
 * only doing this when the update flag has been set. Then call the draw() method of GraphicBuffer.
 * Called setFlagToUpdate() to set the update flag. Automatically handles ZoomPan interactions.
 * 
 * Instances of GraphicBuffer are transparent by default, so these can be superimposed to handle
 * groups (layers) of graphics so that changes in one group does not require those in other 
 * groups to be redrawn.
 * 
 * Dynamic content can be drawn over the top, particular useful for mouse highlighting and
 * tooltips.
 * 
 * Particularly useful for sketches that use ZoomPan. During zooming and panning, the (bitmap) buffer
 * will scale appropriately. After zooming and panning, a redraw will be automatically triggers.
 * Multiple instances can share the same ZoomPan instance.
 * 
 * The sketch will still freeze when drawing to the buffer takes place, but this will only happen
 * for the (relatively) few times this happens. Use ThreadedGraphicBuffer to avoid such freezing.
 * 
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


public class GraphicBuffer implements ZoomPanListener{

	private PGraphics bufferImage;          //the buffer
	private PGraphics oldG;                 //the old graphic context
	private PApplet applet;                 //the sketch 
	private Rectangle screenBounds;         //the bounds of the buffer (in screen coordinates)
	private Rectangle2D boundsAtLastBuffer; //keeps track of the zoompan state at the last draw
	private ZoomPan zoomPan;                //zoompan
	boolean needToUpdate=true;              //flag indicating whether content needs to be redrawn 
	
	
	/**
	 * Creates a new graphic buffer that fills the sketch screen
	 * 
	 * @param applet
	 * @param zoomPan  ZoomPan used to draw on this buffer - set to null if none used
	 */	 
	public GraphicBuffer(PApplet applet, ZoomPan zoomPan){
		this(applet, zoomPan,new Rectangle(0,0,applet.width,applet.height));
	}

	
	/**
	 * Creates a new graphic buffer at the specified screen area for use with ZoomPan
	 * 
	 * @param applet
	 * @param zoomPan  ZoomPan used to draw on this buffer - set to null if none used
	 * @param screenBounds  Screen bounds
	 * 	 */	 
	public GraphicBuffer(PApplet applet, ZoomPan zoomPan, Rectangle screenBounds){
		this.applet=applet;
		this.screenBounds=screenBounds;
		this.zoomPan=zoomPan;
		
		//create the off-screen buffer
		bufferImage=applet.createGraphics(screenBounds.width,screenBounds.height,PApplet.JAVA2D);
		//give it a transparent background
		bufferImage.beginDraw();
		bufferImage.background(0,0,0,0);
		bufferImage.endDraw();
		
		//if a zoompan is specified listen to it so that it can update itself at the
		//end of zooming or panning
		if (zoomPan!=null)
			zoomPan.addZoomPanListener(this);
	}
	
	/** Returns the current viewport based on zoom/pan in original coordinates.
	 * 
	 * @return The current viewport
	 */
	public Rectangle2D getViewPort(){
		//Find the coordinates of the top left and bottom right corners
		PVector topLeft=zoomPan.getDispToCoord(new PVector((float)screenBounds.getMinX(),(float)screenBounds.getMinY()));
		PVector bottomRight=zoomPan.getDispToCoord(new PVector((float)screenBounds.getMaxX(),(float)screenBounds.getMaxY()));
		Rectangle2D coordBounds=new Rectangle2D.Float(topLeft.x,topLeft.y,bottomRight.x-topLeft.x,bottomRight.y-topLeft.y);
		return coordBounds;
	}

	/** Returns the current viewport based on specific ZoomPanState (usually that at the start of the
	 * sketch'w draw loop) in original coordinates.
	 * 
	 * @return The current viewport
	 */
	public Rectangle2D getViewPort(ZoomPanState zoomPanState){
		//Find the coordinates of the top left and bottom right corners
		PVector topLeft=zoomPanState.getDispToCoord(new PVector((float)screenBounds.getMinX(),(float)screenBounds.getMinY()));
		PVector bottomRight=zoomPanState.getDispToCoord(new PVector((float)screenBounds.getMaxX(),(float)screenBounds.getMaxY()));
		Rectangle2D coordBounds=new Rectangle2D.Float(topLeft.x,topLeft.y,bottomRight.x-topLeft.x,bottomRight.y-topLeft.y);
		return coordBounds;
	}


	/** Starts capturing standard Processing drawing functions
	 * Call this before when you want to redraw the buffered content, then
	 * draw using the usual Processing functions.
	 * 
	 * Use zoomPan.transform() in the sketch if you're using a zoompan instance with this
	 */
	public void startCapture(){
		//reset the flag
		needToUpdate=false;
		//keep a record of the zoom scale at last draw, for scaling the buffer image
		//during zoom/pan
		if (zoomPan!=null){
			PVector p1 = zoomPan.getDispToCoord(new PVector((float)screenBounds.getMinX(),(float)screenBounds.getMinY()));
			PVector p2 = zoomPan.getDispToCoord(new PVector((float)screenBounds.getMaxX(),(float)screenBounds.getMaxY()));
			boundsAtLastBuffer=new Rectangle2D.Float(p1.x,p1.y,p2.x-p1.x,p2.y-p1.y);
		}
		
		//switch the graphic context to that of the buffer
		oldG = applet.g;
		applet.g=bufferImage;
		bufferImage.beginDraw();
		
		//use smooth() if the original sketch does
		if (oldG.smooth){
			applet.g.smooth();
		}
		
		//copy existing sketch styles across
		applet.g.fill(oldG.fillColor);
		if (!oldG.fill)
			applet.g.noFill();
		applet.g.stroke(oldG.strokeColor);
		if (!oldG.stroke)
			applet.g.noStroke();
		applet.g.tint(oldG.tintColor);
		applet.g.strokeWeight(oldG.strokeWeight);
		applet.g.strokeCap(oldG.strokeCap);
		applet.g.strokeJoin(oldG.strokeJoin);
		applet.g.imageMode(oldG.imageMode);
		applet.g.rectMode(oldG.rectMode);
		applet.g.ellipseMode(oldG.ellipseMode);
		applet.g.shapeMode(oldG.shapeMode);
		applet.g.colorMode(oldG.colorMode);
		applet.g.textAlign(oldG.textAlign);
		if (oldG.textFont!=null)
			applet.g.textFont(oldG.textFont);
		applet.g.textMode(oldG.textMode);
		applet.g.textSize(oldG.textSize);
		applet.g.textLeading(oldG.textLeading);
		applet.g.emissive(applet.color(oldG.emissiveR,oldG.emissiveG,oldG.emissiveB));
		applet.g.specular(applet.color(oldG.specularR,oldG.specularG,oldG.specularB));
		applet.g.shininess(oldG.shininess);
		applet.g.ambient(applet.color(oldG.ambientR,oldG.ambientG,oldG.ambientB));
		
		//translate to the x and y offset
		applet.g.setMatrix(oldG.getMatrix());
		applet.g.pushMatrix();
		applet.g.translate(-screenBounds.x,-screenBounds.y);
		applet.background(255,0);//transparent background

	}
	
	/** Stop capturing drawn content
	 * You MUST call this when you've finished, otherwise it will continue to capture drawn content!
	 */
	public void stopCapture(){
		bufferImage.endDraw();
		applet.g.popMatrix();
		oldG.setMatrix(applet.g.getMatrix());
		//restore graphic content
		applet.g=oldG;
	}
	
	
	/**Draws the buffered content
	 * Will zoom the bitmap appropriately if the zooming/panning has changed since the last redraw
	 * 
	 * If using ZoomPan.transform(), you must reset this before calling this method by using pushMatrix
	 * before zoomPan.transform() and popMatrix afterwards, before calling this method
	 * 
	 */
	public void draw(){
		if (zoomPan!=null)
			this.draw(zoomPan.getZoomPanState());
		else{
			this.draw(null);
		}
	}

	/**Draws the buffered content, using a particular ZoomPanScale - usually that at the start of
	 * the sketch's draw loop. Will zoom the bitmap appropriately if the zooming/panning has changed
	 * since the last redraw
	 * 
	 * If using ZoomPan.transform(), you must reset this before calling this method by using pushMatrix
	 * before zoomPan.transform() and popMatrix afterwards, before calling this method
	 * 
	 */
	public void draw(ZoomPanState zoomPanState){
		if (boundsAtLastBuffer==null)
			applet.image(bufferImage,screenBounds.x,screenBounds.y);
		else{			

			//clip to the bounds
			startClipping();
			
			//Calculate where to draw the image to take into account zooming/panning since the last draw
			PVector p1 = zoomPanState.getDispToCoord(new PVector((float)screenBounds.getMinX(),(float)screenBounds.getMinY()));
			PVector p2 = zoomPanState.getDispToCoord(new PVector((float)screenBounds.getMaxX(),(float)screenBounds.getMaxY()));
			float x=PApplet.map((float)boundsAtLastBuffer.getMinX(),p1.x,p2.x,(float)screenBounds.getMinX(),(float)screenBounds.getMaxX());
			float y=PApplet.map((float)boundsAtLastBuffer.getMinY(),p1.y,p2.y,(float)screenBounds.getMinY(),(float)screenBounds.getMaxY());
			float w=PApplet.map((float)boundsAtLastBuffer.getWidth(),0,p2.x-p1.x,0,(float)screenBounds.getWidth());
			float h=PApplet.map((float)boundsAtLastBuffer.getHeight(),0,p2.y-p1.y,0,(float)screenBounds.getHeight());
			applet.image(bufferImage,x,y,w,h);
			
			//stop clipping
			stopClipping();
		}
	}
	
	/** Gets the screen bounds
	 * 
	 * @return Screenbounds
	 */
	public Rectangle getScreenBounds(){
		return this.screenBounds;
	}
	
	/** Set flag to update drawn content. This ONLY affects the return value of needToUpdate()
	 * which needs to be monitored by the sketch
	 * 
	 */
	public void setUpdateFlag(){
		this.needToUpdate=true;
	}
	
	/** Finds whether the content has been flagged for update redraw
	 * Reports false when/if zooming/panning
	 * @return  Whether the content should be redrawn
	 */
	public boolean needToUpdate(){
		if (zoomPan!=null && (zoomPan.isPanning() || zoomPan.isZooming()))
			return false;
		else
			return needToUpdate;
	}
	
	/**Gets the buffer contents as an image
	 * 
	 * @return
	 */
	public PImage getGraphics(){
		return bufferImage;
	}

	/**Set the update flag after zooming has finished
	 * 
	 */
	public void zoomEnded() {
		setUpdateFlag();
		
	}

	/**Set the update flag after panning has finished
	 * 
	 */
	public void panEnded() {
		setUpdateFlag();
	}
	
	/** Start clipping all drawn content to the screen bounds of this map
	 * Only works with JAVA2D
	 * 
	 */
	public void startClipping(){
		if (applet.g instanceof PGraphicsJava2D)
			((PGraphicsJava2D)applet.g).g2.setClip(screenBounds);
		else
			System.err.println("Cannot clip with this renderer.");
	}

	/** Stop clipping drawn content
	 * Only works with JAVA2D
	 * 
	 */
	public void stopClipping(){
		if (applet.g instanceof PGraphicsJava2D)
			((PGraphicsJava2D)applet.g).g2.setClip(null);
	}
}
