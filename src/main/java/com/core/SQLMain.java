package com.core;

/**
 * SQL入口
 * @author 魏国兴
 *
 */
public class SQLMain {

	public static void execute(String config,String ws,String cmd) throws Exception{
		if(cmd.matches("^select.*")){
			if(config==null) config=ws;
			new FileSQL(config,ws).run(cmd);
		}else if(cmd.matches("^(create|drop|update|show|desc).*")){
			new TabMgr(config).run(cmd);
		}
	}
}
