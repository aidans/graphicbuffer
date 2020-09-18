package org.gicentre.tests;

import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gicentre.utils.gui.BusyIcon;
import org.gicentre.utils.gui.ThreadedDraw;
import org.gicentre.utils.gui.ThreadedGraphicBuffer;
import org.gicentre.utils.gui.Tooltip;
import org.gicentre.utils.move.ZoomPan;
import org.gicentre.utils.move.ZoomPanState;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.core.PVector;

/**Demonstrates a use of ThreadedGraphicBuffer with ZoomPan (note use of ZoomPan is not required). 
 * 
 * 30,000 shapes are drawn in the draw loop.
 *
 * Like GraphicBufferTest, but draws in separate threat so that the sketch does not freeze.
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


@SuppressWarnings("serial")
public class ThreadedGraphicBufferTest extends PApplet implements ThreadedDraw{

	ZoomPan zoomPan;
	ThreadedGraphicBuffer graphicBuffer;
	Set<EllipseShape> ellipseShapes; //store our shapes to draw
	Rectangle graphicBufferBounds; //screen bounds of the GraphicBuffer
	Tooltip tooltip;
	PFont font;
	BusyIcon busyIcon;
	
	public void setup(){
		size(800,500);
		smooth();
		
		//Set up zoompan
		zoomPan=new ZoomPan(this);
		zoomPan.setZoomMouseButton(RIGHT);
		zoomPan.setMinZoomScale(0.5f);
		
		//Create GraphicBuffer
		graphicBufferBounds=new Rectangle(50,50,width-100,height-100);
		//we've chosen to make this sketch implement ThreadedDraw, hence "this" as the 3rd parameter
		graphicBuffer=new ThreadedGraphicBuffer(this,zoomPan,this,graphicBufferBounds);
		graphicBuffer.setUpdateDuringZoomPan(true);
		
		//Create 30,000 randomly positioned, sized and coloured ellipses and add to a set
		ellipseShapes=Collections.synchronizedSet(new HashSet<ThreadedGraphicBufferTest.EllipseShape>());
		for (int i=0;i<300000;i++){
			float x=random((float)graphicBufferBounds.getMinX(),(float)graphicBufferBounds.getMaxX());
			float y=random((float)graphicBufferBounds.getMinY(),(float)graphicBufferBounds.getMaxY());
			float w=random(2,10);
			float h=random(2,10);
			int fillColour=color(random(0,255),random(0,255),random(0,255));
			int strokeColour=color(random(0,255),random(0,255),random(0,255));
			EllipseShape ellipseShape=new EllipseShape(x, y, w, h, fillColour, strokeColour);
			ellipseShapes.add(ellipseShape);
		}
		
		font=createFont("Helvetica",12);
		//Create a tooltip
		tooltip=new Tooltip(this, font, 12, 100);

		//Create an animated busy icon
		busyIcon=new BusyIcon();
		
	}
	
	
	public void draw(){
		//white background
		background(255);

		//Firstly find all the ellipses that the mouse cursor is in
		//(Even through we iterate through all - this is very fast compared to drawing them!)
		List<EllipseShape> mouseOveredShapes=new ArrayList<ThreadedGraphicBufferTest.EllipseShape>();
		PVector transformedMouseCoord=zoomPan.getDispToCoord(new PVector(mouseX,mouseY));
		for (EllipseShape ellipseShape:ellipseShapes)
			if (ellipseShape.ellipse2d.contains(transformedMouseCoord.x,transformedMouseCoord.y))
				mouseOveredShapes.add(ellipseShape);
		
		
		//use the correct ellipse drawing mode
		ellipseMode(CORNER);
				
		//set the tooltip text to report the number of shapes the mouse cursor is over
		if (!mouseOveredShapes.isEmpty())
			tooltip.setText(mouseOveredShapes.size()+" shapes");

		//draw the buffer - with invoke the threadDraw code if flagged for updating
		graphicBuffer.draw();
		
		//Start clipping drawn content to the bounds of the GraphicBuffer
		graphicBuffer.startClipping();
		//Transform using zoompan once again
		pushMatrix();
		zoomPan.transform();
		//Draw outline of all the ellipses that the mouse is over
		strokeWeight((float)(2/zoomPan.getZoomScale()));
		for (EllipseShape ellipseShape:mouseOveredShapes){
			noFill();
			stroke(0,150);
			ellipse((float)ellipseShape.ellipse2d.getX(), (float)ellipseShape.ellipse2d.getY(), (float)ellipseShape.ellipse2d.getWidth(), (float)ellipseShape.ellipse2d.getHeight());			
		}
		popMatrix();
		//stop clipping
		graphicBuffer.stopClipping();
		
		//draw tooltip if the mouse is over ellipses
		if (!mouseOveredShapes.isEmpty())
			tooltip.draw(mouseX, mouseY);
		
		textFont(font);
		textAlign(LEFT,BOTTOM);
		fill(80);
		
		//draw busy icon if threaded drawing is in progress
		if (graphicBuffer.isDrawingInThread()){
			busyIcon.draw(this,0,height-30,30);
			textAlign(LEFT,BOTTOM);
			textSize(20);
			fill(80);
			text("Drawing content...",30,height);
		}
	}
	
	public void mouseMoved(){
		//Disable mouse-controlled zooming/panning if mouse is not in the area
		if (graphicBufferBounds.contains(mouseX,mouseY))
			zoomPan.setMouseMask(0);
		else
			zoomPan.setMouseMask(-1);
	}
	
	public void keyPressed(){
		//reset zoom
		if (key=='r')
			this.zoomPan.reset();
		if (key=='f')
			graphicBuffer.setUseFadeEffect(!graphicBuffer.getUseFadeEffect());
	}
	
	//class to store the various characteristics of the ellipses
	private class EllipseShape{
		Ellipse2D ellipse2d;
		int fillColour,strokeColour;
		
		public EllipseShape(float x, float y, float w, float h, int fillColour, int strokeColour){
			ellipse2d=new Ellipse2D.Float(x,y,w,h);
			this.fillColour=fillColour;
			this.strokeColour=strokeColour;
		}
	}

	//Code to draw onto the buffer
	public void threadedDraw(PGraphics canvas,ZoomPanState zoomPanState, Object extraInfo) {
		//Note that all drawing needs to be to the canvas, so prepend
		//all Processing draw functions with "canvas."
		
		//apply zoom TO CANVAS
		zoomPanState.transform(canvas);
		
		//scale the strokeWeight
		canvas.strokeWeight((float)(1/zoomPanState.getZoomScale()));
		canvas.noFill();
		
		canvas.ellipseMode(CORNER);

		//find the viewport
		//Use this below to only draw shapes within view. Vastly
		//increased drawing time for zoomed-in views
		Rectangle2D viewPort=graphicBuffer.getViewPort();
		//iterate through all shapes and draw
		for (EllipseShape ellipseShape:ellipseShapes){
			if (viewPort.intersects(ellipseShape.ellipse2d.getBounds2D())){
				canvas.fill(ellipseShape.fillColour,100);
				canvas.stroke(ellipseShape.strokeColour,100);
				canvas.ellipse((float)ellipseShape.ellipse2d.getX(), (float)ellipseShape.ellipse2d.getY(), (float)ellipseShape.ellipse2d.getWidth(), (float)ellipseShape.ellipse2d.getHeight());
			}
			if (Thread.currentThread().isInterrupted())
				return;
		}
	}
}
