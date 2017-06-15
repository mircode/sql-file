package com.core;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.core.format.Formater;

/**
 * SQL上下文
 * @author 魏国兴
 *
 */
public class FileSQL {

	// 时间戳 
	long time;
	
	// 配置文件目录
	String config=null;
	// 当前工作空间
	String workspace=null;
	
	// 输入输出
	File output=null;
	Map<File,Formater> input=null;
	
	// 执行引擎
	Engine engine=null;
	// 数据流
	Stream<String> stream=null;
	
	
	// 构造函数
	public FileSQL(){
		this(".");
	}
	public FileSQL(String workspace){
		this(workspace,workspace);
	}
	public FileSQL(String config,String workspace){
		this.config=config;
		this.workspace=workspace;
	}
	
	// 执行
	public FileSQL run(String sql) throws Exception{
		time=System.currentTimeMillis();
		engine=new Engine(sql);
		input=new TabMgr(config).search(engine.FROM,workspace);
		if(engine.INTO!=null){
			File file=null;
			if(Paths.get(engine.INTO).isAbsolute()){
				file=Paths.get(engine.INTO).toFile();
			}else{
				file=Paths.get(workspace,engine.INTO).toFile();
			}
			if(file.exists()) file.delete();
			output=file;
		}
		if(engine.isgroup())
			group();
		else
			filter();
		return this;
	}
	private void filter() throws Exception{
		// map
		List<Stream<String>> streams=new ArrayList<>();
		input.forEach((f,fmt)->{
			try{
			 Stream<String> stream=Files.lines(f.toPath())
					  .map(row->engine.field(row,fmt))
					  .filter(engine::where);
			 streams.add(stream);
			}catch(Exception e){
				throw new RuntimeException("error",e);
			}
		});
		// reduce
		stream=streams.parallelStream().flatMap(child->child);
		// save
		sort().limit().print();
	}
	private void group() throws Exception{
		// map
		List<Stream<String>> streams=new ArrayList<>();
		input.forEach((f,fmt)->{
			try{
				Map<String,List<String>> group=Files.lines(f.toPath())
						  .map(row->engine.field(row,fmt))
						  .filter(engine::where)
						  .collect(Collectors.groupingBy(engine::group));
				
				streams.add(engine.reduce(group).stream());
			}catch(Exception e){
				throw new RuntimeException("error",e);
			}
		});
		// reduce
		Map<String,List<String>> reduce=streams.parallelStream().flatMap(child->child).collect(Collectors.groupingBy(engine::group));
		stream=engine.reduce(reduce).stream();
		// save
		sort().limit().print();
	}
	private FileSQL limit(){
		if(engine.LIMIT!=null){
			Long limits[]=engine.limit();
			stream=stream.skip(limits[0]).limit(limits[1]);
		}
		return this;
	}
	private FileSQL sort(){
		if(engine.ORDER!=null)
		stream=stream.parallel().sorted(engine::order);
		return this;
	}
	private FileSQL print() throws Exception{
		Printer printer=new Printer(time);
		PrintStream out=output!=null?new PrintStream(output):null;
		stream.map(engine::select).forEachOrdered(row->printer.table(engine.getHeader(),row,out));
		printer.close();
		return this;
	}
	public static void main(String args[]) throws Exception{
		String sql="select id,ip,user,name from {log1,log2}.txt";
		new FileSQL(Config.getWorkspace()).run(sql);
	}
}
