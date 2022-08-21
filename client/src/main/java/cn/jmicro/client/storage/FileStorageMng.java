package cn.jmicro.client.storage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.async.ISuccess;
import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.client.IClientSession;
import cn.jmicro.api.client.IClientSessionManager;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.internal.async.Promise;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MT;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.security.SecretManager;
import cn.jmicro.api.storage.FileJRso;
import cn.jmicro.api.storage.IFileStorage;
import cn.jmicro.api.storage.IProgress;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.FileUtils;

/**
 * 对象存储类
 * 基于二进制流存储
 * 如果当前可以直接操作IFileStorageJMSrv实例，则直接做本地调用做存储，否则做远程调整存储
 * 
 * @author Yulei Ye
 * @date 2022年7月17日 上午9:20:12
 */
@Component(side=Constants.SIDE_COMSUMER)
public class FileStorageMng implements IMessageHandler{
	
	private final static Logger logger = LoggerFactory.getLogger(FileStorageMng.class);

	private static final Class<?> TAG = FileStorageMng.class;
	
	private final Map<String,FileResp> waitForResponse = new ConcurrentHashMap<>();
	
	@Inject(required=true)
	private IClientSessionManager sessionManager;
	
	@Inject(required=false)
	private IObjectStorage os;
	
	@Inject
	private ICodecFactory codecFactory;
	
	@Inject
	private SecretManager secManager;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject(required=true)
	private ProcessInfoJRso pi;
	
	@Cfg(value=Config.CFG_PREFIX_SYSTEM + "/filesystem/fileServerInsName", defGlobal=true)
	private String fileServerInsName = "apigateway";
	
	@Cfg(value=Config.CFG_PREFIX_SYSTEM + "/filesystem/host", defGlobal=true)
	private String host="127.0.0.1";
	
	@Cfg(value=Config.CFG_PREFIX_SYSTEM + "/filesystem/port", defGlobal=true)
	private String port = "9092";
	
	public String getFileId(String fn) {
		return idGenerator.getStringId(FileJRso.class)+"."+FileUtils.getFileExt(fn);
	}
	/**
	 * 
	 * @param attr 对象的附加属性
	 * @param file 要存储的文件
	 * @return 进度信息
	 */
	public IPromise<RespJRso<String>> saveFile(Map<String,String> attr, String filePath, IProgress p) {
		return new Promise<RespJRso<String>>((suc,fail)->{
			ActInfoJRso ai = JMicroContext.get().getAccount();
			
			File file = new File(filePath);
			FileJRso fvo = new FileJRso();
			fvo.setName(file.getName());
			fvo.setSize(file.length());
			fvo.setTochar(false);
			fvo.setType(Utils.getContentType(file.getAbsolutePath()));
			fvo.setUpdatedTime(file.lastModified());
			fvo.setLastModified(file.lastModified());
			fvo.setClientId(ai.getClientId());
			fvo.setCreatedBy(ai.getId());
			fvo.setId(this.getFileId(fvo.getName()));
			fvo.setUpdatedTime(ai.getId());
			fvo.setClientId(ai.getClientId());
			fvo.setCreatedBy(ai.getId());
			fvo.setLocalPath(filePath);
			fvo.setAttr(attr);
			fvo.setMcode(-1984341167);//存文件系统
			
			if(os != null && os.fileSystemEnable()) {
				RespJRso<String> r = os.saveFile2Db(fvo);
				suc.success(r);
			} else {
				FileResp f = new FileResp(p,fvo);
				f.suc = suc;
				saveThrouthNetwork(f);
			}
		});
	}
	
	public IPromise<RespJRso<String>> saveSteam(FileJRso fvo, InputStream is, IProgress p) {
		return new Promise<RespJRso<String>>((suc,fail)->{
			if(Utils.isEmpty(fvo.getId())) {
				fvo.setId(this.getFileId(fvo.getName()));
			}
			
			if(os != null && os.fileSystemEnable()) {
				RespJRso<String> r = os.saveSteam2Db(fvo,is);
				suc.success(r);
			} else {
				FileResp f = new FileResp(p,fvo);
				f.is = is;
				f.suc = suc;
				saveThrouthNetwork(f);
			}
			fvo.getId();
		});
	}
	
	public IPromise<RespJRso<String>> saveData(FileJRso fvo, byte[] byteData,IProgress p) {
		return new Promise<RespJRso<String>>((suc,fail)->{
			if(Utils.isEmpty(fvo.getId())) {
				fvo.setId(this.getFileId(fvo.getName()));
			}
			if(os != null && os.fileSystemEnable()) {
				RespJRso<String> r = os.saveByteArray2Db(fvo,byteData);
				suc.success(r);
			} else {
				FileResp f = new FileResp(p,fvo);
				f.is = new ByteArrayInputStream(byteData);
				f.suc = suc;
				saveThrouthNetwork(f);
			}
			fvo.getId();
		});
	}

