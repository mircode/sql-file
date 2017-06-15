package com.core.format;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

/**
 * 类加载
 * @author 魏国兴
 *
 */
public class ClazzFormater extends Formater{

	Object target=null;
	public ClazzFormater(String fields,String clazz,String path) throws Exception{
		super(fields, clazz);
		URLClassLoader loader=new URLClassLoader(new URL[]{Paths.get(path).toUri().toURL()});
		target=loader.loadClass(clazz).newInstance();
		loader.close();
	}
	public String format(String row, String field) throws Exception{
		Method[] methods=target.getClass().getDeclaredMethods();
		for(Method m:methods){
			if(m.getName().equals("format")){
				int args=m.getParameterCount();
				Object res=null;
				if(args==2){
					res=m.invoke(target,new Object[]{row,field});
				}
				if(args==3){
					res=m.invoke(target,new Object[]{row,field,fields});
				}
				if(args==4){
					res=m.invoke(target,new Object[]{row,field,fields,express});
				}
				return res==null?null:res.toString();
			}
		}
		return null;
	}
}
