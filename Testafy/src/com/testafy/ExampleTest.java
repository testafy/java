package com.testafy;



public class ExampleTest {
	public static void main(String[] args) throws Exception {
		Test t = new Test("user", "pass");
		
		System.out.println("Ping: " + t.ping());
		long trtID = t.runAndWait();
		System.out.println("Ran test #" + trtID);
		System.out.println("Passed " + t.passed() + " of " + t.planned());
		System.out.println("Results:\n " + t.resultsString());
	}
}
