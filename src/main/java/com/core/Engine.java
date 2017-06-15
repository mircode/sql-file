package com.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Stream;

import com.core.format.Formater;
import com.core.format.SplitFormater;

/**
 * 执行引擎
 * @author 魏国兴
 *
 */
public class Engine {
	
	// SQL
	public String SQL=null;
	public String SELECT=null;
	public String FROM=null;
	public String WHERE=null;
	public String GROUP=null;
	public String ORDER=null;
	public String LIMIT=null;
	public String AGGREG_FUNC=null;
	public String FIELD_FUNC=null;
	public String INTO=null;
	public Map<String,String> ALIAS=null;
	
	// 内部格式
	private String TOKEN="|";
	private SplitFormater TABLE=null;
	
	public Engine(String sql){
		Map<String,String> map=Parser.Parse(sql);
		this.SELECT=map.get("select");
		this.FROM=map.get("from");
		this.WHERE=map.get("where");
		this.GROUP=map.get("group by");
		this.ORDER=map.get("order by");
		this.LIMIT=map.get("limit");
		this.AGGREG_FUNC=map.get("aggreg_func");
		this.FIELD_FUNC=map.get("field_func");
		this.INTO=map.get("into");
		this.ALIAS=Parser.Alias(this.SELECT);
	}
	
	private SplitFormater tableFormat(Formater formater){
		String group=this.GROUP;
		String where=this.WHERE;
		String order=this.ORDER;
		String select=this.SELECT;
		String aggreg=this.AGGREG_FUNC;
		
		if(select.equals("*")){
			return new SplitFormater(formater.fields,TOKEN);
		}
		
		Set<String> fieldSet=new LinkedHashSet<>();
		if(group!=null){
			fieldSet.addAll(Arrays.asList(group.split(",")));
		}else if(group==null&&aggreg!=null){
			fieldSet.add("all");
		}
		if(select!=null){
			String fields="";
			for(String field:select.split(",")){
				field=field.replaceAll("(.*?)\\s+as\\s+(.*)","$1");
				fields+=field+",";
			}
			fieldSet.addAll(Arrays.asList(fields.replaceAll(",$","").split(",")));
		}
		if(where!=null){
			for(String field:formater.fields()){
				fieldSet.add(field);
			}
		}
		if(order!=null){
			order=order.replaceAll("\\s+(asc|desc)\\s*","");
			fieldSet.addAll(Arrays.asList(order.split(",")));
		}
		String format="";
		for(String set:fieldSet){
			if(this.ALIAS.get(set)==null){
				format+=set+",";
			}
		}
		format=format.replaceAll(",$","");
		return new SplitFormater(format,TOKEN);
	}
	
	public String field(String row,Formater table) {
		if(this.TABLE==null){
			this.TABLE=tableFormat(table);
		}
		
		// hook for xxx.tb file
		if(row.startsWith("+-")){
			return null;
		}
		String head=row.replaceAll("^[|,]|[|,]$","");
		if(head.equals(table.fields)){
			return null;
		}
		head=head.replaceAll("\\s+","").replaceAll("[|]",",");
		if(head.equals(table.fields)){
			return null;
		}
		
		String select=this.SELECT;
		if(select.equals("*")){
			return table.flatRow(row);
		} 
		String fields="";
		for(String field:TABLE.fields()){
			String val=null;
			if(field.equals("all")){
				val="all";
				fields+=val+TOKEN;
				continue;
			}
			field=field.replaceAll("(.*?)\\s+as\\s+(.*)","$1");
			try{
				val=table.format(row,field);
				if(val==null||val.equals("")){
					if(field.matches("^("+Parser.AGGREG_FUNCS+").*")){
						String funcs[]=Parser.Func(field);
						String func=funcs[0];
						String param=funcs[1];
						val=func.equals("count")?"1":table.format(row,param);
					}else{
						val=SQLFunc.execute(field,row,table);
					}
				}
			}catch(Exception e){
				throw new RuntimeException("error",e);
			}
			fields+=val+TOKEN;
		}
		return fields.replaceAll("["+TOKEN+"]$","");
		
	}
	
	public String select(String row){
		String select=this.SELECT;
		if(select.equals("*")) return row;
		return getFields(row,select);
	}
	
	public String group(String row){
		String group=this.GROUP;
		if(group==null||group.equals("")) return "all";
		return getFields(row,group);
	}
	
