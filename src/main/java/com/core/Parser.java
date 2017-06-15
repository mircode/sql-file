package com.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析器
 * @author 魏国兴
 *
 */
public class Parser {
	
	// 语法
	public static final String DMLWORDS="select|from|where|group by|order by|limit|into";
	public static final String DDLWORDS="create table|fmt|drop table|update table|show tables|desc";
	public static final String OPERTIONS=">=|<=|!=|=|>|<|like|regex";
	public static final String AGGREG_FUNCS="sum|avg|max|min|count|distinct";
	public static final String FIELD_FUNCS="json_path|math|replace";
	public static final String OTHER="desc|asc|as";
    
	public static Map<String,String> Parse(String sql){
		Map<String,String> map=new LinkedHashMap<>();
		sql=format(sql);
		if(sql.matches("^("+DMLWORDS+").*")){
			map=DML(sql);
		}else if(sql.matches("^("+DDLWORDS+").*")){
			map=DDL(sql);
		}
		return map;
	}
	// 格式化
	private static String format(String sql){
		sql=sql.replaceAll("^\\s+|\\s+$|\\n+|;$","")
				.replaceAll("\\s+"," ")
				.replaceAll("\\s*,\\s*",",");
		sql=replace(sql,"\\s*("+DMLWORDS+"|"+DDLWORDS+"|"+OPERTIONS+"|"+AGGREG_FUNCS+"|"+FIELD_FUNCS+"|"+OTHER+")\\s*",
				match->{
					return match.group(0).toLowerCase();
		});
		return sql;
	}
	// 解析
	public static Map<String,String> DML(String sql) {
		
	    Map<String,String> map=new LinkedHashMap<>();
	    // 格式化
		sql=sql.replaceAll("\\s*("+OPERTIONS+")\\s*","$1");
	 	// 保存
		map.put("sql",sql);
		
		
	 	// 转换distinct
		sql=distinct(sql);
		// 转换sql
		sql=translate(sql).replaceAll("\\s*("+DMLWORDS+")\\s+","#$0#").substring(1).concat("#");
				
		// 保存
		replace(sql,"("+DMLWORDS+")\\s+#(.*?)#",match->{
			String func=match.group(1);
			String param=match.group(2);
			map.put(func,param);
			return null;
		});
		// 聚合函数
		String aggreg_func="";
		for(String field : map.get("select").split(",")){
			if(field.matches("^("+AGGREG_FUNCS+").*")) {
				aggreg_func+=field+",";
			}
		}
		if(!aggreg_func.equals("")){
			map.put("aggreg_func",aggreg_func.replaceAll(",$",""));
		}
		
		return map;
	}
	// 别名
	public static Map<String,String> Alias(String select){
		Map<String,String> alias=new LinkedHashMap<>();
		for(String field:select.split(",")){
			replace(field,"(.*?)\\s+as\\s+(\\w+)",m->{
				String key=m.group(1);
				String value=m.group(2);
				alias.put(value,key);
				return null;
			});
		};
		return alias;
	}
	// 函数参数
	public static String[] Func(String exp){
		String regx="(^[a-zA-Z_][a-zA-Z0-9_]*)\\((.*?)\\)";
		Pattern p = Pattern.compile(regx);
		Matcher m = p.matcher(exp);
		String func=null;
		String param=null;
		while (m.find()) {
			func=m.group(1);
			param=m.group(2);
		}
		return new String[]{func,param};
	}
	
	// 替换掉括弧之中的逗号
	private static String translate(String str){
		int index=0;
		int start=-1;
		for(int i=0;i<str.length();i++){
			String chart=str.charAt(i)+"";
			if(chart.equals("(")){
				index++;
				start=i;
			}else if(chart.equals(")")){
				index--;
			}
			if(index==0&&start!=-1){
				String item=str.substring(start,i+1);
				str=str.replace(item,item.replace(",",":"));
				index=0;
				start=-1;
			}
		}
		return str;
	}
	// 转换distinct语句
	private static String distinct(String sql){
		final StringBuffer sqlbuff=new StringBuffer(sql);
		replace(sql,"(distinct)\\s+(.*?)\\s+(from)",match->{
 			if(!sqlbuff.toString().contains("group")){
 				sqlbuff.append(" group by "+match.group(2));
 			}
 			return null;
 		});
 		sql=sqlbuff.toString().replace("distinct","").replaceAll("\\s+"," ");
 		return sql;
	}
	private static String replace(String str,String rgx,Function<Matcher,String> func){
		Pattern p=Pattern.compile(rgx);
		Matcher m=p.matcher(str);
		while(m.find()){
			String replace=func.apply(m);
			if(replace!=null&&!replace.equals("")){
				str=str.replace(m.group(0),replace);
			}
		}
		return str;
	}
	
	// DDL语句解析
	public static Map<String,String> DDL(String sql){
		Map<String,String> map=new LinkedHashMap<>();
		if(sql.startsWith("show tables")){
			map.put(sql,"show tables");
			return map;
		}
		sql=sql.replaceAll("\\s*("+DDLWORDS+")\\s+","#$0#").substring(1).concat("#");
		replace(sql,"("+DDLWORDS+")\\s+#(.*?)#",match->{
			String func=match.group(1);
			String param=match.group(2);
			map.put(func,param);
			return null;
		});
		
		return map;
	}
	
	public static void main(String args[]){
		String sql=" select  name  ,sum(num),distinct user  ,json_path(sum(json,json),$.office) as office from  {log1,log2}.txt,log3.json where office = taobao and name  =  haoe group by user order by num limit 0 , 10";
		Parse(sql).forEach((key,value)->{
			System.out.printf("%s==>%s\n",key,value);
		});
		Alias(Parse(sql).get("select")).forEach((key,value)->{
			System.out.printf("%s==>%s\n",key,value);
		});
		
		String create="create table log3.tb (id,user,ip) fmt function format(row,field,fields){ for(var i in fields){ if(fields[i]==field){ return row.split('|')[i];};};return null;};";
		Parse(create).forEach((key,value)->{
			System.out.printf("%s==>%s\n",key,value);
		});
		String drop="drop table *.txt";
		Parse(drop).forEach((key,value)->{
			System.out.printf("%s==>%s\n",key,value);
		});
		String desc="desc *.txt";
		Parse(desc).forEach((key,value)->{
			System.out.printf("%s==>%s\n",key,value);
		});
		String show="show tables";
		Parse(show).forEach((key,value)->{
			System.out.printf("%s==>%s\n",key,value);
		});
	}
}

