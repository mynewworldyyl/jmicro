package cn.jmicro.gateway.log;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.OneLogJRso;
import cn.jmicro.common.Constants;

public class LogContext {

	private byte level = MC.LOG_NO;
	
	private Set<OneLogJRso> logs = new HashSet<>();
	
	public void addOne(byte level,String tag,String content,Throwable ex) {
		OneLogJRso lo = new OneLogJRso(level,tag,content);
		if(ex != null) {
			lo.setEx(serialEx(ex));
		}
		logs.add(lo);
	}
	
	public static String serialEx(Throwable ex) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ex.printStackTrace(new PrintStream(baos,true,Constants.CHARSET));
			return baos.toString(Constants.CHARSET);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return ex.getMessage();
	}

	public byte getLevel() {
		return level;
	}

	public void setLevel(byte level) {
		this.level = level;
	}

	public Set<OneLogJRso> getLogs() {
		return logs;
	}
	
}
