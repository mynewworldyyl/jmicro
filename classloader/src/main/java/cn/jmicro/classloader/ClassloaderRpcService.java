package cn.jmicro.classloader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.classloader.IClassloaderRpc;
import cn.jmicro.api.classloader.RemoteClassRegister;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.common.Constants;

@Service(version="0.0.1",timeout=30000,showFront=false,clientId=Constants.NO_CLIENT_ID)
@Component
public class ClassloaderRpcService implements IClassloaderRpc {

	private final static Logger logger = LoggerFactory.getLogger(ClassloaderRpcService.class);
	
	/*@Cfg(value="/")
	private String jarResp;*/
	
	@Override
	public byte[] getClassData(String clazz,Integer dataVersion,boolean isTesting) {
		
		try {
			
			if(clazz.indexOf("/") > 0) {
				clazz = clazz.replaceAll("/", ".");
				if(clazz.endsWith(".class")) {
					clazz = clazz.substring(0,clazz.length()-".class".length());
				}
			}
			
			String resName = clazz.replaceAll("\\.", "/");
			if(!resName.startsWith("/")) {
				resName ="/" + resName;
			}
			
			if(!resName.endsWith(".class")) {
				resName=resName + ".class";
			}
			
			InputStream is = Class.forName(clazz).getResourceAsStream(resName);
			
			ByteArrayOutputStream bais = new ByteArrayOutputStream();
			byte[] data = new byte[1024];
			
			int len = 0;
			while((len = is.read(data, 0, data.length)) > 0) {
				bais.write(data, 0, len);
			}
			
			byte[] clsData = bais.toByteArray();
			
			String msg = "return class: "+resName+", data length: " + clsData.length;
			logger.info(msg);
			LG.log(MC.LOG_INFO, this.getClass(), msg);
			
			return clsData;
			
		} catch (ClassNotFoundException | IOException e) {
			logger.warn(clazz);
			LG.log(MC.LOG_ERROR, this.getClass(), "Fail to loadd class: "+clazz,e);
		}
		return null;
	}

	@Override
	public Resp<Boolean> registRemoteClass(RemoteClassRegister r) {
		return new Resp<>(Resp.CODE_FAIL,false);
	}

	
}