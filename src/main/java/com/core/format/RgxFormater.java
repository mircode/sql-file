package com.core.format;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * 正则格式化
 * @author 魏国兴
 *
 */
public class RgxFormater extends Formater{
	public RgxFormater(String fields, String express) {
		super(fields, express);
	}
	@Override
	public String format(String row,String field) {
		int index=this.index(field);
		String express=this.express;
		Pattern p=Pattern.compile(express);
		Matcher m=p.matcher(row);
		if(m.find()){
			return m.group(index+1).replaceAll("^\\s+|\\s+$","");
		}
		return null;
	}

}
