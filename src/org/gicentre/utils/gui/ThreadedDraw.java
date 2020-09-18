package org.gicentre.utils.gui;

import org.gicentre.utils.move.ZoomPanState;

import processing.core.PGraphics;

/** Interfaces for classes to implement, which allows them to specify code to draw
 * to a ThreadedGraphicBuffer
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

public interface ThreadedDraw {

	/** Code to draw onto a ThreadedGraphicBuffer in a different thread
	 * 
	 * @param canvas  The PGraphics canvas to draw to 
	 */
	public void threadedDraw(PGraphics canvas,ZoomPanState zoomPanState,Object drawData);
}
