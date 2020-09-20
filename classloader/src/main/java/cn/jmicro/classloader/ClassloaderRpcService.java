package cn.jmicro.classloader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.classloader.IClassloaderRpc;
import cn.jmicro.api.config.Config;

@Service(namespace="classloaderrpc",version="0.0.1",timeout=30000,showFront=false)
@Component
public class ClassloaderRpcService implements IClassloaderRpc {

	private final static Logger logger = LoggerFactory.getLogger(ClassloaderRpcService.class);
	
	@Cfg(value="/")
	private String jarResp;
	
	@Override
	public byte[] getClassData(String clazz) {
		
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
			
			logger.info("return class: {}, data length: {}", resName,clsData.length);
			
			return clsData;
			
		} catch (ClassNotFoundException e) {
			logger.warn(clazz);
		}catch (IOException e) {
			logger.error(clazz);
		}

		return null;
	}

	@Override
	public String info() {
		StringBuffer sb = new StringBuffer();
		sb.append("host:").append(Config.getExportSocketHost());
		sb.append(",instanceName:").append(Config.getInstanceName());
		return sb.toString();
	}

	
}