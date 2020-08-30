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
import cn.jmicro.api.pubsub.ILocalCallback;
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
	
	private Set<LogFileReader> fileReaders = new HashSet<>();
	
	private File workDirFile;
	
	private File agentLogDirFile;
	
	public void ready() {
		String workDir = cfg.getString(Constants.INSTANCE_DATA_DIR,"") + File.separatorChar + "agentInstanceDir";
		workDirFile = new File(workDir);
		agentLogDirFile = new File(System.getProperty("user.dir") + File.separatorChar + "logs");
		
		TimerTicker.doInBaseTicker(1, "fileWatchers", null, (key,att) -> {
			doReader();
			doWatch();
		});
	}
	
	private void doReader() {
		if(fileReaders.isEmpty()) {
			return;
		}
		
		Set<LogFileReader> ws = new HashSet<>();
		synchronized(fileReaders) {
			ws.addAll(this.fileReaders);
		}
		
		for(LogFileReader r : ws) {
			if(-1 == r.readOne()) {
				//读到文件最后一行，开始做监听
				fileReaders.remove(r);
				registWatch(r.getProcessId(), r.getLogFile(), r.fileLength);
			}
		}
		
	}

	private void registWatch(String processId, String logFile,long fileLength) {
		
		String piLogDir =  processId + File.separatorChar + "logs";
		String fullPath = processId + File.separatorChar + "logs" + File.separatorChar + logFile;
		
		boolean pactive = im.getProcessesByInsId(processId, false) != null;
		if(!pactive) {
			logger.info(processId + " : " + logFile + ", pactive: " + pactive);
			return;
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
			fw.addFile(logFile, fileLength, (type,fileName,content)->{
				if(type == FileWatcher.NORMAL) {
					publishLog(topic0,content,(code,item)->{
						if(code == PubSubManager.PUB_TOPIC_INVALID) {
							 boolean initStatus = fw.getTopicStatus(logFile);
		           			 if(!initStatus) {
		           				 //从可用状态进入不可用状态
		           				 stopLogMonitor(processId,fileName);
		           			 }
	           		 	}
					});
				}else if(type == FileWatcher.FILE_DELETE || type == FileWatcher.IDLE_TIMEOUT) {
					stopLogMonitor(processId,fileName);
				}
			});
		}
		
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
		Set<ProcessInfo> ps = getAllProcesses();
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
			if(StringUtils.isNotEmpty(json)) {
				ProcessInfo p = JsonUtils.getIns().fromJson(json, ProcessInfo.class);
				if(p != null ) {
					ps.add(p);
				}
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
		
		if(lineNum > 0) {
			LogFileReader reader = new LogFileReader(processId,logFile,lineNum);
			reader.init();
			synchronized(fileReaders) {
				fileReaders.add(reader);
			}
		} else {
			boolean pactive = im.getProcessesByInsId(processId, false) == null;
			if(!pactive) {
				logger.info(processId + " : " + logFile +  ", pactive: " + pactive);
				return false;
			}
			String apath = workDirFile.getAbsolutePath()  +  "/" + processId + "/" + "logs" +  "/" + logFile;
			File lf = new File(apath);
			this.registWatch(processId, logFile, lf.length());
		}
		return true;
		/*
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
		*/
	}

	@Override
	public boolean stopLogMonitor(String processId, String logFile) {
		logger.info("stopLogMonitor processId: {}  logFile: {}",processId,logFile);
		FileWatcher fw = fileWatchers.get(processId);
		if(fw != null && fw.containsFile(logFile)) {
			fw.removeFile(logFile);
		}
		
		if(fw != null && fw.isEmpty()) {
			fileWatchers.remove(processId);
			fw.close();
		}
		
		if(!fileReaders.isEmpty()) {
			LogFileReader lf = new LogFileReader(processId,logFile,0);
			synchronized(fileReaders) {
				if(this.fileReaders.contains(lf)) {
					fileReaders.remove(lf);
				}
			}
		}
		
		return true;
	}
    
    public void publishLog(String topic,String content,ILocalCallback cb) {
    	//logger.debug(topic + " : " + content);
    	PSData item = new PSData();
		item.setTopic(topic);
		item.setData(new Object[] {content});
		item.setContext(null);
		item.setLocalCallback(cb);
		item.setFlag(PSData.FLAG_PUBSUB);
		pm.publish(item);
    }
    
    private class LogFileReader {
    	
    	private String processId;
    	private String logFile;
    	private int lineNum;
    	
    	private RandomAccessFile rf = null;
    	
    	private long fileLength;
    	
    	private String topic;
    	
    	private boolean initStatus = true;
    	
    	public LogFileReader(String processId, String logFile, int lineNum) {
    		this.processId = processId;
    		this.logFile = logFile;
    		this.lineNum = lineNum;
    	}
    	
    	public long init() {
    		
    		try {
    			topic = "/" + processId + "/" + "logs" +  "/" + logFile;
    			String apath = workDirFile.getAbsolutePath()  + topic;
				rf = new RandomAccessFile(apath, "r");
				fileLength = rf.length();
				
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
				    if(c == '\n' || c == '\r') {
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
			} catch (IOException e) {
				logger.error("init error",e);
			}
            
    		return fileLength;
    	}
    	
        private long readOne() {
           
        	/*if(!pm.hasTopic(this.topic)) {
        		logger.info("topic {} is invalid now!",this.topic );
        		return 1;
        	}*/
        	
        	String line = null;
            try {
            	StringBuffer sb = new StringBuffer();
                while((line = rf.readLine()) != null) {
                	  sb.append(line).append("<br/>");
                	  //break;
                	  if(sb.length() > 9192) {
                		 break;
                	  }
                }
                
                if(sb.length() > 0) {
                	 publishLog(topic,sb.toString(),(code,item)->{
                		 if(code == PubSubManager.PUB_TOPIC_INVALID) {
                			 if(!initStatus) {
                				 //从可用状态进入不可用状态
                				 stopLogMonitor(this.processId,this.logFile);
                			 }else {
                				 //topic进入可用状态
                				 initStatus = false;
                			 }
                		 }
                	 });
                	 //pm.publish(null, topic, sb.toString(), PSData.FLAG_PUBSUB);
                	 //System.out.print(sb.toString());
                }
                
                if(line == null) {
                	return -1;
                }else {
                	return sb.length();
                }
            } catch (IOException e) {
                logger.error("Read error: " + logFile,e);
                return -1;
            } finally {
                try {
                	if(line == null && rf != null) {
                		rf.close();
                    }
                } catch (IOException e) {
                	logger.error("Close error: " +logFile,e);
                }
            }
        }

		public String getProcessId() {
			return processId;
		}

		public String getLogFile() {
			return logFile;
		}

		@Override
		public int hashCode() {
			return (this.processId + this.logFile).hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if(obj == null) {
				return false;
			}
			
			if(!(obj instanceof LogFileReader)) {
				return false;
			}
			
			LogFileReader lf = (LogFileReader)obj;
			
			return this.processId.equals(lf.processId) && this.logFile.equals(lf.logFile);
		}
        
    }

}
