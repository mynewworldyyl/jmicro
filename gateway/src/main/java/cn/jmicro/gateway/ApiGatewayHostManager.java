package cn.jmicro.gateway;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.raft.IChildrenListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.Server;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;

@Component
public class ApiGatewayHostManager {

	public static final String HostDir = Config.getRaftBasePath("") + "/apiGateways";
	
	@Cfg(value="/nettyHttpPort",required=false,defGlobal=false)
	private int port=9090;
	
	@Cfg(value="/nettyPort",required=false,defGlobal=false)
	private int nettyPort=9092;
	
	@Inject
	private IDataOperator op;
	
	private AtomicInteger seq = new AtomicInteger(0);
	
	private AtomicInteger seq0 = new AtomicInteger(0);
	
	private List<String> httpHosts = new ArrayList<>();
	private List<String> socketHosts = new ArrayList<>();
	
	private IChildrenListener childrenListener = (type, parentParent, child, data) -> {
		if(type == IListener.ADD) {
			if(child.startsWith(Constants.TRANSPORT_NETTY_HTTP)) {
				httpHosts.add(child.substring(Constants.TRANSPORT_NETTY_HTTP.length()+1));
			}else {
				socketHosts.add(child.substring(Constants.TRANSPORT_NETTY.length()+1));
			}
		}else if(type == IListener.REMOVE){
			if(child.startsWith(Constants.TRANSPORT_NETTY_HTTP)) {
				httpHosts.remove(child.substring(Constants.TRANSPORT_NETTY_HTTP.length()+1));
			}else {
				socketHosts.remove(child.substring(Constants.TRANSPORT_NETTY.length()+1));
			}
		}
	};
	
	public void ready() {
		op.addChildrenListener(HostDir, childrenListener);
		regist(Config.getExportHttpHost(),port+"",Constants.TRANSPORT_NETTY_HTTP);
		regist(Config.getExportSocketHost(),nettyPort+"",Constants.TRANSPORT_NETTY);
	}
	
	public String bestHost(String protocol) {
		if(Constants.TRANSPORT_NETTY_HTTP.equals(protocol)) {
			if(httpHosts.size() == 0) {
				return null;
			}
			if(seq.get() >= httpHosts.size()) {
				seq.set(0);
				return httpHosts.get(0);
			} else {
				int idx = seq.incrementAndGet() % httpHosts.size();
				return httpHosts.get(idx);
			}
		}else {
			if(socketHosts.size() == 0) {
				return null;
			}
			if(seq0.get() >= socketHosts.size()) {
				seq0.set(0);
				return socketHosts.get(0);
			} else {
				int idx = seq0.incrementAndGet() % socketHosts.size();
				return socketHosts.get(idx);
			}
		}
		
	}

	public void regist(String host,String port,String protocol) {
		String path = HostDir + "/"+protocol +"#"+ host+"#" +port;
		if(op.exist(path)) {
			throw new CommonException("Api gateway host exist: " + path);
		}
		op.createNodeOrSetData(path, "", IDataOperator.EPHEMERAL);
	}
	
	public List<String> getHosts(String protocol) {
		if(Constants.TRANSPORT_NETTY_HTTP.equals(protocol)) {
			if(this.httpHosts.isEmpty()) {
				return Collections.EMPTY_LIST;
			} else {
				return Collections.unmodifiableList(this.httpHosts);
			}
		}else {
			if(this.socketHosts.isEmpty()) {
				return Collections.EMPTY_LIST;
			} else {
				return Collections.unmodifiableList(this.socketHosts);
			}
		}
	}
	
	
}
