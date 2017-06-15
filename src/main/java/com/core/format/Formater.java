package com.core.format;

/**
 * 
 * 抽象格式类
 * @author 魏国兴
 *
 */
public abstract class Formater {
	public String fields;
	protected String express;
	public Formater(String fields,String express){
		this.fields=fields;
		this.express=express;
	}
	public int index(String field){
		String[] fields=this.fields();
		for(int i=0,len=fields.length;i<len;i++){
			if(fields[i].equals(field)) return i;
		}
		return -1;
	}
	public String flatRow(String row){
		String fields="";
		try{
			for(String field:fields()){
				fields+=this.format(row, field)+"|";
			}
			return fields.replaceAll("[|]$","");
		}catch(Exception e){
			return row;
		}
	}
	public String[] fields(){
		return this.fields.split(",");
	}
	abstract public String format(String row,String field) throws Exception;
}