	private void saveThrouthNetwork(FileResp f) {
		
		IClientSession s = this.sessionManager.getOrConnect(fileServerInsName,host, port);
		if(s == null) {
			doFail(f,"连接文件服务失败Ins: " + fileServerInsName + ",host: " + host + ",port: " + port,null);
			return;
		}
		
		Map<String,String> attr = f.f.getAttr();
		if(attr == null) {
			attr = new HashMap<>();
		}
		
		attr.put("sclient", pi.getClientId()+"");
		
		if(f.is == null) {
			try {
				f.is = new FileInputStream(new File(f.f.getLocalPath()));
			} catch (FileNotFoundException e) {
				doFail(f,"",e);
			}
		}
		
		f.session = s;
		waitForResponse.put(f.f.getId(),f);
		ByteBuffer data = ICodecFactory.encode(this.codecFactory, f.f, Message.PROTOCOL_BIN);
		
		sendMsg(f,data,IFileStorage.TYPE_INIT,0);
	}
	
	private void sendMsg(FileResp f, Object data, int phrase, int blockNo) {
		
        Message msg = new Message();
		msg.setType(Constants.MSG_TYPE_OBJECT_STORAGE);
		msg.setUpProtocol(Message.PROTOCOL_BIN);
		msg.setDownProtocol(Message.PROTOCOL_BIN);
		msg.setMsgId(idGenerator.getLongId(IRequest.class));
		
		msg.putExtra(Message.EXTRA_KEY_EXT0, phrase);
		msg.putExtra(Message.EXTRA_KEY_EXT1, f.f.getId());
		msg.putExtra(Message.EXTRA_KEY_EXT2, blockNo);
		
		if(f.ai != null) {
			msg.putExtra(Message.EXTRA_KEY_LOGIN_KEY, f.ai.getLoginKey());
		}
		
		if(pi.isLogin())  {
			msg.putExtra(Message.EXTRA_KEY_LOGIN_SYS, pi.getAi().getLoginKey());
		}
		
		//msg.setVersion(Message.MSG_VERSION);
		msg.setPriority(Message.PRIORITY_NORMAL);
		
		long curTime = TimeUtils.getCurTime();
		
		msg.setPayload(data);
		
		msg.setEncType(false);
		msg.setDownSsl(false);
		msg.setUpSsl(false);
		msg.setRpcMk(false);
		
		//msg.setStream(sm.isStream());
		//是否记录二进制流数据到日志文件
		msg.setDumpDownStream(false);
		msg.setDumpUpStream(false);
		msg.setRespType(Message.MSG_TYPE_PINGPONG);
		
		msg.setLogLevel(MC.LOG_NO);
		//往监控服务器上传监控包
		msg.setMonitorable(false);
		//控制在各JVM实例内部转出日志
		msg.setDebugMode(false);
		
		msg.setLinkId(JMicroContext.lid());
		msg.setInsId(pi.getId());
		
		if(msg.isDebugMode()) {
			//开启Debug模式，设置更多信息在消息包中，网络流及编码会有损耗，但更晚于问题追踪
			msg.setTime(curTime);
			//msg.setMethod(sm.getKey().toSnvm());
		}
		
		f.session.write(msg);
		if(msg.isMonitorable()) {
        	  MT.rpcEvent(MC.MT_CLIENT_IOSESSION_WRITE,1);
        	  MT.rpcEvent(MC.MT_CLIENT_IOSESSION_WRITE_BYTES,msg.getLen());
        }
	}

	@Override
	public Byte type() {
		return Constants.MSG_TYPE_OBJECT_STORAGE_RESP;
	}

	@Override
	public boolean onMessage(ISession session, Message respMsg) {

		if(LG.isLoggable(MC.LOG_DEBUG,respMsg.getLogLevel())) {
			LG.log(MC.LOG_DEBUG,TAG,"receive message");
		}
		
		String fid = respMsg.getExtra(Message.EXTRA_KEY_EXT1);
		FileResp p = waitForResponse.get(fid);
		
		if(p== null){
			String errMsg = LG.messageLog("waitForResponse keySet:" + waitForResponse.keySet(),respMsg);
			LG.log(MC.LOG_ERROR,TAG,errMsg);
			logger.error(errMsg);
			return false;
		}

		try {
			//下面处理响应消息
			if(respMsg.isMonitorable()) {
	      	  MT.rpcEvent(MC.MT_CLIENT_IOSESSION_READ, 1);
	      	  MT.rpcEvent(MC.MT_CLIENT_IOSESSION_READ_BYTES, respMsg.getLen());
	        }
			
			RespJRso<?> resp = ICodecFactory.decode(this.codecFactory, respMsg.getPayload(),
					RespJRso.class, respMsg.getDownProtocol());
			
			//假调参数是Promise,则返回值一定是RespJRso
			if(resp.getCode() != RespJRso.CODE_SUCCESS || respMsg.isError()) {
				if(resp.getCode() == MC.MT_AES_DECRYPT_ERROR) {
					//密钥问题，重置
					int insId = respMsg.getExtra(Message.EXTRA_KEY_INSID);
					this.secManager.resetLocalSecret(respMsg.isOuterMessage(),insId);
				}
				//服务器返回错误
				doRespFail(session,respMsg,resp,p);
				return true;
			}
			
			int ph = respMsg.getExtra(Message.EXTRA_KEY_EXT0);
			if(ph == IFileStorage.TYPE_INIT) {
				p.f = (FileJRso)resp.getData();
				startUploadData(p);
			}else if(ph == IFileStorage.TYPE_NEXT) {
				sendNextBlock(p);
			}else if(ph == IFileStorage.TYPE_FINISH) {
				finishUpload(p,resp);
			}
			
		}catch(Throwable e) {
			String errMsg = "";
			logger.error(errMsg,e);
    		LG.log(MC.LOG_ERROR, TAG,errMsg);
    		MT.rpcEvent(MC.MT_REQ_ERROR);
    		return false;
		} finally {
			
		}
	
		return true;
	}
	
