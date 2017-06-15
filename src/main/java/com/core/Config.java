package com.core;

import java.nio.file.Paths;

/**
 * 系统配置
 * @author 魏国兴
 *
 */
public class Config {
	
	private static String TABLEDIR=null;
	private static String LOGDIR=null;
	private static String SCRIPTDIR=null;
	
	private static String USERDIR=Paths.get(System.getProperty("user.home")).toString();
	private static String WORKSPACE=Paths.get(System.getProperty("user.dir")).toString();
	
	public static void setUserHome(String userHome){
		USERDIR=userHome;
	}
	public static void setTableDir(String tableDir){
		TABLEDIR=tableDir;
	}
	public static void setScriptDir(String scriptDir){
		SCRIPTDIR=scriptDir;
	}
	public static void setLogDir(String logDir){
		LOGDIR=logDir;
	}
	public static void setWorkSpace(String workSpace){
		WORKSPACE=workSpace;
	}
	public static String getUserHome(){
		return USERDIR==null?WORKSPACE:USERDIR;
	}
	public static String getTableDir(){
		return TABLEDIR==null?WORKSPACE:TABLEDIR;
	}
	public static String getLogDir(){
		return LOGDIR==null?WORKSPACE:LOGDIR;
	}
	public static String getScriptDir(){
		return SCRIPTDIR==null?WORKSPACE:SCRIPTDIR;
	}
	public static String getWorkspace(){
		return WORKSPACE;
	}
}
