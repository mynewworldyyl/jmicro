package cn.jmicro.gateway.log;

import java.util.Set;

import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.OneLogJRso;
import cn.jmicro.api.monitor.genclient.IOutterMonitorService$Gateway$JMAsyncClient;
import cn.jmicro.gateway.client.ApiGatewayClient;

public class LogManager {

	private static final ThreadLocal<LogContext> cxt = new ThreadLocal<>();
	
	private static final Object locker = new Object();
	
	private static IOutterMonitorService$Gateway$JMAsyncClient ms = null;
	
	public static final boolean log(byte level,String tag,String content,Throwable ex) {
		if(level <= MC.LOG_NO || level > MC.LOG_FINAL) {
			return false;
		}
		LogContext c = getCxt();
		if(level < c.getLevel()) {
			return false;
		}
		c.addOne(level, tag, content, ex);
		return true;
	}
	
	public static final boolean log(byte level,String tag,String content) {
		return log(level,tag,content,null);
	}
	
	public static final boolean submit() {
		LogContext c = getCxt();
		Set<OneLogJRso> logs = c.getLogs();
		if(logs == null || logs.isEmpty()) {
			return false;
		}
		
		if(ms == null) {
			synchronized(locker) {
				if(ms == null) {
					ms = ApiGatewayClient.getClient().getService(IOutterMonitorService$Gateway$JMAsyncClient.class,
							"monitorServer", "0.0.1");
				}
			}
		}
		
		ms.submitJMAsync(logs)
		.fail((code,msg,cxt)->{
			System.out.println("code=" + code+", msg="+msg);
		});
		
		return true;
	}
	
	public static final void setLevel(byte level) {
		LogContext c = getCxt();
		c.setLevel(level);
	}
	
	private static LogContext getCxt() {
		LogContext c = cxt.get();
		if(c != null) {
			return c;
		}
		
		synchronized(locker) {
			 c = cxt.get();
			 if(c == null) {
				 c = new LogContext();
				 cxt.set(c);
			 }
		}
		return c;
	}
	
}
