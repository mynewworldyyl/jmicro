package cn.jmicro.api.net;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.codec.OnePrefixDecoder;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.executor.ExecutorConfig;
import cn.jmicro.api.executor.ExecutorFactory;
import cn.jmicro.common.util.DateUtils;


public class DumpManager {

	private static final Logger logger = LoggerFactory.getLogger(DumpManager.class);
	
	private static  DumpManager ins = null;
	public static DumpManager getIns() {
		if(ins != null) {
			return ins;
		}
		synchronized(DumpManager.class) {
			if(ins != null) {
				return ins;
			}
			ins = new DumpManager();
		}
		return ins;
	}
	
	private DumpManager() {
		ExecutorConfig config = new ExecutorConfig();
		config.setMsCoreSize(1);
		config.setMsMaxSize(5);
		config.setTaskQueueSize(10);
		executor = new ExecutorFactory().createExecutor(config);
	}
	
	private OnePrefixDecoder decoder =  new OnePrefixDecoder();
	
	private String dumpFileDir = "D:\\opensource\\github\\dumpdir";
	
	private String dumpFileName = null;
	
	private OutputStream dumpStream = null;
	
	private ExecutorService executor = null;
	
	public void doDump(ByteBuffer buffer) {
		executor.submit(()->{
			doDump1(buffer);
		});
	}
	
	private void doDump1(ByteBuffer buffer) {
		if(dumpFileDir == null || "".equals(dumpFileDir.trim())) {
			dumpFileDir = System.getProperty("user.dir");
		}
		
		if(this.dumpFileName == null || "".equals(dumpFileName.trim())) {
			dumpFileName = Config.getInstanceName()+"-"+ DateUtils.formatDate(new Date(), "YYYYMMddHHmm");
		}
		
		if(this.dumpStream == null) {
			try {
				this.dumpStream = new FileOutputStream(dumpFileDir+"/"+dumpFileName+".dump");
			} catch (FileNotFoundException e) {
				logger.error("",e);
				return;
			}
		}
		
		if(this.dumpStream == null) {
			logger.error(dumpFileDir+"/"+dumpFileName+".data"+" not found");
			return;
		}
		
		try {
			dumpStream.write(buffer.array(), buffer.position(), buffer.remaining());
			dumpStream.flush();
		} catch (IOException e) {
			logger.error("fail to dump data: {}",e,buffer);
		}
	}
	
	public List<Message> parseDumpData(ByteBuffer buffer,String methodFilter) {
     	
		if(buffer == null || buffer.remaining() == 0) {
			return null;
		}
		
		//buffer.position(buffer.limit());
		
		List<Message> msgs = new ArrayList<>();
		
     	while(true) {
     		 try {
     			  Message message =  Message.readMessage(buffer);
				 if(message == null){
				  	break;
				  }
				 if(message.isDebugMode()) {
          			 logger.debug("T:{},payload:{}",Thread.currentThread().getName(),message);
          		  }
				  if(methodFilter != null) {
					  if(methodFilter.equals(message.getMethod())) {
						  msgs.add(message);
					  }
				  } else {
					  msgs.add(message);
				  }
				  
			} catch (Throwable e) {
				e.printStackTrace();
			}
     	 }
     	return msgs;
	}
	
	public List<Message> parseDumpData(String path,String methodFilter) {

		FileInputStream is = null;
		try {
			is = new FileInputStream(path);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
			int len = -1;
			byte[] data = new byte[512];
			while((len = is.read(data)) > 0) {
				baos.write(data, 0, len);
			}
			
			ByteBuffer bb = ByteBuffer.wrap(baos.toByteArray());
			
			return parseDumpData(bb,methodFilter);
		} catch (IOException e) {
			logger.error("",e);
		} finally {
			if(is != null) {
				try {
					is.close();
				} catch (IOException e) {
					logger.error("",e);
				}
			}
		}
		return null;
	}
	
	public void printByLinkId(String path,long linkId) {
		List<Message> msgs = parseDumpData(path,null);
		if(msgs == null || msgs.isEmpty()) {
			logger.debug("{} no data",path);
			return;
		}
		
		for(Message m : msgs) {
			if(linkId== -1 || m.getLinkId() == linkId) {
				logger.info("{} MSG {},resp {}",linkId,m.toString(),decoder.decode((ByteBuffer)m.getPayload()));
			}
		}
	}
	
	public void reqResp(String outputFileName,String groupBy,String methodFilter) {
		File dir = new File(dumpFileDir);
		List<String> files = new ArrayList<String>();
		for(File f: dir.listFiles((d,name)->{return name.endsWith(".dump");} )) {
			files.add(f.getAbsolutePath());
		}
		reqResp(outputFileName,groupBy,methodFilter,files.toArray(new String[files.size()]));
	}
	
	public void reqResp(String outputPath,String groupBy,String methodFilter,String... inputs) {
		
		List<Message> msgs = new ArrayList<>();
		for(String p: inputs) {
			List<Message> l = parseDumpData(p,methodFilter);
			if(l != null) {
				msgs.addAll(l);
			}else {
				logger.warn("Null data for path: {}",p);
			}
		}
				
		if(msgs == null || msgs.isEmpty()) {
			logger.debug("{} no data",Arrays.asList(inputs).toString());
			return;
		}
		
		Comparator<Long> c = (o1,o2) -> {return o1 > o2 ? 1:(o1.longValue() == o2.longValue()?0:-1);};
		Map<Long,List<Message>> map = new TreeMap<>(c);
		
		for(Message m : msgs) {
			Long gid =  m.getId();
			if("lid".equals(groupBy)) {
				gid = m.getLinkId();
			}else if("reqid".equals(groupBy)) {
				gid = m.getReqId();
			}
			
			if(!map.containsKey(gid)) {
				map.put(gid, new ArrayList<Message>());
			}
			map.get(gid).add(m);
		}
		
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dumpFileDir+"/"+outputPath)));
			
			for(Long key: map.keySet()) {
				bw.write(groupBy + " ID:");
				bw.write(key+"");
				bw.newLine();
				for(Message m : map.get(key)) {
					bw.write("instance:"+m.getInstanceName());
					bw.write(", method:"+m.getMethod());
					bw.write(", time:"+DateUtils.formatDate(new Date(m.getTime()),DateUtils.PATTERN_YYYY_MM_DD_HHMMSSZZZ));
					bw.newLine();
					bw.write(m.toString());
					bw.newLine();
					if(m.getPayload() != null) {
						Object resp = null;
						try {
							resp = decoder.decode((ByteBuffer)m.getPayload());
						} catch (Throwable e) {
							bw.write("decode error for: "+e.getMessage());
							bw.write(m.getPayload().toString());
							continue;
						}
						if(resp != null) {
							bw.write(resp.toString());
						} else {
							bw.write("decode error for: ");
							bw.write(m.getPayload().toString());
						}
					}
					bw.newLine();
					bw.write("**************");
				}
				bw.write("============================================");
				bw.newLine();
				
			}
			
			bw.flush();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			if(bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
	
	
	public String getDumpFileDir() {
		return dumpFileDir;
	}

	public void setDumpFileDir(String dumpFileDir) {
		this.dumpFileDir = dumpFileDir;
	}

	public String getDumpFileName() {
		return dumpFileName;
	}

	public void setDumpFileName(String dumpFileName) {
		this.dumpFileName = dumpFileName;
	}

	public OutputStream getDumpStream() {
		return dumpStream;
	}

	public void setDumpStream(OutputStream dumpStream) {
		this.dumpStream = dumpStream;
	}
	
}
