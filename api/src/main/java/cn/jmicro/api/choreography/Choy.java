package cn.jmicro.api.choreography;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.common.util.SystemUtils;

@Component(level=100)
public class Choy {
	
	private static final Logger logger = LoggerFactory.getLogger(Choy.class);
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ComponentIdServer idServer;
	
	@Inject
	private Config cfg;
	
	public void ready() {
		saveProccessId();
	}
	
	private void saveProccessId() {
		
		String initProcessInfoPath = cfg.getString(ChoyConstants.PROCESS_INFO_FILE,null);
		String json = "";
		
		if(StringUtils.isEmpty(initProcessInfoPath)) {
			String dataDir = cfg.getString(Constants.INSTANCE_DATA_DIR,null);
			if(StringUtils.isEmpty(dataDir)) {
				throw new CommonException("Data Dir ["+Constants.INSTANCE_DATA_DIR+"] cannot be a file");
			}
			initProcessInfoPath = dataDir + File.separatorChar + "processInfo.json";
		} 
		
		File processInfoData = new File(initProcessInfoPath);
		
		if(processInfoData.exists()) {
			json = SystemUtils.getFileString(processInfoData);
		}else {
			try {
				processInfoData.createNewFile();
			} catch (IOException e) {
				throw new CommonException("Fail to create file [" +processInfoData+"]");
			}
		}
		
		logger.info("Origit ProcessInfo:" + json);
		
		ProcessInfo pi = null;
		if(StringUtils.isNotEmpty(json)) {
			pi = JsonUtils.getIns().fromJson(json, ProcessInfo.class);
			//编排环境下启动的实例
			//checkPreProcess(pi);
		} else {
			//非编排环境下启动的实例
			pi = new ProcessInfo();
			pi.setAgentHost(Config.getHost());
			pi.setAgentId(Config.getCommandParam(ChoyConstants.ARG_AGENT_ID));
			pi.setDepId(Config.getCommandParam(ChoyConstants.ARG_DEP_ID));
			
			String id = Config.getCommandParam(ChoyConstants.ARG_INSTANCE_ID);
			if(StringUtils.isNotEmpty(id)) {
				pi.setId(id);
			}else {
				pi.setId(idServer.getStringId(ProcessInfo.class));
			}
			pi.setAgentProcessId(Config.getCommandParam(ChoyConstants.ARG_MYPARENT_ID));
		}
		
		String pid = SystemUtils.getProcessId();
		logger.info("Process ID:" + pid);
		pi.setPid(pid);
		pi.setActive(true);
		pi.setInstanceName(Config.getInstanceName());
		pi.setHost(Config.getHost());
		pi.setDataDir(cfg.getString(Constants.INSTANCE_DATA_DIR,null));
		pi.setOpTime(System.currentTimeMillis());
		//pi.setTimeOut(0);
		
		String p = ChoyConstants.INS_ROOT+"/" + pi.getId();
		final String js = JsonUtils.getIns().toJson(pi);
		if(op.exist(p)) {
			String oldJson = op.getData(p);
			ProcessInfo pri = JsonUtils.getIns().fromJson(oldJson, ProcessInfo.class);
			if(pri != null && pri.isActive()) {
				throw new CommonException("Process exist[" +oldJson+"]");
			}
			op.deleteNode(p);
		}
		
		op.createNodeOrSetData(p,js ,IDataOperator.EPHEMERAL);
		
		logger.info("Update ProcessInfo:" + js);
		
		initProcessInfoPath = cfg.getString(Constants.INSTANCE_DATA_DIR,null) + File.separatorChar + "processInfo.json";
		SystemUtils.setFileString(initProcessInfoPath, js);
		
		op.addNodeListener(p, (int type, String path,String data)->{
			//防止被误删除，只要此进程还在，此结点就不应该消失
			if(type == IListener.REMOVE) {
				op.createNodeOrSetData(p,js ,true);
				logger.warn("Recreate process info node: " + js);
			}else if(type == IListener.DATA_CHANGE) {
				ProcessInfo pi0 = JsonUtils.getIns().fromJson(data, ProcessInfo.class);
				if(!pi0.isActive()) {
					op.deleteNode(p);
					logger.warn("JVM exit by other system");
					System.exit(0);
				}
			}
		});
		
	}

}
