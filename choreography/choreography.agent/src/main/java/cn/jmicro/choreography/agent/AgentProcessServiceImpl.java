package cn.jmicro.choreography.agent;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.choreography.IAgentProcessService;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.mng.LogFileEntry;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.pubsub.PubSubManager;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.choreography.instance.InstanceManager;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.common.util.SystemUtils;

@Component
@Service(namespace="chro", version="0.0.1")
public class AgentProcessServiceImpl implements IAgentProcessService {

	private final static Logger logger = LoggerFactory.getLogger(AgentProcessServiceImpl.class);
	
	@Inject
	private ServiceAgent srvAgent;
	
	@Inject
	private Config cfg;
	
	@Inject
	private ProcessInfo pi;
	
	@Inject
	private PubSubManager pm;
	
	@Inject
	private InstanceManager im;
	
	private Map<String,FileWatcher> fileWatchers = new HashMap<>();
	
	private File workDirFile;
	
	private File agentLogDirFile;
	
	public void ready() {
		String workDir = cfg.getString(Constants.INSTANCE_DATA_DIR,"") + File.separatorChar + "agentInstanceDir";
		workDirFile = new File(workDir);
		agentLogDirFile = new File(System.getProperty("user.dir") + File.separatorChar + "logs");
		
		TimerTicker.doInBaseTicker(1, "fileWatchers", null, (key,att) -> {
			doWatch();
		});
	}
	
	private void doWatch() {
		
		if(fileWatchers.isEmpty()) {
			return;
		}
		
		Set<FileWatcher> ws = new HashSet<>();
		synchronized(fileWatchers) {
			ws.addAll(this.fileWatchers.values());
		}
		
		for(FileWatcher w : ws) {
			try {
				w.watcherLog();
			} catch (IOException | InterruptedException e) {
				logger.error("",e);
			}
		}
		
	}

	@Override
	public Set<ProcessInfo> getProcessesByDepId(String depId) {
		if(StringUtils.isEmpty(depId)) {
			return null;
		}
		Set<ProcessInfo> ps = getAllProcesses() ;
		Iterator<ProcessInfo> ite = ps.iterator();
		while(ite.hasNext()) {
			ProcessInfo pi = ite.next();
			if(!pi.getDepId().equals(depId)) {
				ite.remove();
			}
		}
		return ps;
	}

	@Override
	public Set<ProcessInfo> getAllProcesses() {
		
		Set<ProcessInfo> ps = new HashSet<>();
		String[] fnames = this.workDirFile.list();

		for(String fn : fnames) {
			String path = this.workDirFile.getAbsolutePath() + File.separatorChar + fn + File.separatorChar + "processInfo.json";
			String json = SystemUtils.getFileString(path);
			ProcessInfo p = JsonUtils.getIns().fromJson(json, ProcessInfo.class);
			if(p != null ) {
				ps.add(p);
			}
		}
		return ps;
	}

	@Override
	public List<LogFileEntry> getProcessesLogFileList() {
		
		Set<ProcessInfo> ps = getAllProcesses();
		if(ps == null || ps.isEmpty()) {
			return null;
		}
		
		List<LogFileEntry> logs = new ArrayList<>();
		for(ProcessInfo pi : ps) {
			LogFileEntry le = new LogFileEntry();
			le.setAgentId(pi.getAgentId());
			le.setInstanceName(pi.getInstanceName());
			le.setProcessId(pi.getId());
			le.setActive(im.getProcessesByInsId(pi.getId(), false) != null);
			List<String> logFiles = getLogFiles(pi.getId());
			le.setLogFileList(logFiles);
			logs.add(le);
		}
		
		return logs;
		
	}

	private List<String> getLogFiles(String processId) {
		String parentDir = "";
		File dir = new File(this.workDirFile.getAbsoluteFile() + File.separator + processId + File.separator  + "logs/",
				parentDir);
		if(!dir.exists()) {
			return null;
		}
		
		List<String> files = new ArrayList<>();
		
		File[] fs = dir.listFiles();
		for(File f : fs) {
			if(f.isFile()) {
				files.add(f.getName());
			}
		}
		return files;
	}

	@Override
	public LogFileEntry getItselfLogFileList() {
		List<String> files = new ArrayList<>();
		File[] fs = agentLogDirFile.listFiles();
		
		for(File f : fs) {
			getLogFile(f,"/",files);
		}
		
		if(new File("./nohup.out").exists()) {
			files.add("/nohup.out");
		}
		
		LogFileEntry le = new LogFileEntry();
		le.setAgentId(Config.getInstanceName());
		le.setInstanceName(Config.getInstanceName());
		
		if(pi != null) {
			le.setProcessId(pi.getId());
		}
		le.setLogFileList(files);
		
		return le;
	}

