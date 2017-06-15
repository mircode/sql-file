package com.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.core.format.Formater;
import com.jayway.jsonpath.JsonPath;

/**
 * SQL º¯Êý
 * @author Îº¹úÐË
 *
 */
@SuppressWarnings("rawtypes")
public class SQLFunc {
	
	static Map<String,Executable> funclib=new HashMap<>();
	
	static{
		Executable<String,Map,String> json_path=SQLFunc::json_path;
		Executable<String,Map,String> replace=SQLFunc::replace;
		Executable<String,Map,String> math=SQLFunc::math;
		
		funclib.put("json_path",json_path);
		funclib.put("replace",replace);
		funclib.put("math",math);
		
	}
	@SuppressWarnings("unchecked")
	public static String execute(String exp,String row,Formater format) throws Exception{
		String func=Parser.Func(exp)[0];
		String param=Parser.Func(exp)[1];
		if(func==null) return null;
		Map args=args(param,row,format);
		String res=(String) funclib.get(func).execute(param,args);
		return res;
	}
	
	private static Map<String,String> args(String exp,String row,Formater table) throws Exception{
		Map<String,String> res=new LinkedHashMap<>();
		for(String f: table.fields()){
			if(exp.contains(f)){
				res.put(f,table.format(row,f));	
			}
		}
		return res;
	}
	public static String math(String param,Map<String,String> map){
		
		String[] express=param.replaceAll("\\s*([+-*/])\\s*"," $1 ").split(" ");
		List<String> tokens=Arrays.asList(express);
		Collections.reverse(tokens);
		Stack<String> stack = new Stack<String>();
		stack.addAll(tokens);
		
		while(!stack.isEmpty()){
			Double val1=Double.parseDouble(stack.pop());
			String opt=stack.pop();
			Double val2=Double.parseDouble(stack.pop());
			
			Double res=null;
			if(opt.equals("+")){
				res=val1+val2;
			}else if(opt.equals("-")){
				res=val1-val2;
			}else if(opt.equals("*")){
				res=val1*val2;
			}else if(opt.equals("/")){
				res=val1/val2;
			}
			stack.push(res.toString());
			
			if(stack.size()==1){
				break;
			}
		}
		return stack.pop();
	}
	public static String replace(String param,Map<String,String> map){
		String[] args=param.split(":");
		String field=args[0];
		String oldstr=args[1];
		String newstr=args[3];
		return map.get(field).replace(oldstr,newstr);
	}
	public static String json_path(String param,Map<String,String> map){
		String[] args=param.split(":");
		
		String json=map.get(args[0]);
		String path=args[1].replaceAll("^['\"]|['\"]$","");
		
		Object res=JsonPath.read(json,path);
		return res.toString();
	}
	public static boolean compare(String wherecase){
		String val1=null;
		String opt=null;
		String val2=null;
		Pattern p=Pattern.compile("(.*?)("+Parser.OPERTIONS+")(.*)");
		Matcher m=p.matcher(wherecase);
		while(m.find()){
			val1=m.group(1);
			opt=m.group(2);
			val2=m.group(3);
		}
		
		boolean res=false;
		if (val1.matches("\\d+\\.?\\d*")&&val2.matches("\\d+\\.?\\d*")){
			Double v1 = Double.parseDouble(val1);
			Double v2 = Double.parseDouble(val2);
			if(opt.equals(">=")){
				res=v1.compareTo(v2)>=0;
			}else if (opt.equals("<=")){
				res=v1.compareTo(v2)<=0;
			}else if (opt.equals("=")){
				res=v1.equals(v2);
			}else if (opt.equals("!=")){
				res=!v1.equals(v2);
			}else if(opt.equals("<")){
				res=v1.compareTo(v2)<0;
			}else if(opt.equals(">")) {
				res=v1.compareTo(v2)>0;
			}else if(opt.equals("like")){
				res=val1.matches(val2.replace("%",".*"));
			}else if(opt.equals("regex")){
				res=val1.matches(val2);
			}
		}else{
			String v1=val1.replaceAll("^['\"]|['\"]$","");
			String v2=val2.replaceAll("^['\"]|['\"]$","");
			
			if(opt.equals(">=")){
				res=v1.compareTo(v2)>=0;
			}else if (opt.equals("<=")){
				res=v1.compareTo(v2)<=0;
			}else if (opt.equals("=")){
				res=v1.equals(v2);
			}else if (opt.equals("!=")){
				res=!v1.equals(v2);
			}else if(opt.equals("<")){
				res=v1.compareTo(v2)<0;
			}else if(opt.equals(">")) {
				res=v1.compareTo(v2)>0;
			}else if(opt.equals("like")){
				res=v1.matches(v2.replace("%",".*"));
			}else if(opt.equals("regex")){
				res=v1.matches(v2);
			}
		}
		return res;
	}
	
}
@FunctionalInterface
interface Executable<E,M,R> {
	R execute(E express,M args);
}
