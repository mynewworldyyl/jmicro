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
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileWatcher {

	private final Logger logger = LoggerFactory.getLogger(FileWatcher.class);
	
	private String dir;
	
	//private Set<String> fileNames = new HashSet<>();
	
	private WatchService watchService;
	
	private Map<String,Consumer<String>> consumers = new HashMap<>();
	
	private Map<String,Long> bpoints = new HashMap<>();
	
	private  WatchKey key;
	
	public FileWatcher(String dir) {
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
	
	public boolean addFile(String fileName,Long points,Consumer<String> consumer) {
		if(!bpoints.containsKey(fileName)) {
			consumers.put(fileName, consumer);
			bpoints.put(fileName, points);
			return true;
		}
		return false;
	}
	
	public boolean containsFile(String fileName) {
		return consumers.containsKey(fileName);
	}
	
	public boolean isEmpty() {
		return consumers.isEmpty();
	}
	
	public void removeFile(String fileName) {
		if(!bpoints.containsKey(fileName)) {
			consumers.remove(fileName);
			bpoints.remove(fileName);
		}
	}
	
    public void watcherLog() throws IOException, InterruptedException {
    	key = watchService.poll();
    	if(key == null) {
    		return;
    	}
        List<WatchEvent<?>> watchEvents = key.pollEvents();
        for(WatchEvent<?> e : watchEvents) {
        	/*if (e.count() > 1) {
        		continue;
            }*/
        	
        	String n = ((Path) e.context()).getFileName().toString();
        	if(!bpoints.containsKey(n)) {
        		continue;
        	}
        	
        	if(StandardWatchEventKinds.ENTRY_DELETE == e.kind()) {
        		bpoints.remove(n);
        		consumers.remove(n);
        		continue;
        	}
        	
        	if(StandardWatchEventKinds.ENTRY_MODIFY == e.kind()) {
        		  
        		  File configFile = Paths.get(dir + "/" + e.context()).toFile();
                  StringBuilder str = new StringBuilder();
                  long len = getFileContent(configFile, bpoints.get(n), str);
                  bpoints.put(n, len);
                  Consumer<String> c = consumers.get(n);
                  if (str.length() != 0 && c != null ) {
                      c.accept(str.toString());
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

