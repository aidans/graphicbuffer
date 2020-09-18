package org.gicentre.tests;

import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gicentre.utils.gui.GraphicBuffer;
import org.gicentre.utils.gui.Tooltip;
import org.gicentre.utils.move.ZoomPan;
import org.gicentre.utils.move.ZoomPanState;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PVector;

/**Demonstrates a use of GraphicBuffer with ZoomPan and compares with not using buffer
 * 
 * 10,000 shapes are drawn in the draw loop.
 * 
 * Use of GraphicBuffer makes zooming/panning and mouseover interactions quick as the
 * shapes are only drawn when they need to be. The pause after zooming or panning is the time
 * taken to draw the content.
 * 
 * Pressing 'b' toggle between using the buffer and not.
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
public class GraphicBufferTest extends PApplet{

	ZoomPan zoomPan;
	GraphicBuffer graphicBuffer;
	Set<EllipseShape> ellipseShapes; //store our shapes to draw
	Rectangle graphicBufferBounds; //screen bounds of the GraphicBuffer
	Tooltip tooltip;
	boolean useBuffer=true;
	PFont font;
	
	public void setup(){
		size(800,500);
		smooth();
		
		//Set up zoompan
		zoomPan=new ZoomPan(this);
		zoomPan.setZoomMouseButton(RIGHT);
		zoomPan.setMinZoomScale(0.5f);
	//		zoomPan.allowMousePanX(false);
	//		zoomPan.allowMouseZoomX(false);
		
		//Create GraphicBuffer
		graphicBufferBounds=new Rectangle(50,50,width-100,height-100);
		graphicBuffer=new GraphicBuffer(this,zoomPan,graphicBufferBounds);
		
		//Create 10,000 randomly positioned, sized and coloured ellipses and add to a set
		ellipseShapes=new HashSet<GraphicBufferTest.EllipseShape>();
		for (int i=0;i<100000;i++){
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
		
	}
	
	public void draw(){
		//white background
		background(255);
		ellipseMode(CORNER);
		
		ZoomPanState zoomPanState=zoomPan.getZoomPanState();

		//Firstly find all the ellipses that the mouse cursor is in
		//(Even through we iterate through all - this is very fast compared to drawing them!)
		List<EllipseShape> mouseOveredShapes=new ArrayList<GraphicBufferTest.EllipseShape>();
		PVector transformedMouseCoord=zoomPanState.getDispToCoord(new PVector(mouseX,mouseY));
		for (EllipseShape ellipseShape:ellipseShapes)
			if (ellipseShape.ellipse2d.contains(transformedMouseCoord.x,transformedMouseCoord.y))
				mouseOveredShapes.add(ellipseShape);
		
		
				
		//set the tooltip text to report the number of shapes the mouse cursor is over
		if (!mouseOveredShapes.isEmpty())
			tooltip.setText(mouseOveredShapes.size()+" shapes");

		//Only draw all shapes if the update flag has been set
		if (!useBuffer || graphicBuffer.needToUpdate()){
			//capture drawing output form now on
			if (useBuffer)
				graphicBuffer.startCapture();
			if (!useBuffer)
				graphicBuffer.startClipping();
			//use the correct ellipse drawing mode
			//transform using zoompan
			pushMatrix();
			zoomPanState.transform();
			//scale the strokeWeight
			strokeWeight((float)(1/zoomPanState.getZoomScale()));
			//iterate through all shapes and draw
			for (EllipseShape ellipseShape:ellipseShapes){
				fill(ellipseShape.fillColour,100);
				stroke(ellipseShape.strokeColour,100);
				ellipse((float)ellipseShape.ellipse2d.getX(), (float)ellipseShape.ellipse2d.getY(), (float)ellipseShape.ellipse2d.getWidth(), (float)ellipseShape.ellipse2d.getHeight());
			}
			//reset zoompan transformation
			popMatrix();
			//stop capturing drawn output
			if (useBuffer)			
				graphicBuffer.stopCapture();
			if (!useBuffer)
				graphicBuffer.stopClipping();
		}
		//draw the buffer
		if (useBuffer)
			graphicBuffer.draw();
		
		//Start clipping drawn content to the bounds of the GraphicBuffer
		graphicBuffer.startClipping();
		//Transform using zoompan once again
		pushMatrix();
		zoomPanState.transform();
		
		//Draw outline of all the ellipses that the mouse is over
		//We're not using the buffer here, but if there were many to draw, we might consider using 
		//another buffer with the same zoomPan
		strokeWeight((float)(2/zoomPanState.getZoomScale()));
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
		if (useBuffer)
			text("Using buffer",0,height);
		else
			text("Not using buffer",0,height);
		
	}
	
	public void mouseMoved(){
		//Disable mouse-controlled zooming/panning if mouse is not in the area
		if (graphicBufferBounds.contains(mouseX,mouseY))
			zoomPan.setMouseMask(0);
		else
			zoomPan.setMouseMask(-1);
	}
	
	public void keyPressed(){
		//toggle the use of the buffer when 'b' is pressed
		if (key=='b'){
			this.useBuffer=!useBuffer;
			if (this.useBuffer)
				graphicBuffer.setUpdateFlag();
		}
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
}
