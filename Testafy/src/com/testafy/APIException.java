/* 
 *  Copyright 2012 Grant Street Group, Inc.
 * 
 *  This file is part of Testafy's Java API wrapper.
 *
 *  Testafy's Java API wrapper is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  Testafy's Java API wrapper is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with Testafy's Java API wrapper.  If not, see <http://www.gnu.org/licenses/>.
 *  
 */

package com.testafy;

import java.io.IOException;

@SuppressWarnings("serial")
public class APIException 
	extends IOException {
	String message;
	
	public APIException(String message) {
		this.message = message;
	}
}
