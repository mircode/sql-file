package com.core;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 文件工具类
 * @author 魏国兴
 *
 */
public class FileUtil {

	public static final String CMD="cd|ls|cat";
	
	public static void execute(String ws,String args[]) throws Exception{
		String cmd=args[0];
		if(cmd.equals("ls")){
			ls(ws);
		}else if(cmd.equals("cd")){
			cd(ws,args[1]);
		}else if(cmd.equals("cat")){
			cat(ws,args[1]);
		}
	}
	public static void ls(String workspace) throws Exception{
		Path path=Paths.get(workspace);
        info(path);
	}
	public static void cd(String workspace,String targets) throws Exception{
		Path path=null;
		if(targets.matches("^[A-Za-z]:\\.*")){
			path=Paths.get(targets,".");
		}else if(targets.startsWith("/")){
			path=Paths.get(targets);
		}else{
			path=Paths.get(workspace,targets);
		}
		if(path.toFile().exists()){
			Config.setWorkSpace(path.toString());
			info(path);
		}else{
			System.out.println("cd: "+targets+": No such file or directory");
		}
	}
	public static void cat(String workspace,String file) throws Exception{
		Path path=Paths.get(workspace,file);
		if(path.toFile().isFile()){
			Files.lines(path).forEach(System.out::println);
		}
	}
	public static void info(Path path) throws Exception{
		
		print("dir=>%s",new String[]{path.toString()});
		
		List<String> buff=new ArrayList<>();
		buff.add("name|type|time|size");
		
		File[] files=path.toFile().listFiles();
		
		for(File file : files){  
        	String name=file.getName();
        	String type=file.isFile()?"file":"dir";
        	buff.add(String.format("%s|%s|%s|%s",name,type,time(file),size(file)));
        } 
		print(buff);
	}
	private static String time(File file){
    	return new SimpleDateFormat("yyyyMMdd HH:dd:mm").format(new Date(file.lastModified()));
	}
	private static String size(File file){
		long size=file.length();
		if(size/1024==0){
			return size+"B";
		}
		if(size/1024>0){
			return size+"K";
		}
		if(size/(1024*1024)>0){
			return size+"M";
		}
		if(size/(1024*1024*1024)>0){
			return size+"G";
		}
		return size+"B";
	}
	private static void print(List<String> lines){
		List<Integer> maxlen=new ArrayList<>();
		int length=0;
		for(String line:lines){
			String args[]=line.split("[|]");
			for(int i=0;i<args.length;i++){
				int len=args[i].length();
				if(maxlen.size()<args.length){
					maxlen.add(len);
				}else{
					Integer max=maxlen.get(i);
					if(max<len){
						maxlen.set(i,len);
					}
				}
			}
			if(line.length()>length){
				length=line.length();
			}
		}
		String format="";
		for(int i=0;i<lines.get(0).split("[|]").length;i++){
			format+=""+"%-"+(maxlen.get(i)+5)+"s";
		}
		for(int i=0;i<lines.size();i++){
			print(format,lines.get(i).split("[|]"));
			if(i==0) print(line(length));
		}
	}
	private static void print(String format){
		print(format,null);
	}
	private static void print(String format,Object[] args){
		System.out.println(String.format(format, args));
	}
	private static String line(int length){
		String line="";
		for(int i=0,len=length+15;i<len;i++){
			line+="-";
		}
		return line;
	}
}