	public List<String> reduce(Map<String,List<String>> group){
		final List<String> lines=new ArrayList<>();
		group.forEach((key,list)->{
			 String matrix=this.AGGREG_FUNC;
			 String line="";
			 String token=TOKEN;
			 if(matrix==null){
				 lines.add(key+token+line.replaceAll("["+TOKEN+"]$",""));
				 return;
			 }
			 for(String field : matrix.split(",")){
				final String func=Parser.Func(field)[0];
				Stream<String> math=list.stream().map(row->this.getFields(row,field));
		                 
				String res="";
				if(func.equals("sum")){
					res=math.mapToDouble(Double::parseDouble).sum()+"";
				}
				if(func.equals("avg")){
					Double sum=math.mapToDouble(row->{
						if(row.split("/").length==2){
							return Double.parseDouble(row.split("/")[0]);
						}else{
							return Double.parseDouble(row);
						}
					}).sum();
					math=list.stream().map(row->this.getFields(row,field));
					Double count=math.mapToDouble(row->{
						if(row.split("/").length==2){
							return Double.parseDouble(row.split("/")[1]);
						}else{
							return 1;
						}
					}).sum();
					res=String.format("%.2f",sum/count);
				}
				if(func.equals("max")){
					res=math.mapToDouble(Double::parseDouble).max().getAsDouble()+"";
				}
				if(func.equals("min")){
					res=math.mapToDouble(Double::parseDouble).min().getAsDouble()+"";
				}
				if(func.equals("count")){
					res=math.mapToDouble(Double::parseDouble).sum()+"";
				}
				res=res.replaceAll("(\\.0+)$","");
				line+=res+token;
			 }
			 lines.add(key+token+line.replaceAll("["+TOKEN+"]$",""));
		});
		return lines;
	}
	public boolean where(String row){
		
		// 空行
		if(row==null||row.equals("")){
			return false;
		}
		
		String where=this.WHERE;
		if(where==null||where.equals("")) return true;
		
		String wheres[]=where.split("\\s+");
		Stack<String> opt=new Stack<>();
		List<String> output=new ArrayList<>();

		// 将中缀表达式转换为后缀表达式
		for(String it : wheres){
			if(it.equals("and")||it.equals("or")){
				if(opt.isEmpty()||opt.peek().equals("(")) {
					opt.push(it);
				}else{
					output.add(opt.pop());
				}
			}else if(it.equals("(")){
				opt.push(it);
			}else if (it.equals(")")){
				String el=opt.pop();
				while(el.equals("(")==false){
					output.add(el);
					el=opt.pop();
				}
			}else{
				output.add(it);
			}
		}
		while(!opt.isEmpty()){
			output.add(opt.pop());
		}

		// 存放操作符
		Stack<Boolean> opts=new Stack<>();
		// 解析后缀表达式并运算结果
		for(String v : output){
			if(v.equals("or")) {
				boolean v1=opts.pop();
				boolean v2=opts.pop();
				opts.push(v1 || v2);
			}else if(v.equals("and")){
				boolean v1 = opts.pop();
				boolean v2 = opts.pop();
				opts.push(v1 && v2);
			}else if(v.equals("true")||v.equals("false")){
				return Boolean.parseBoolean(v);
			}else{
				String col=v.split(Parser.OPERTIONS)[0];
				String val=this.getFields(row,col);
				opts.push(SQLFunc.compare(v.replace(col,val)));
			}
		}
		// 取出计算结果
		return opts.pop();
	}

	public int order(String row1,String row2){
		String order=this.ORDER;
		String orders[] = order.split(",");
		int res=0;
		for(String cols : orders){
			String[] args=cols.split("\\s+");
			String col=args[0];
			String type="asc";
			if(args.length==2){
				type=args[1];
			}
			String val1=this.getFields(row1,col);
			String val2=this.getFields(row2,col);
			if(val1.matches("\\d+\\.?\\d*")) {
				Integer v1 = Integer.parseInt(val1);
				Integer v2 = Integer.parseInt(val2);
				res=type.equals("asc")?v1.compareTo(v2):v2.compareTo(v1);
			}else{
				String v1=val1;
				String v2=val2;
				res=type.equals("asc")?v1.compareTo(v2):v2.compareTo(v1);
			}
			if(res!=0){
				break;
			}
		}
		return res;
	}
	public String getHeader(){
		if(this.SELECT.equals("*")){
			return this.TABLE.fields.replace(",",TOKEN);
		}
		String fields="";
		for(String field:this.SELECT.split(",")){
			fields+=field.replaceAll("(.*?)\\s+as\\s+(.*)","$2")+TOKEN;
		}
		return fields.replaceAll("["+TOKEN+"]$","");
	}
	public String getFields(String row,String fields){
		if(fields==null||fields.equals("*")||fields.equals("")){
			return row;
		}
		String res="";
		for(String field:fields.split(",")){
			field=field.replaceAll("(.*?)\\s+as\\s+(.*)","$1");
			String value=TABLE.format(row,field);
			if(value==null||value.equals("")){
				value=TABLE.format(row,ALIAS.get(field));
			}
			res+=value+TOKEN;
		}
		return res.replaceAll("["+TOKEN+"]$","");
	}
	public Long[] limit(){
		if(LIMIT==null) return null;
		String[] limits=LIMIT.split(",");
		long skip=0;
		long limit=0;
		if(limits.length==1){
			limit=Long.parseLong(limits[0]);
		}
		if(limits.length==2){
			skip=Long.parseLong(limits[0]);
			limit=Long.parseLong(limits[1]);
		}
		return new Long[]{skip,limit};
	}
	public boolean isgroup(){
		return (AGGREG_FUNC!=null||GROUP!=null);
	}
	
	
}
