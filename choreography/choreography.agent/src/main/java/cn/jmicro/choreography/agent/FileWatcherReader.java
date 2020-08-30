package cn.jmicro.choreography.agent;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileWatcherReader {

	private final Logger logger = LoggerFactory.getLogger(FileWatcherReader.class);
	
	public static final int NORMAL = 0;
	
	public static final int FILE_DELETE = 1;
	
	public static final int NO_CHANGE = 2;
	
	private String dir;
	
	//主题是否在可用状态，或初始化状态
	//如果是初始化状态，则等侍主题可用，如果是可用状态，则主题进入不可用状时，需要停止日志分发
	private Map<String,LogFileEntry> logFileEntries = new HashMap<>();
	
	private WatchService watchService;
	
	private  WatchKey key;
	
	private class LogFileEntry {
		private String logFileName;
		private boolean initStatus = true;
		private IFileListener listener;
		private long readPoint;
		
		LogFileEntry(String fileName, long points, IFileListener consumer) {
			this.logFileName = fileName;
			this.initStatus = true;
			this.listener = consumer;
			this.readPoint = points;
		}
	}
	
	public FileWatcherReader(String dir) {
		 this.dir = dir;
	}
	
	public boolean start() {
		try {
			watchService = FileSystems.getDefault().newWatchService();
			Paths.get(dir).register(watchService, StandardWatchEventKinds.ENTRY_MODIFY,
					StandardWatchEventKinds.ENTRY_DELETE);
			return true;
		} catch (IOException e) {
			logger.error("", e);
			return false;
		}
	}
	
	public boolean addFile(String fileName, long points, IFileListener consumer) {
		if(!logFileEntries.containsKey(fileName)) {
			LogFileEntry le = new LogFileEntry(fileName,points,consumer);
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
		try {
			this.watchService.close();
		} catch (IOException e) {
			logger.error("close error: " + dir,e);
		}
	}
	
	public void removeFile(String fileName) {
		if(logFileEntries.containsKey(fileName)) {
			logFileEntries.remove(fileName);
		}
	}
	
    public void watcherLog() throws IOException, InterruptedException {
    	key = watchService.poll();
    	if(key == null) {
    		return ;
    	}
    	
        List<WatchEvent<?>> watchEvents = key.pollEvents();
        for(WatchEvent<?> e : watchEvents) {
        	/*if (e.count() > 1) {
        		continue;
            }*/
        	
        	String n = ((Path) e.context()).getFileName().toString();
        	if(!logFileEntries.containsKey(n)) {
        		continue;
        	}
        	
        	LogFileEntry le = logFileEntries.get(n);
        	
        	if(StandardWatchEventKinds.ENTRY_DELETE == e.kind()) {
        		le.listener.onEvent(FILE_DELETE, n,null);
        		logFileEntries.remove(n);
        		logger.info("Logfile delete: " + n);
        		continue;
        	}
        	
        	if(StandardWatchEventKinds.ENTRY_MODIFY == e.kind()) {
        		  File configFile = Paths.get(dir + "/" + e.context()).toFile();
                  StringBuilder str = new StringBuilder();
                  long len = getFileContent(configFile, le.readPoint, str);
                  le.readPoint = len;
                  if (str.length() != 0 ) {
                	  le.listener.onEvent(NORMAL, n, str.toString());
                  }
        	}
        }
        
        key.reset();
    
    }

   
    private long getFileContent(File configFile, long beginPointer, StringBuilder str) {
        if (beginPointer < 0) {
            beginPointer = 0;
        }
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(configFile, "r");
            if (beginPointer > file.length()) {
                return 0;
            }
            file.seek(beginPointer);
            String line;
            while ((line = file.readLine()) != null) {
            	str.append(line).append("<br/>");
            }
            return file.getFilePointer();
        } catch (IOException e) {
            logger.error("Read File error: " + configFile.getAbsolutePath(),e);
            return -1;
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                	logger.error("Close file error: " + configFile.getAbsolutePath(),e);
                }
            }
        }
    }
}

