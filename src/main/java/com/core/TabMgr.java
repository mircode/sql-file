package com.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.core.format.FormatFactory;
import com.core.format.Formater;


/**
 * SQL-DDL语句
 * @author weiguoxing
 */
public class TabMgr {
		
	File file=null;
	String ws=null;
	
	public TabMgr(String ws) throws IOException{
		this(ws,null);
	}
	public TabMgr(String ws,String tablefile) throws IOException{
		this.ws=ws;
		Path path=Paths.get(ws);
		if(tablefile==null){
			tablefile="table.fs";
		}
		file=new File(path.toFile(),tablefile);
		if(!file.exists()){
			file.createNewFile();
		}
	}
	public void run(String cmd) throws IOException{
		Map<String,String> map=Parser.Parse(cmd);
		if(map.get("create table")!=null){
			new TabMgr(ws).create(cmd);
		}
		if(map.get("update table")!=null){
			new TabMgr(ws).update(cmd);
		}
		if(map.get("drop table")!=null){
			new TabMgr(ws).drop(cmd);
		}
		if(map.get("desc")!=null){
			new TabMgr(ws).desc(cmd);
		}
		if(map.get("show tables")!=null){
			new TabMgr(ws).show(cmd);
		}
	}
	public TabMgr create(String sql) throws IOException{
		List<String> lines=read(file);
		Map<String,String> map=Parser.Parse(sql);
		String key=map.get("create table").replaceAll("[()]","");
		String table=key.split("\\s+")[0];
		String format=map.get("fmt");
		long exist=lines.stream().filter(row->row.split("\\s+")[0].equals(table)).count();
		if(exist==0){
			lines.add(key+" "+format);
			write(file,lines);
		}
		return this;
	}
	public TabMgr update(String sql) throws IOException{
		List<String> lines=read(file);
		Map<String,String> map=Parser.Parse(sql);
		String key=map.get("update table").replaceAll("[()]","");
		String table=key.split("\\s+")[0];
		String format=map.get("fmt");
		lines=lines.stream().map(row->{
				if(row.split("\\s+")[0].equals(table)){
					return key+" "+format;
				}
				return row;
		}).collect(Collectors.toList());
		write(file,lines);
		return this;
	}
	public TabMgr drop(String sql) throws IOException{
		List<String> lines=read(file);
		Map<String,String> map=Parser.Parse(sql);
		String table=map.get("drop table");
		lines=lines.stream().filter(row->!row.split("\\s+")[0].equals(table)).collect(Collectors.toList());
		write(file,lines);
		return this;
	}
	public TabMgr show(String sql) throws IOException{
		List<String> lines=Files.readAllLines(file.toPath());
		List<String> buff=lines.stream().map(row->row.split("\\s+")[0]).collect(Collectors.toList());
		// hook for xxx.tb file
		Paths.get(Config.getWorkspace()).toFile().listFiles(file->{
			String name=file.getName();
			if((name.endsWith(".csv")||name.endsWith(".tb"))&&!buff.contains(name)){
				buff.add(name);
			}
			return true;
		});
		new Printer("table").table(buff);
		return this;
	}
	// desc xxx desc xxx more
	public TabMgr desc(String sql) throws IOException{
		List<String> lines=read(file);
		Map<String,String> map=Parser.Parse(sql);
		String table=map.get("desc");
		
		boolean more=false;
		if(table.contains("more")){
			more=true;
			table=table.split("\\s+")[0];
		}
		String tb=table;
		
		// hook for xxx.tb file
		if(table.endsWith(".tb")||table.endsWith(".csv")){
			String fields=readFields(Paths.get(Config.getWorkspace(),table).toFile());
			List<String> buff=new ArrayList<>();
			buff.add(fields+"#table");
			new Printer("fields#format","#").table(buff);
			return this;
		}
		
		boolean flag=more;
		List<String> buff=lines.stream()
				.filter(row->row.split("\\s+")[0].equals(tb))
				.map(row->{
					String infos[]=row.split("\\s+");
					
					String type=infos[2];
					if(flag){
						type=row.replace(infos[0]+" "+infos[1]+" ","");
					}else{
						if(infos[2].startsWith("~")){
							type="regex";
						}
						if(infos[2].equals("|")){
							type="default";
						}
						if(infos[2].startsWith("function")){
							type="script";
						}
					}
					return infos[1]+"#"+type;
				}).collect(Collectors.toList());
		new Printer("fields#format","#").table(buff);
		return this;
	}
	public TabMgr clear(){
		if(this.file.exists()){
			this.file.delete();
		}
		return this;
	}
	private List<String> read(File file) throws IOException{
		List<String> lines=Files.lines(file.toPath()).filter(row->{
			return !(row.equals("")||row.matches("^\\s*#.*"));
		}).collect(Collectors.toList());
		return lines;
	}
	private void write(File file,List<String> lines) throws IOException{
		Files.write(file.toPath(),lines);
	}
	public String readFields(File file){
		String fields=null;
		if(file.getName().endsWith(".tb")||file.getName().endsWith(".csv")){
			Iterator<String> it;
			try {
				it = Files.lines(file.toPath()).iterator();
				while(it.hasNext()){
					String row=it.next();
					if(!row.startsWith("+")){
						fields=row.replaceAll("^[|,]|[|,]$","").replaceAll("\\s+","").replaceAll("[|]",",");
						break;
					}
				}
			} catch (IOException e) {
				return null;
			}
		}
		return fields;
	}
	public Map<File,Formater> search(String name,String base) throws Exception{
		if(base==null){base=".";}
		Map<File,Formater> res=new LinkedHashMap<>();
		// 读取格式
		List<String> lines=read(file);
		// 符合条件的文件
		File[] files=FileFilter.search(name,base);
		for(File file:files){
			// 查找对应文件的格式
			String fields=null;
			String express=null;
			boolean find=false;
			String filename=file.getName();
			
			// hook for xxx.tb file
			fields=readFields(file);
			if(fields!=null){
				if(filename.endsWith(".tb")){
					res.put(file,FormatFactory.formater(fields,"tb"));
				}else if(filename.endsWith(".csv")){
					res.put(file,FormatFactory.formater(fields,"csv"));
				}
				continue;
			}
			
			for(String line:lines){
				String[] infos=line.split("\\s+");
				String rgx=infos[0];
				String fds=infos[1];
				String exp=infos[2];
				if(filename.equals(rgx)){
					fields=fds;
					express=exp;
					find=true;
					break;
				}
				// 最后一次匹配
				if(match(filename,rgx)){
					fields=fds;
					express=exp;
					find=true;
				}
			}
			if(find==true){
				res.put(file,FormatFactory.formater(fields,express));
			}
		}
		return res;
	}
	private boolean match(String name,String filter){
		boolean flag=false;
		try{
			if(name.matches(filter)){
				flag=true;
			}
		}catch(Exception e){
			filter=filter.replace(",","|")
						 .replace(".","\\.")
					     .replace("*",".*?")
					     .replace("{","(").replace("}",")");
		
			try{
				if(name.matches(filter)){
					flag=true;
				}
			}catch(Exception ee){
				flag=false;
			}
		}
		return flag;
	}
	public static void main(String args[]) throws Exception{

		String sql="create table *.txt (num,id,ip,user,date,segment) fmt ~(.*?)|(.*?)|(.*)";
		new TabMgr(Config.getWorkspace()).create(sql).show("show tables").desc("desc *.txt more");
		
		sql="update table *.txt (num,id,ip,user,date,segment) fmt json";
		new TabMgr(Config.getWorkspace()).update(sql).show("show tables").desc("desc *.txt");
		
		sql="drop table *.txt";
		new TabMgr(Config.getWorkspace()).drop(sql).show("show tables");
		
		new TabMgr(Config.getWorkspace()).clear();
		
//		new TabMgr(Config.WORKSPACE).search("./**/*.txt",Config.WORKSPACE).forEach((file,fmt)->{
//			System.out.println(file.getName()+"==>"+fmt.fields()[0]);
//		});
	
		
	}
}
