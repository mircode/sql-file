package com.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.core.Config;

public class Log {
	static Logger log=null;
	public static void log(String format,Object arg){
		if(Config.getLogDir()!=null){
			System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY,"DEBUG");
			System.setProperty(org.slf4j.impl.SimpleLogger.SHOW_DATE_TIME_KEY,"true");
			System.setProperty(org.slf4j.impl.SimpleLogger.DATE_TIME_FORMAT_KEY,"yyy-MM-dd HH:mm:ss");
			System.setProperty(org.slf4j.impl.SimpleLogger.LOG_FILE_KEY,Paths.get(Config.getLogDir(),"sql.out").toString());
			if(log==null){
				log=LoggerFactory.getLogger(Log.class);
			}
			if(arg instanceof Exception){
				log.info(format+trace((Exception)arg));
			}else{
				log.info(format,arg);
			}
		}
	    
	}
	public static String trace(Exception e) {
        StringWriter stwriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stwriter);
        e.printStackTrace(writer);
        StringBuffer buffer=stwriter.getBuffer();
        return buffer.toString();
    }
	public static void main(String args[]){
		Log.log("test","tes");
		Log.log("test","tes");
		Log.log("test","tes");
		try{
			int z=0;
			System.out.println(1/z);
		}catch(Exception e){
			Log.log("eror",e);
		}
	}
}
