package com.shell;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import com.core.Config;
import com.core.FileUtil;
import com.core.SQLMain;
import com.log.Log;
/**
 * 控制台
 * @author 魏国兴
 *
 */
public class Console {

    Scanner in;
    PrintStream out;
    List<String> cmds;
    public Console(){
    	in=new Scanner(System.in);
        out=System.out;
        try {
    		File file=Paths.get(Config.getUserHome(),"his.fs").toFile();
    		if(!file.exists()){
    			file.createNewFile();
    		}
			cmds=Files.readAllLines(file.toPath());
		} catch (Exception e) {
			if(cmds==null) cmds=new LinkedList<>();
		}
    }
    public void execute(){
    	version();
    	while(true){
    		 out.printf("sql> ");
    		 // read
    		 String line = in.nextLine().trim();
			 while(line.matches("^(select|create|update).*")&&!line.endsWith(";")){
				 out.printf("  -> ");
				 line+=in.nextLine().trim();
			 }
			 // format
			 line=line.replaceAll("^\\s+|\\s+$|;$|\n","");
			 // execute
			 try{
				 if(line.equals("exit")) { exit();break;} 
				 run(line);
	    	 }catch(Exception e){
	    		 Log.log("execute error:",e);
			 }finally{}
    	}
    }
    public void run(String line) throws Exception{
		 if(line.startsWith("!")){
			 history(line);
		 }else if(line.equals("help")||line.equals("?")){
			 help();
		 }else if(line.matches("^("+FileUtil.CMD+")\\s+.*")||line.matches("^"+FileUtil.CMD+"$")){
			 FileUtil.execute(Config.getWorkspace(),line.split("\\s+"));
		 }else{
			 cmds.add(line);
			 SQLMain.execute(Config.getTableDir(),Config.getWorkspace(),line);
		 }
    }
    public void exit() throws Exception{
    	int end=cmds.size();
    	int start=end>50?(end-50):0;
    	File file=Paths.get(Config.getUserHome(),"his.fs").toFile();
		if(!file.exists()){
			file.createNewFile();
		}
    	Files.write(file.toPath(),cmds.subList(start,end),StandardOpenOption.WRITE);
    }
    public void history(String cmd) throws Exception{
    	String num=cmd.substring(1);
		if(num.equals("!")){
			 for(int i=0,len=cmds.size();i<len;i++)
			 out.println(String.format("%s\t%s","!"+(len-i),cmds.get(i)));
		}else{
			 run(cmds.get((cmds.size()-Integer.parseInt(num))));
		}
    }
    public void version(){
    	String software="";
    	software+="    _______ __  __             _____        __    \n";
    	software+="   /  ____//_/ / /   _____    /  __/ ___   / /    \n";
    	software+="  / /___  __  / /   / __  |   |__| / __ | / /     \n";
    	software+=" / /___/ / / / /__ /  ____/  ___/ / /  / / /___   \n";
    	software+="/_/     /_/ /____/ |_/__|   /___ /|___| |_____/   \n";
    	software+="                                       |_|        \n";
    	software+="                                                  \n";
    	software+="Copyright(c) 2017, weiguoxing | version: v.1.0    \n\n";
		out.printf(software);
    }
    public void help(){
    	String help="";
    	help+="List of all FileSQL commands:                                        \n";
    	help+="?                synonym for `help`                                  \n";
    	help+="help             display this help                                   \n";
    	help+="cd               switch the dir                                      \n";
    	help+="ls               ls the files of dir                                 \n";
    	help+="cat              print the content of file                           \n";
    	help+="!!               list the history of command                         \n";
    	help+="exit             exit filesql                                        \n";
    	help+="select           select f1,f2 from table where f1>0 limit 0,10       \n";
    	help+="create table     create table *.txt (f1,f2,f3) fmt |                 \n";
    	help+="update table     update table  *.txt (f1,f2,f3) fmt json             \n";
		help+="drop table       drop table *.txt                                    \n";
		help+="desc table       desc *.txt                                          \n";
		help+="show tables      show tables                                         \n";
		
		out.printf(help);
    }
    // java com.shell.Console -Dscript=xxxxx -Dtable=xxxx -Dworkspace=xxx 
    public static void main(String args[]){
    	for(String arg:args){
    		if(arg.startsWith("-Dlog")){
				Config.setLogDir(arg.split("=")[1]);
			}
    		if(arg.startsWith("-Dscript")){
    			Config.setScriptDir(arg.split("=")[1]);
    		}
    		if(arg.startsWith("-Dtable")){
    			Config.setTableDir(arg.split("=")[1]);
    		}
			if(arg.startsWith("-Dworkspace")){
				Config.setWorkSpace(arg.split("=")[1]);
			}
    	}
    	new Console().execute();
    	
    }
}
