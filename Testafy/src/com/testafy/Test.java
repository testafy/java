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


import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.auth.*;

/**
 * This is a representation of a Testafy test run.
 * It contains all the methods necessary to use the Testafy API.
 * 
 * @author David Orr
 * 
 * For reference, this line contains no more and no less than eighty characters
 * 
 */

public class Test {
	int authScopePort;
	long testID;
	String loginName, password, pbehave, baseURI, authScopeLoc,
		stepScreenshots;
	DefaultHttpClient httpclient;
	Gson gson;
	
	public static Test tryItNow(String pbehave) {
		Test t = new Test("try_it_now", "");
		t.setPbehave(pbehave);
		
		return t;
	}

	/**
	 *  Create a new Test with default PBehave 
	 *  
	 *  @param	loginName	the name of a Testafy account
	 *  @param	password	the password for the account named by loginName
	 */
	public Test(String loginName, String password) {
		this(loginName, password, "then pass this test");
	}
	/* Create a new Test. */
	public Test(String loginName, String password, String pbehave) {
		this.loginName = loginName;
		this.password = password;
		this.pbehave = pbehave;
		
		testID = 0;
		baseURI = "https://app.testafy.com/api/v0/";
		authScopeLoc = "app.testafy.com";
		authScopePort = 443;
		httpclient = new DefaultHttpClient();
		gson = new Gson();
		setStepScreenshots(false);
	}
	
	/* 
	 * Internal method to JSONify the parameters.
	 */
	private String assembleParams(Map<String, String> params) {
		//JSON-encode the parameters to pass to the server
		Type mapStrType = new TypeToken<Map<String, String>>(){}
			.getType();
		String jsonParams = gson.toJson(params, mapStrType);
		return jsonParams;
	}
	
	/*
	 * Internal method to create a request object
	 * to be passed to the HttpClient.
	 */
	private HttpPost generateRequest(String command, String json)
			throws APIException {
		// Set up basic auth
		httpclient.getCredentialsProvider().setCredentials(
				new AuthScope(authScopeLoc, authScopePort), 
				new UsernamePasswordCredentials(loginName, password));

		//compose the URI from the base and the given command
		String fullURI = baseURI + command;

		//set up the POST request
		HttpPost req = new HttpPost(fullURI);

		//add parameters to the request
		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
		nvps.add(new BasicNameValuePair("json", json));
		UrlEncodedFormEntity paramEntity = 
				new UrlEncodedFormEntity(nvps, Consts.UTF_8);

		req.setEntity(paramEntity);
		
		return req;
	}
	
	/*
	 * An internal method to parse the HttpResponse object
	 * received from the server.
	 */
	private Map<String, Object> getResponse(HttpPost request) 
			throws IOException {

		//execute the request
		String response = httpclient.execute(request, 
				new TestafyResponseHandler());

		Type mapStrType = new TypeToken<Map<String, Object>>(){}.getType();
		//parse the response as JSON
		Map<String, Object> parsed = gson.fromJson(response, mapStrType);

		// make sure the response is OK
		if(parsed.size() == 0 || 
				parsed.get("error") != null) {
			throw new APIException("Problem with request:\n" 
				+ parsed.get("error"));
		}
		return parsed;
	}
	
	/*
	 * An internal method to make an API call.
	 * 
	 * Params:
	 * 	String command: the path to the command on the server
	 * 	vars: the variables to be passed for this API call
	 * 
	 */
	private Map<String, Object> makeAPICall(String command, 
			Map<String, String> params) 
					throws IOException {
		String json = assembleParams(params);
		
		HttpPost req = generateRequest(command, json);
		
		return getResponse(req);
	}
	
	/* 
	 * Run a test on the server, returning the testID of the 
	 * resulting test run. Returns as soon as the run has been
	 * received by the server. 
	 * 
	 * Try runAndWait() if you need the results before doing anything else.
	 * 
	 * @return	testID 	the ID of the test run on the server.
	 * @throws	IOException	when the call to the server was not successful
	 * 				usually involves bad or missing parameters in the request.
	 * 
	 */
	public long run() throws IOException {
		String path = "test/run";
		if(loginName.equals("try_it_now")) path = "try_it_now/run";
		
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("pbehave", pbehave);
		vars.put("screenshots", stepScreenshots);
		
		Map<String, Object> res = makeAPICall(path, vars);
		
		if(!res.containsKey("test_run_test_id"))
			throw new IOException((String)res.get("error"));
		testID = Integer.parseInt((String)res.get("test_run_test_id"));
		return testID;
	}
	
	/*
	 * Run a test on the server and wait until it finishes to return.
	 * 
	 * @return	testID	the ID of the test run on the server.
	 * @throws	IOException	when the call to the server was not successful
	 * 				usually involves bad or missing parameters in the request.
	 */
	public long runAndWait() throws IOException {
		long testID = run();
		while(!isDone()) {
			try {
				Thread.sleep(5000);
			}
			catch(InterruptedException e) {} //ignore
		}
		return testID;
	}
	
