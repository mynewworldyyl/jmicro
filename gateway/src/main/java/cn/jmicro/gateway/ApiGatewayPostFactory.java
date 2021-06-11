package cn.jmicro.gateway;

import java.util.Set;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.gateway.MessageRouteRow;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.net.IServer;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.objectfactory.PostFactoryAdapter;
import cn.jmicro.api.objectfactory.ProxyObject;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;

@Component
public class ApiGatewayPostFactory extends PostFactoryAdapter {

	@Override
	public void afterInit(IObjectFactory of) {
		Config cfg = of.get(Config.class);
		if(cfg.getBoolean(MessageRouteRow.API_MODEL, MessageRouteRow.API_MODEL_PRE)) return;
		
		IDataOperator op = of.get(IDataOperator.class);
		String keys = cfg.getString(MessageRouteRow.MSG_ROUTE_KEYS, "");
		if(Utils.isEmpty(keys)) {
			keys = Constants.MSG_TYPE_REQ_RAW+"";
		}
		
		ComponentIdServer idGenerator = of.get(ComponentIdServer.class);
		
		String host =  Config.getExportSocketHost();
		String port = null;
		
		Set<IServer> ss = of.getByParent(IServer.class);
		for(IServer s : ss){
			cn.jmicro.api.annotation.Server sano = ProxyObject.getTargetCls(s.getClass())
					.getAnnotation(cn.jmicro.api.annotation.Server.class);
			if(Constants.TRANSPORT_NETTY.equals(sano.transport())) {
				port = s.port();
				break;
			}
		}
		
		String[] ks = keys.split(",");
		for(String k : ks) {
			MessageRouteRow r = new MessageRouteRow();
			r.setBackendType(MessageRouteRow.TYPE_GATEWAY);
			r.setKey(k);
			r.setId(idGenerator.getIntId(MessageRouteRow.class));
			r.setIp(host);
			r.setPort(port);
			
			String path = MessageRouteRow.MSG_ROUTE_KEYS + "/" + r.getId();
			op.createNodeOrSetData(path, JsonUtils.getIns().toJson(r), true);
			
		}
		
	}

}
