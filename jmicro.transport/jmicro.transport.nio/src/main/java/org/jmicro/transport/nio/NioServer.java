package org.jmicro.transport.nio;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Server;
import org.jmicro.api.server.IServer;
import org.jmicro.common.Constants;
import org.jmicro.common.JMicroContext;

@Server()
public class NioServer implements IServer {

	private Map<String,Object> handlers = new ConcurrentHashMap<String,Object>(); 
	
	@Inject(required=true)
	private NioTcpServer server = null;
	
	private String host = null;
	
	private int port;
	
	@Override
	public void init() {
		
	}

	@Override
	public void start() {
		
		JMicroContext cxt = JMicroContext.get();
		this.host = JMicroContext.getCfg().getBindIp()+":"+JMicroContext.getCfg().getPort();
		server.bind(new InetSocketAddress(JMicroContext.getCfg().getBindIp(),JMicroContext.getCfg().getPort()));
		
	}

	@Override
	public void stop() {
		
	}

	@Override
	public void addService(Object srv) {
		handlers.put(srv.getClass().getName(), srv);
	}

	@Override
	public String host() {
		return this.host.toString();
	}

	@Override
	public int port() {
		// TODO Auto-generated method stub
		return this.port;
	}

}
