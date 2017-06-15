package com.core.format;

import java.io.FileReader;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
/**
 * 脚本格式化
 * @author 魏国兴
 *
 */
public class ScriptFormater extends Formater{

	private static ScriptEngine engine=null;
	public ScriptFormater(String fields, String express) {
		super(fields, express);
		
	}
	@Override
	public String format(String row, String field) throws Exception{
		ScriptEngine engine=getEngine();
		if(this.express.endsWith(".js")){
			engine.eval(new FileReader(this.express));
		}else{
			engine.eval(this.express);
		}
	    Invocable method=(Invocable)engine;
	    String res=(String) method.invokeFunction("format",row,field,this.fields,this.express);
		return res.replaceAll("^\\s+|\\s+$","");
		
	}
	private ScriptEngine getEngine(){
		if(ScriptFormater.engine==null){
			ScriptEngineManager manager=new ScriptEngineManager();
			ScriptFormater.engine=manager.getEngineByName("javascript");	
		}
		return ScriptFormater.engine;
	}
}