	/*
	 * Check if the phrases in the PBehave code associated with this
	 * test are valid PBehave.
	 * 
	 * @return	message	a string describing the validity of the PBehave code
	 * 		and detailing any errors found.
	 * @throws	IOException	when the call to the server was not successful
	 * 				usually involves bad or missing parameters in the request.
	 */
	public String phraseCheck() throws IOException {
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("pbehave", pbehave);
		
		Map<String, Object> response = 
				makeAPICall("phrase_check", vars);
		return (String)response.get("message");
	}
	
	/* 
	 * Get the status of the last run of this test.
	 * 
	 * @return	status	A string describing the status of this test run.
	 * 
	 */
	public String status() throws IOException {
		if(testID == 0) return "unscheduled";
		
		String path = "test/status";
		if(loginName.equals("try_it_now")) path = "try_it_now/status";
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("trt_id", String.valueOf(testID));
		Map<String, Object> response = 
				makeAPICall(path, vars);
		return (String)response.get("status");
	}
	
	/* 
	 * Get the number of "then" statements in the PBehave code
	 * that have passed (so far).
	 * 
	 * @return	passed	the number of "then" statements in the test run 
	 * 	that have passed.
	 */
	public int passed() throws IOException {
		if(testID == 0) return 0;
			
		String path = "test/stats/passed";
		if(loginName.equals("try_it_now")) path = "try_it_now/stats/passed";
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("trt_id", String.valueOf(testID));
		Map<String, Object> response = 
				makeAPICall(path, vars);
		return Integer.parseInt((String)response.get("passed"));
	}
	
	/* 
	 * Get the number of "then" statements in the PBehave code
	 * that have failed (so far).
	 * 
	 * @return	failed	the number of "then" statements in the test run 
	 * 	that have failed.
	 */
	public int failed() throws IOException {
		if(testID == 0) return 0;
		
		String path = "test/stats/failed";
		if(loginName.equals("try_it_now")) path = "try_it_now/stats/failed";
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("trt_id", String.valueOf(testID));
		Map<String, Object> response = 
				makeAPICall(path, vars);
		return Integer.parseInt((String)response.get("failed"));
	}
	
	/* 
	 * Get the total number of "then" statements in the PBehave code that will
	 * be run. For a completed test, passed() + failed() == planned().
	 * 
	 * @return	passed	the number of "then" statements in the test run 
	 * 	that have passed.
	 */
	public int planned() throws IOException {
		if(testID == 0) return 0;
		
		String path = "test/stats/planned";
		if(loginName.equals("try_it_now")) 
			path = "try_it_now/stats/planned";	
		
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("trt_id", String.valueOf(testID));
		Map<String, Object> response = 
				makeAPICall(path, vars);
		return Integer.parseInt((String)response.get("planned"));
	}
	
	/* 
	 * Get a list of the screenshots from this test run.
	 * 
	 * @return	screenshots	a List<String> containing the names of the
	 * 	screenshots taken in this test run.
	 */
	public List<String> screenshots() throws IOException {
		if(testID == 0) return null;
	
		String path = "test/screenshots";
		if(loginName.equals("try_it_now")) path = "try_it_now/screenshots";
		
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("trt_id", String.valueOf(testID));
		Map<String, Object> response = 
				makeAPICall(path, vars);
		
		return (List<String>) response.get("screenshots");
	}
	
	/*
	 * Get a specified base64-encoded screenshot from this test run,
	 * as a string.
	 * 
	 * @param	screenshotName	the name of the screenshot to get from the
	 * 	server. Should be a member of the list returned by screenshots().
	 * 
	 * @return	screenshot	a String containing the base64 encoding of the 
	 * 	screenshot. 
	 */
	public String screenshotAsBase64(String screenshotName) 
			throws IOException {
		if(testID == 0) return null;
	
		String path = "test/screenshot";
		if(loginName.equals("try_it_now")) path = "try_it_now/screenshot";
		
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("trt_id", String.valueOf(testID));
		vars.put("filename", screenshotName);
		
		Map<String, Object> response = 
				makeAPICall(path, vars);
		return (String)response.get("screenshot");
	}
	
	/* 
	 * Get all base64-encoded screenshots from this test run
	 * as an array of strings.
	 * 
	 * @return	screenshots	a map from screenshot names (as in screenshots())
	 * 	to the base64 encoding of the screenshots.
	 */
	public Map<String, String> allScreenshotsAsBase64() 
			throws IOException {
		if(testID == 0) return null;
		
		Map<String, String> screenshots = 
				new HashMap<String, String>();
		
		for(String screenshotName : screenshots()) {
			screenshots.put(screenshotName, 
					screenshotAsBase64(screenshotName));
		}
		
		return screenshots;
	}
	