	private void startUploadData(FileResp fr) {
		FileJRso f = fr.f;
		int blockNum = (int)(f.getSize() / f.getBlockSize());
		if(f.getSize() % f.getBlockSize() > 0) {
			blockNum++;
		}
		f.setTotalBlockNum(blockNum);
		f.setFinishBlockNum(0);

		fr.cache = new byte[f.getBlockSize()];
		//fr.is = new FileInputStream(fr.file);
		if(fr.p != null) fr.p.onStart(fr.f);
		 
		this.sendNextBlock(fr);
	
	}

	private void finishUpload(FileResp fr,RespJRso<?> resp) {
		RespJRso<String> r = new RespJRso<>(resp.getCode(),fr.f.getId());
		if(fr.p != null) fr.p.onEnd(fr.f, r);
		r.setData(fr.f.getId());
		fr.suc.success(r);
		close(fr);
	}

	private void sendNextBlock(FileResp fr) {
		byte[] cache = fr.cache;
		boolean last = false;
		if(fr.f.getTotalBlockNum() -1 == fr.f.getFinishBlockNum()) {
			int len = (int)(fr.f.getSize() % fr.f.getBlockSize());
			cache = new byte[len];
			last = true;
		}
		
		try {
			fr.f.setFinishBlockNum(fr.f.getFinishBlockNum()+1);
			if(fr.p != null) fr.p.onPregress(fr.f, (int)((fr.f.getFinishBlockNum() *100.0) / fr.f.getSize()) );
			fr.is.read(cache);
			if(last) {
				//最后一块
				sendMsg(fr, cache, IFileStorage.TYPE_FINISH, fr.f.getFinishBlockNum()-1);
			} else {
				sendMsg(fr, cache, IFileStorage.TYPE_NEXT, fr.f.getFinishBlockNum()-1);
			}
			
		} catch (IOException e) {
			RespJRso<String> r = new RespJRso<>(RespJRso.CODE_FAIL, "读文件异常："+fr.file.getAbsolutePath());
			logger.error(r.getMsg(),e);
			fr.p.onEnd(fr.f, r);
			r.setData(fr.f.getId());
			fr.suc.success(r);
			close(fr);
			return;
		}
		
	}

	private void close(FileResp fr) {
		if(fr.is != null) {
			try {
				fr.is.close();
			} catch (IOException e) {
				logger.error("close input stream error: "+fr.file.getAbsolutePath(),e);
			}
		}
		waitForResponse.remove(fr.f.getId());
	}

	private void doRespFail(ISession session, Message respMsg, RespJRso<?> resp, FileResp fr) {
		RespJRso<String> r = new RespJRso<>(resp.getCode(), "上传失败：" + resp.getMsg());
		logger.error(r.getMsg());
		if(fr.p != null) fr.p.onEnd(fr.f, r);
		r.setData(fr.f.getId());
		fr.suc.success(r);
		this.close(fr);
	}
	
	private void doFail(FileResp fr,String msg,Throwable e) {
		RespJRso<String> r = new RespJRso<>(RespJRso.CODE_FAIL, msg);
		r.setData(fr.f.getId());
		logger.error(r.getMsg(),e);
		if(fr.p != null) fr.p.onEnd(fr.f, r);
		fr.suc.success(r);
		this.close(fr);
	}

	class FileResp {
		
		private FileResp(IProgress p,FileJRso f) {
			this.p = p;
			this.f = f;
		}
		
		private ActInfoJRso ai;
		private IProgress p;
		private FileJRso f;
		private File file;
		private InputStream is;
		private byte[] cache = null;
		
		private ISuccess<RespJRso<String>> suc;
		
		private IClientSession session;
	}
	
}