	private void getLogFile(File f, String parent, List<String> files) {
		String p = parent + File.separatorChar + f.getName();
		if(f.isFile()) {
			files.add(p);
		} else {
			File[] fs = f.listFiles();
			for(File f0 : fs) {
				getLogFile(f0,p,files);
			}
		}
	}

	@Override
	public String agentId() {
		return Config.getInstanceName();
	}

	@Override
	public boolean startLogMonitor(String processId, String logFile, int lineNum) {
		String piLogDir =  processId + File.separatorChar + "logs";
		String fullPath = piLogDir + File.separatorChar + logFile;
		
		long readPoint = readReverse(fullPath, Constants.CHARSET, lineNum);
		boolean pactive = im.getProcessesByInsId(processId, false) == null;
		if(readPoint == -1 || !pactive) {
			logger.info(processId + " : " + logFile + ", rp: " + readPoint + ", pactive: " + pactive);
			return false;
		}
		
		if(!fileWatchers.containsKey(processId)) {
			synchronized(fileWatchers) {
				if(!fileWatchers.containsKey(processId)) {
					FileWatcher fw = new FileWatcher(workDirFile.getAbsolutePath() + File.separatorChar + piLogDir);
					fw.start();
					fileWatchers.put(processId, fw);
				}
			}
		}
		
		String topic0 = "/" + fullPath.replaceAll("\\\\", "/");
		
		FileWatcher fw = fileWatchers.get(processId);
		if(!fw.containsFile(logFile)) {
			fw.addFile(logFile, readPoint, (content)->{
				publishLog(topic0,content);
			});
		}
		
		return true;
	}

	@Override
	public boolean stopLogMonitor(String processId, String logFile) {
		FileWatcher fw = fileWatchers.get(processId);
		if(fw != null && fw.containsFile(logFile)) {
			fw.removeFile(logFile);
		}
		
		if(fw.isEmpty()) {
			fileWatchers.remove(processId);
		}
		return true;
	}
	
    private long readReverse(String filename, String charset, int lineNum) {
        RandomAccessFile rf = null;
        
        String topic = "/" + filename.replaceAll("\\\\", "/");
        try {
        	String apath = this.workDirFile.getAbsolutePath() + File.separatorChar + filename;
            rf = new RandomAccessFile(apath, "r");
            long fileLength = rf.length();
            
            if(lineNum <= 0 || fileLength == 0) {
            	return fileLength;
            }
            
            long start = rf.getFilePointer(); //返回此文件中的当前偏移量
            long readIndex = start + fileLength - 1;
            rf.seek(readIndex); //设置偏移量为文件末尾
            int c = -1;
            
            while (readIndex > start ) {
                c = rf.read();
                readIndex--;
                rf.seek(readIndex);
                if (c == '\n' || c == '\r') {
                	readIndex--;
                	rf.seek(readIndex);
                	if(--lineNum < 0 || readIndex < 0) {
                    	break;
                    }
                }
            }
            
            if(readIndex < 0) {
            	readIndex = 0;
            }
            
            rf.seek(readIndex+3);
            
            StringBuffer sb = new StringBuffer();
            String line = null;
            while((line = rf.readLine()) != null) {
            	  sb.append(line).append("<br/>");
            	  if(sb.length() > 9192) {
            		  //pm.publish(null, topic, sb.toString(), PSData.FLAG_PUBSUB);
            		  publishLog(topic,sb.toString());
            		  //System.out.print(sb.toString());
            		  sb.delete(0, sb.length());
            	  }
            }
            
            if(sb.length() > 0) {
            	 publishLog(topic,sb.toString());
            	 //pm.publish(null, topic, sb.toString(), PSData.FLAG_PUBSUB);
            	//System.out.print(sb.toString());
            }
           
            return fileLength;
        } catch (IOException e) {
            logger.error("Read error: " +filename,e);
            return -1;
        } finally {
            try {
                if (rf != null) {
                	 rf.close();
                }
            } catch (IOException e) {
            	logger.error("Close error: " +filename,e);
            }
        }
    }
    
    public void publishLog(String topic,String content) {
    	logger.debug(topic + " : "+content);
    	PSData item = new PSData();
		item.setTopic(topic);
		item.setData(new Object[] {content});
		item.setContext(null);
		item.setFlag(PSData.FLAG_PUBSUB);
		pm.publish(item);
    }

}
