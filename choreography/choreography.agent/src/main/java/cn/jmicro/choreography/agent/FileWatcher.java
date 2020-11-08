package cn.jmicro.choreography.agent;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.utils.TimeUtils;

public class FileWatcher {

	private final Logger logger = LoggerFactory.getLogger(FileWatcher.class);
	
	public static final int NORMAL = 0;
	
	public static final int FILE_DELETE = 1;
	
	public static final int NO_CHANGE = 2;
	
	public static final int IDLE_TIMEOUT = 3;
	
	private String dir;
	
	//主题是否在可用状态，或初始化状态
	//如果是初始化状态，则等侍主题可用，如果是可用状态，则主题进入不可用状时，需要停止日志分发
	private Map<String,LogFileEntry> logFileEntries = new HashMap<>();
	
	private class LogFileEntry {
		private String logFileName;
		private boolean initStatus = true;
		private IFileListener listener;
		private long readPoint;
		private long lastReadTime = TimeUtils.getCurTime();
		
		private RandomAccessFile r;
		
		LogFileEntry(String fileName, long points, IFileListener consumer) {
			this.logFileName = fileName;
			this.initStatus = true;
			this.listener = consumer;
			this.readPoint = points;
		}
	}
	
	public FileWatcher(String dir) {
		 this.dir = dir;
	}
	
	public boolean start() {
		return true;
	}
	
	public boolean addFile(String fileName, long points, IFileListener consumer) {
		if(!logFileEntries.containsKey(fileName)) {
			LogFileEntry le = new LogFileEntry(fileName,points,consumer);
			try {
				le.r =  new RandomAccessFile(this.dir + File.separator + fileName, "r");
				le.r.seek(points);
			} catch (IOException e) {
				logger.error("",e);
				return false;
			}
			logFileEntries.put(fileName, le);
			return true;
		}
		return false;
	}
	
	public boolean getTopicStatus(String fileName) {
		return logFileEntries.get(fileName).initStatus;
	}
	
	
	public boolean containsFile(String fileName) {
		return logFileEntries.containsKey(fileName);
	}
	
	public boolean isEmpty() {
		return logFileEntries.isEmpty();
	}
	
	public void close() {
		
	}
	
	public void removeFile(String fileName) {
		if(logFileEntries.containsKey(fileName)) {
			LogFileEntry le = logFileEntries.remove(fileName);
			try {
				le.r.close();
			} catch (IOException e) {
				logger.error("close error: " + fileName,e);
			}
		}
	}
	
    public void watcherLog() throws IOException, InterruptedException {
    	
    	long curTime = TimeUtils.getCurTime();
    	
        Set<String> keys = logFileEntries.keySet();
        for(String k : keys) {
        	LogFileEntry le = logFileEntries.get(k);
     
        	 String line = le.r.readLine();
        	 if(line == null) {
        		 if(curTime - le.lastReadTime > 18000000) {
        			 //超过30分钟没日志产生，关闭日志监听
        			 le.listener.onEvent(IDLE_TIMEOUT, le.logFileName, null);
        			 le.listener.onEvent(NORMAL, le.logFileName, "Stop log for timeout over 30 minutes");
        		 }
        		 continue;
        	 }
        	 
        	le.lastReadTime = curTime;
            //le.r.seek(le.readPoint);
            StringBuilder str = new StringBuilder(line).append("<br/>");
            
            while ((line = le.r.readLine()) != null) {
            	str.append(line).append("<br/>");
            }
        
            if(str.length() > 0) {
            	le.listener.onEvent(NORMAL, le.logFileName, str.toString());
            }
            
        }
    }
   
}