	/*
	 * Save a specified screenshot to disk.
	 * 
	 * @param	screenshotName	the name of the screenshot to get
	 * @param	localName	the name as which to save the screenshot on disk
	 */
	public void saveScreenshot(String screenshotName, 
			String localName) 
					throws IOException {
		if(testID == 0) return;
		
		String screenshot = screenshotAsBase64(screenshotName);

		FileWriter fwriter = new FileWriter(localName);
		BufferedWriter f = new BufferedWriter(fwriter);
		f.write(screenshot);
		f.close();
	}
	
	/* 
	 * Save all screenshots from this test run to disk, in the 
	 * specified directory.
	 * 
	 * @param	localDir	the directory in which to save the screenshots.
	 */
	public void saveAllScreenshots(String localDir) 
			throws IOException {
		if(testID == 0) return;
		
		for(String screenshotName : screenshots()) {
			new File(localDir).mkdir();
			File f = new File(localDir + File.separator + screenshotName);
			f.createNewFile();
			FileWriter fwriter = new FileWriter(localDir 
					+ File.separator
					+ screenshotName);
			
			BufferedWriter writer = new BufferedWriter(fwriter);
			writer.write(screenshotAsBase64(screenshotName));
			writer.close();
		}
	}
	
	/* 
	 * Get whether or not the test is done runnning.
	 * Just checks whether the status is "unscheduled," "queued," or "running"
	 * 
	 * @return	done	whether or not the test is done running
	 */
	public boolean isDone() throws IOException {
		String status = status();
		boolean done = true;
		if(status.equals("unscheduled") || 
				status.equals("queued") || 
				status.equals("running")) {
			done = false;
		}
		return done;
	}
	
	/* 
	 * Get results of this test run as an arraylist of 2-element arraylists,
	 * in which the second element is a line in TAP format and the
	 * first element describes the type of line of the second element.
	 * 
	 * @returns	results	a 2d arraylist of results; see method description for format
	 */
	public ArrayList<ArrayList<String>> results() 
			throws IOException {
		if(testID == 0) return null;
		
		String path = "test/results";
		if(loginName.equals("try_it_now")) path = "try_it_now/results";
		
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("trt_id", String.valueOf(testID));
		Map<String, Object> response = 
				makeAPICall(path, vars);
		
		return (ArrayList<ArrayList<String>>)response.get("results");
	}
	
	/*
	 * Get results of this test run as a string in TAP format.
	 * 
	 * @returns	resultsString	a single string with the results in
	 * 		TAP (Test Anything Protocol) format.
	 */
	public String resultsString() throws IOException {
		if(testID == 0) return null;
		
		ArrayList<ArrayList<String>> results = results();
		String resultsString = "";
		for(int i=0; i< results.size(); i++) {
			resultsString += results.get(i).get(1) + "\n";
		}
		
		return resultsString;
	}
	
	/* 
	 * Check if the server is responding correctly.
	 * Good for verifying that everything is set up properly.
	 * 
	 * @returns	success	whether we got a successful "pong" back from the server
	 */
	public boolean ping() throws IOException {
		Map<String, String> vars = new HashMap<String, String>();
		Map<String, Object> response = makeAPICall("ping", vars);
		
		return ((String)response.get("message")).equals("pong");
	}
	
	
	/* Getters and setters. */
	
	public boolean getStepScreenshots() {
		if(stepScreenshots.equals(""))
			return false;
		return true;
	}
	public void setStepScreenshots(boolean val) {
		if(val) stepScreenshots = "true";
		else stepScreenshots = "";
	}
	public long getTestID() {
		return testID;
	}
	public void setTestID(long newTestID) {
		testID = newTestID;
	}
	
	public String getLoginName() {
		return loginName;
	}
	public void setLoginName(String newLoginName) {
		loginName = newLoginName;
	}
	
	public String getPassword() {
		return password;
	}
	public void setPassword(String newPassword) {
		password = newPassword;
	}
	
	public String getBaseURI() {
		return baseURI;
	}
	public void setBaseURI(String newURI) {
		baseURI = newURI;
		if(!baseURI.endsWith("/"))
			baseURI += "/";
	}
	
	public String getAuthScopeLoc() {
		return authScopeLoc;
	}
	public void setAuthScopeLoc(String newAuthScopeLoc) {
		authScopeLoc = newAuthScopeLoc;
	}
	public int getAuthScopePort() {
		return authScopePort;
	}
	public void setAuthScopePort(int newPort) {
		authScopePort = newPort;
	}
	
	public String getPbehave() {
		return pbehave;
	}
	public void setPbehave(String newPbehave) {
		pbehave = newPbehave;
	}
}
