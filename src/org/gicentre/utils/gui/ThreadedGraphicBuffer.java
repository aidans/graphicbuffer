package org.gicentre.utils.gui;


import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.gicentre.utils.move.ZoomPanListener;
import org.gicentre.utils.move.ZoomPan;
import org.gicentre.utils.move.ZoomPanState;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PGraphicsJava2D;
import processing.core.PImage;
import processing.core.PVector;

/**Graphic Buffer that draws content in a separate thread
 * 
 * Offscreen buffer for use with complex graphics that take a long time to draw.
 * An improvement over GraphicBuffer because the sketch does not freeze when 
 * content is drawn onto the buffer. This is achieved by drawing the content in parallel
 * in a different thread. This makes its use less simple than for GraphicBuffer and
 * introduced problems of synchronisation that are associated with threaded programming

 * Use is different to that of GraphicBuffer
 * 
 *   - Put the code for drawing to the buffer into a class that implements ThreadedDraw in
 *     a method called threadedDraw(). If only one instance of ThreadedGraphicBuffer is used
 *     in the sketch, this can be the sketch itself. Otherwise, implementing as inner classes
 *     enables the code to access the sketch's variables and methods
 *   - threadDraw provides a "canvas" to draw on. Simply prepend the processing drawing functions
 *     with "canvas.", (e.g. canvas.fill(255);)
 *   - in the draw loop, simply use threadedGraphicBuffer.draw(). If will call the code in
 *     threadedDraw() automatically if the update flag is set. It will also cancel any
 *     drawing that is currently taking place.
 *   - Whilst it's drawing, the previous buffered image will be displayed. Use isDrawingInThread
 *     to find out whether it's being updated. You may like to use the BusyIcon to indicate this
 *   - use setUpdateFlag() to flag for redraw
 * 
 * As with GraphicBuffer, multiple ThreadedGraphical buffers can be used for mangagin groups (layers)
 * of graphical objects.
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


public class ThreadedGraphicBuffer implements ZoomPanListener{

	private PImage oldImage;                       //the buffered image
	private PImage image;                       //the buffered image
	private PApplet applet;						//the sketch 
	private Rectangle screenBounds; 			//the bounds of the buffer (in screen coordinates)
	private Rectangle2D boundsAtLastLastBuffer; 	//keeps track of the zoompan state at the last draw
	private Rectangle2D boundsAtLastBuffer; 	//keeps track of the zoompan state at the last draw
	private ZoomPan zoomPan;
	private boolean needToUpdate=true; 			//flag to update 
	private ThreadedDraw threadedDrawToGraphicBuffer; //The class containing the draw code
	private Thread thread=null;                 //The thread in which the drawing takes place
	private boolean isDrawingInThread=false;    //indicates whether drawing on the on-screen buffer is taking place
	private int lastMouseX=-1;
	private int lastMouseY=-1;
	private Set<ThreadedGraphicBufferListener> listeners;
	private boolean updateDuringZoomPan=false;
	private boolean useFade=false;              //fade drawn image in       
	private int tintValue=0;  					//amount of fade (if fading)
	private int fadeIncrement;      			//

	private List<PGraphics> tempImages=Collections.synchronizedList(new ArrayList<PGraphics>());
	private Set<PGraphics> tempImagesInUse=Collections.synchronizedSet(new HashSet<PGraphics>());

	
	/**
	 * Creates a new graphic buffer that fills the screen
	 *  
	 * @param applet  The sketch
	 * @param threadedDrawToGraphicBuffer  The class containing the threadedDraw code 
	 */	 
	public ThreadedGraphicBuffer(PApplet applet,ThreadedDraw threadedDrawToGraphicBuffer){
		this(applet,null,threadedDrawToGraphicBuffer,new Rectangle(0,0,applet.width,applet.height));
	}

	
	/**
	 * Creates a new graphic buffer in the specified screen area
	 * 
	 * @param applet
	 * @param threadedDrawToGraphicBuffer  The class containing the threadedDraw code 
	 * @param screenBounds  Screen area for the graphic buffer
	 */	 
	public ThreadedGraphicBuffer(PApplet applet,ThreadedDraw threadedDrawToGraphicBuffer, Rectangle screenBounds){
		this(applet,null,threadedDrawToGraphicBuffer,screenBounds);
	}
	
	/**
	 * Creates a new graphic buffer that fills the screen using a ZoomPan
	 * 
	 * @param applet  The sketch
	 * @param zoomPan  ZoomPan used to draw on this buffer - set to null if none used
	 * @param threadedDrawToGraphicBuffer  The class containing the threadedDraw code 
	 */	 
	public ThreadedGraphicBuffer(PApplet applet,ZoomPan zoomPan,ThreadedDraw threadedDrawToGraphicBuffer){
		this(applet,zoomPan,threadedDrawToGraphicBuffer,new Rectangle(0,0,applet.width,applet.height));
	}

	
	/**
	 * Creates a new graphic buffer in the specified screen area using a ZoomPan
	 * 
	 * @param applet
	 * @param zoomPan  ZoomPan used to draw on this buffer - set to null if none used
	 * @param threadedDrawToGraphicBuffer  The class containing the threadedDraw code 
	 * @param screenBounds  Screen area for the graphic buffer
	 */	 
	public ThreadedGraphicBuffer(PApplet applet,ZoomPan zoomPan,ThreadedDraw threadedDrawToGraphicBuffer, Rectangle screenBounds){
		this.applet=applet;
		this.screenBounds=screenBounds;
		this.zoomPan=zoomPan;
		this.threadedDrawToGraphicBuffer=threadedDrawToGraphicBuffer;
		
		//create the image
		oldImage = applet.createImage(screenBounds.width, screenBounds.height,PConstants.ARGB);
		image = applet.createImage(screenBounds.width, screenBounds.height,PConstants.ARGB);

		//if a zoompan is specified listen to it so that it can update itself at the
		//end of zooming or panning
		if (zoomPan!=null)
			zoomPan.addZoomPanListener(this);
		
		this.listeners=new HashSet<ThreadedGraphicBufferListener>();
		
	}

	
	public void setUpdateDuringZoomPan(boolean updateDuringZoomPan){
		this.updateDuringZoomPan=updateDuringZoomPan;
	}
	
	/**Draws content
	 * If flagged for update/redraw, the code in threadedDraw() will be run (incomplete threaded
	 * drawing will be cancelled).
	 * Will draw the most recent completed buffered content. If ZoomPan is used, this will be
	 * appropriately positioned and scaled.
	 */
	public void draw(){
		if (this.zoomPan==null){
			this.draw(null,null);
		}
		else{
			this.draw(zoomPan.getZoomPanState(),null);
		}
	}
	
	/**Draws content
	 * If flagged for update/redraw, the code in threadedDraw() will be run (incomplete threaded
	 * drawing will be cancelled).
	 * Will draw the most recent completed buffered content. If ZoomPan is used, this will be
	 * appropriately positioned and scaled.
	 */
	public void draw(ZoomPanState zoomPanState){
		this.draw(zoomPanState,null);
	}

	/**Draws content
	 * If flagged for update/redraw, the code in threadedDraw() will be run (incomplete threaded
	 * drawing will be cancelled).
	 * Will draw the most recent completed buffered content.
	 */
	public void draw(Object extraInfo){
		this.draw(null,extraInfo);
	}
	
	/**Draws content, with extra information which will be passed to the threadDraw method
	 * If flagged for update/redraw, the code in threadedDraw() will be run (incomplete threaded
	 * drawing will be cancelled).
	 * Will draw the most recent completed buffered content. If ZoomPan is used, this will be
	 * appropriately positioned and scaled.
	 */
	public void draw(ZoomPanState zoomPanState, Object drawData){
		if (needToUpdate()){
			needToUpdate=false;   //reset
			if (thread!=null){    //cancel existing threaded drawing
				thread.interrupt();
				isDrawingInThread=false;
			}
			thread=new Thread(new DrawInSeparateThread(zoomPanState,drawData,useFade)); //create a new thread, and a new runnable task which will call threadDraw()
			thread.start(); //start this thread
		}
		//if no zoompan is used, just draw the buffer to the screen
		if (boundsAtLastBuffer==null){
			if (useFade){
				applet.g.pushStyle();
				if (tintValue<255){
					applet.g.image(oldImage,screenBounds.x,screenBounds.y);
				}
				if (tintValue==255)
					applet.g.noTint();
				else
					applet.g.tint(255,tintValue);
				applet.g.image(image,screenBounds.x,screenBounds.y);
				applet.g.popStyle();
				if (tintValue<255)
					tintValue+=fadeIncrement;
				if (tintValue>255)
					tintValue=255;
			}
			else{
				applet.g.image(image,screenBounds.x,screenBounds.y);
			}
		}
		else{		
			//otherwise work out how to scale it (with respect to the zoomstate on the last update)

			//clip to the bounds
			startClipping();

			//Calculate where to draw the image to take into account zooming/panning since the last draw
			PVector p1 = zoomPanState.getDispToCoord(new PVector((float)screenBounds.getMinX(),(float)screenBounds.getMinY()));
			PVector p2 = zoomPanState.getDispToCoord(new PVector((float)screenBounds.getMaxX(),(float)screenBounds.getMaxY()));

			float x=PApplet.map((float)boundsAtLastBuffer.getMinX(),p1.x,p2.x,(float)screenBounds.getMinX(),(float)screenBounds.getMaxX());
			float y=PApplet.map((float)boundsAtLastBuffer.getMinY(),p1.y,p2.y,(float)screenBounds.getMinY(),(float)screenBounds.getMaxY());
			float w=PApplet.map((float)boundsAtLastBuffer.getWidth(),0,p2.x-p1.x,0,(float)screenBounds.getWidth());
			float h=PApplet.map((float)boundsAtLastBuffer.getHeight(),0,p2.y-p1.y,0,(float)screenBounds.getHeight());

			if (useFade){
				float x1=PApplet.map((float)boundsAtLastLastBuffer.getMinX(),p1.x,p2.x,(float)screenBounds.getMinX(),(float)screenBounds.getMaxX());
				float y1=PApplet.map((float)boundsAtLastLastBuffer.getMinY(),p1.y,p2.y,(float)screenBounds.getMinY(),(float)screenBounds.getMaxY());
				float w1=PApplet.map((float)boundsAtLastLastBuffer.getWidth(),0,p2.x-p1.x,0,(float)screenBounds.getWidth());
				float h1=PApplet.map((float)boundsAtLastLastBuffer.getHeight(),0,p2.y-p1.y,0,(float)screenBounds.getHeight());

				if (tintValue<255){
					applet.g.image(oldImage,x1,y1,w1,h1);
				}
				applet.g.pushStyle();
				if (tintValue<255){
					applet.g.tint(255,tintValue);
				}
				else{
					applet.g.noTint();
				}
				applet.g.image(image,x,y,w,h);
				applet.g.popStyle();
				
//				image.get().save("/Users/sbbb717/Desktop/temp/sdssd_"+applet.frameCount+".png");
				
				if (tintValue<255)
					tintValue+=fadeIncrement;
				if (tintValue>255)
					tintValue=255;
			}
			else
				applet.g.image(image,x,y,w,h);

			
			//stop clipping
			stopClipping();
		}
//		System.out.println(tempImagesInUse.size()+"/"+tempImages.size());
	}
	
	/** Reports whether drawing to the off-screen buffer is in progress
	 * 
	 * This can be used to display a message explaining that drawing is in progress. Try
	 * org.gicentre.utils.gui.BusyIcon
	 * 
	 * @return
	 */
	public boolean isDrawingInThread(){
		return isDrawingInThread;
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

	/** Returns the current viewport based on specific ZoomPanState (usually that at the start
	 * of the sketch's draw loop) in original coordinates.
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

	/** Gets the screen bounds
	 * 
	 * @return Screenbounds
	 */
	public Rectangle getScreenBounds(){
		return this.screenBounds;
	}
	
	/** Set flag to update drawn content.
	 * 
	 * If true, threadedDraw() will be automatically called, the next time this
	 * is drawn
	 * 
	 */
	public void setUpdateFlag(){
		this.needToUpdate=true;
	}
	
	/**Reports whether the buffer is marked to update itself
	 * 
	 * @return Whether the buffer will update itself
	 */
	public boolean getUpdateFlag(){
		return this.needToUpdate;
	}

	/** Reports whether the fade effect is in use
	 * 
	 * @return
	 */
	public boolean getUseFadeEffect(){
		return useFade;
	}
	
	/** Sets whether the buffered contents fades in over the old
	 * 
	 * This doesn't work properly if the background is transparent,
	 * so it will automatically be set to white. You may subsequently
	 * set it in your code, but best not use transparency  
	 * 
	 * @param useFade
	 */
	public void setUseFadeEffect(boolean useFade){
		this.setUseFadeEffect(useFade, 10);
	}

	/** Sets whether the buffered contents fades in over the old
	 * 
	 * 
	 * @param useFade
	 */
	public void setUseFadeEffect(boolean useFade, int numFadeSteps){
		this.useFade=useFade;
		this.fadeIncrement=(int)(255f/numFadeSteps);
		oldImage = applet.createImage(screenBounds.width, screenBounds.height,PConstants.ARGB);
	}

	
	/**Adds a listener which will be notified when a new buffered image is complete
	 * 
	 * @param threadedGraphicBufferListener
	 */
	public void addListener(ThreadedGraphicBufferListener threadedGraphicBufferListener){
		this.listeners.add(threadedGraphicBufferListener);
	}

	/**Removes a listener
	 * 
	 * @param threadedGraphicBufferListener
	 */
	public void removeListener(ThreadedGraphicBufferListener threadedGraphicBufferListener){
		this.listeners.remove(threadedGraphicBufferListener);
	}

	
	/** Finds whether the content has been flagged for up update redraw
	 * Reports false when/if zooming/panning
	 * 
	 * Private, because only needs to be used internally
	 * 
	 * @return  Whether the content should be redrawn
	 */
	private boolean needToUpdate(){
		if (updateDuringZoomPan &&
				zoomPan!=null
				&& (lastMouseX!=applet.mouseX || lastMouseY!=applet.mouseY)
				&& (zoomPan.isPanning() || zoomPan.isZooming())){
			lastMouseX=applet.mouseX;
			lastMouseY=applet.mouseY;		
			return true;
		}
		else if (!updateDuringZoomPan &&
				zoomPan!=null
				&& (zoomPan.isPanning() || zoomPan.isZooming())){
			return false;
		}
		else
			return needToUpdate;
	}
	
	/**Gets the buffer contents as an image
	 * 
	 * @return
	 */
	public PImage getImage(){
		//if you just return bufferImage - get unexpected effects with tint
		return image;
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
	 * 
	 */
	public void startClipping(){
		if (applet.g instanceof PGraphicsJava2D)
			((PGraphicsJava2D)applet.g).g2.setClip(screenBounds);
		else
			System.err.println("Cannot clip with this renderer.");
	}

	/** Stop clipping drawn content
	 * 
	 */
	public void stopClipping(){
		if (applet.g instanceof PGraphicsJava2D)
			((PGraphicsJava2D)applet.g).g2.setClip(null);
	}
	
	/** Runnable task that draws the content in a different thread
	 */
	private class DrawInSeparateThread implements Runnable{
		
		ZoomPanState zoomPanState;
		Object extraInfo;
		boolean useFade;
		
		public DrawInSeparateThread(ZoomPanState zoomPanState,Object extraInfo, boolean useFade){
			this.zoomPanState=zoomPanState;
			this.extraInfo=extraInfo;
			this.useFade=useFade;
		}
		
		private PGraphics getImageCanvas(){

			PGraphics localBufferImage=null;
			synchronized (tempImages) {
				Iterator<PGraphics> it = tempImages.iterator();
				while (it.hasNext() && localBufferImage==null){
					PGraphics image=it.next();
					if (image.width!=screenBounds.width || image.height!=screenBounds.height){
						tempImages.remove(image);
					}
					else if (!tempImagesInUse.contains(image)){
						localBufferImage=image;
					}
				}
				if (localBufferImage==null){
					localBufferImage=applet.createGraphics(screenBounds.width,screenBounds.height,PApplet.JAVA2D);
					tempImages.add(localBufferImage);
				}
				tempImagesInUse.add(localBufferImage);
			}
			return localBufferImage;
		}

		public void run(){
			PGraphics localBufferImage=getImageCanvas();
			
			//Sets flag indicating threaded drawing is in progress in ThreadedGraphicBuffer
			isDrawingInThread=true;
			
			//keep a record of the zoom scale at last draw - only update in ThreadedGraphicBuffer
			// when/if drawing on the on-screen buffer is complete
			Rectangle2D localBoundsAtLastBuffer=null;
			if (zoomPanState!=null){
				PVector p1 = zoomPanState.getDispToCoord(new PVector((float)screenBounds.getMinX(),(float)screenBounds.getMinY()));
				PVector p2 = zoomPanState.getDispToCoord(new PVector((float)screenBounds.getMaxX(),(float)screenBounds.getMaxY()));
				localBoundsAtLastBuffer=new Rectangle2D.Float(p1.x,p1.y,p2.x-p1.x,p2.y-p1.y);
			}
			
			localBufferImage.beginDraw();
			if (useFade)
				localBufferImage.background(255);//white background if we're using fade effect
			else
				localBufferImage.background(255,0);//transparent background
			//use smooth() if the original sketch does
			if (applet.g.smooth)
				localBufferImage.smooth();

			
			localBufferImage.pushMatrix();
			//offset
			localBufferImage.translate(-screenBounds.x,-screenBounds.y);

			//call the threaded buffer code
			threadedDrawToGraphicBuffer.threadedDraw(localBufferImage,zoomPanState,extraInfo);
			
			localBufferImage.popMatrix();
			localBufferImage.endDraw();
			
			//if thread has been interrupted (i.e. cancelled because the image being drawn is
			//obsolete, exit ASAP before updating the image  
			if (Thread.currentThread().isInterrupted()){
				synchronized (tempImagesInUse) {
					tempImagesInUse.remove(localBufferImage);
				}
				return;
			}

			//drawing is now complete, so set the boundsAtLastBuffer in ThreadedGraphicBuffer

			//copy contents to the "image" field;
//			PImage im = applet.createImage(screenBounds.width, screenBounds.height,PConstants.ARGB);
//			im.copy(localBufferImage, 0,0,screenBounds.width, screenBounds.height, 0,0,screenBounds.width, screenBounds.height);
			if (boundsAtLastBuffer!=null)
				boundsAtLastLastBuffer=boundsAtLastBuffer;
			else
				boundsAtLastLastBuffer=localBoundsAtLastBuffer;
			boundsAtLastBuffer=localBoundsAtLastBuffer;
			tintValue=0;
			synchronized (tempImagesInUse) {
				tempImagesInUse.remove(oldImage);
			}
			oldImage=image;
			image=localBufferImage;

			//...and set the flag to indicate that drawing is complete
			isDrawingInThread=false;
			//inform listeners that imagine is ready
			for (ThreadedGraphicBufferListener threadedGraphicBufferListener:listeners)
				threadedGraphicBufferListener.newBufferedImageAvailable();
					}
	}
}
