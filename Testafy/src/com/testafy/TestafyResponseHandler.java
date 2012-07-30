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
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.impl.client.BasicResponseHandler;

public class TestafyResponseHandler 
	extends BasicResponseHandler 
	implements ResponseHandler<String> {
	
	public TestafyResponseHandler() {
		super();
	}
	
	public String handleResponse(HttpResponse response) throws IOException {
		if(response.getStatusLine().getStatusCode() < 300) {
			return super.handleResponse(response);
		} else {
			if(response.getStatusLine().getStatusCode() == 400) {
				InputStream is = response.getEntity().getContent();
				
				StringBuilder builder = new StringBuilder();
				
				int next = is.read();
				while(next > -1) {
					builder.append((byte)next);
					next = is.read();
				}
				
				String reason = builder.toString();
				throw new APIException(reason);
			}
			return super.handleResponse(response);
		}
	}
}
