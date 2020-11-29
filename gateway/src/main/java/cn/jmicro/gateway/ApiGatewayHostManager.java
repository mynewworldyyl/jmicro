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
import cn.jmicro.common.CommonException;

@Component
public class ApiGatewayHostManager {

	public static final String HostDir = Config.BASE_DIR + "/apiGateways";
	
	@Cfg(value="/nettyHttpPort",required=false,defGlobal=false)
	private int port=9090;
	
	@Inject
	private IDataOperator op;
	
	private AtomicInteger seq = new AtomicInteger(0);
	
	private List<String> hosts = new ArrayList<>();
	
	private IChildrenListener childrenListener = (type, parentParent, child, data) -> {
		if(type == IListener.ADD) {
			hosts.add(child);
		}else if(type == IListener.REMOVE){
			hosts.remove(child);
		}
	};
	
	public void ready() {
		op.addChildrenListener(HostDir, childrenListener);
		regist(Config.getExportHttpHost(),port+"");
	}
	
	public String bestHost() {
		if(hosts.size() == 0) {
			return null;
		}
		if(seq.get() >= hosts.size()) {
			seq.set(0);
			return hosts.get(0);
		} else {
			int idx = seq.incrementAndGet() % hosts.size();
			return hosts.get(idx);
		}
	}

	public void regist(String host,String port) {
		String path = HostDir + "/" + host+":" +port;
		if(op.exist(path)) {
			throw new CommonException("Api gateway host exist: " + path);
		}
		op.createNodeOrSetData(path, "", IDataOperator.EPHEMERAL);
	}
	
	public List<String> getHosts() {
		if(this.hosts.isEmpty()) {
			return Collections.EMPTY_LIST;
		} else {
			return Collections.unmodifiableList(this.hosts);
		}
	}
}
