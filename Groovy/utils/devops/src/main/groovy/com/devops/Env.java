package com.devops;

public class Env {
	static {
		APP_HOME = System.getProperty("APP_HOME");
	}
	
	public static String APP_HOME;
}
