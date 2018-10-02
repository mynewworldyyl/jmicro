package org.jmicro.api.registry;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.dubbo.common.utils.StringUtils;
import org.jmicro.api.AbstractRpcProtocolMessage;
import org.jmicro.api.codec.Decoder;
import org.jmicro.api.codec.Encoder;
import org.jmicro.api.exception.CommonException;
import org.jmicro.common.Utils;

public class ServiceItem extends AbstractRpcProtocolMessage {

	public static final String ROOT="/jmicro/service";
	
	public static final String FILE_SEPERATOR="/";
	
	public static final String I_I_SEPERATOR="####";
	
	public static final String KV_SEPERATOR="=";
	public static final String VAL_SEPERATOR="&";
	
	private String host;
	
	private int port;
	
	protected Map<String,String> params = new HashMap<String,String>();
	
	public ServiceItem() {}
	
	public ServiceItem(String key,String val) {
		this.parseKey(key);
		this.parseVal(val);
	}
	
	@Override
	public void decode(ByteBuffer ois) {
	    Map<Object,Object> objs = Decoder.decodeObject(ois);
	    for(Map.Entry<Object, Object> e : objs.entrySet()){
	    	params.put((String)e.getKey(),(String)e.getValue());
	    }
	}

	@Override
	public void encode(ByteBuffer oos) {
		Encoder.encodeObject(oos, params);
	}
	
	public static String serviceName(String key) {
	    int i = key.indexOf(I_I_SEPERATOR);
		return key.substring(0, i);
	}

	private void parseVal(String val) {
		if(StringUtils.isEmpty(val)){
			return;
		}
		val = Utils.getIns().decode(val);
		String[] kvs = val.split(VAL_SEPERATOR);
		for(String kv : kvs){
			String[] vs = kv.split(KV_SEPERATOR);
			if(vs.length != 2) {
				throw new CommonException("ServerItem value invalid: "+kv);
			}
			switch(vs[0]) {
			case "host":
				this.host=vs[1];
				break;
			case "port":
				this.port=Integer.parseInt(vs[1]);
				break;
			case "group":
				this.namespace=vs[1];
				break;
			case "version":
				this.version=vs[1];
				break;
			default:
				this.params.put(vs[0], vs[1]);
			}
		}
	}

	private void parseKey(String key) {
		String[] tmp = null;
		if(key.startsWith(ROOT)) {
			key = key.substring(ROOT.length()+1);
		}
		int i = key.indexOf(I_I_SEPERATOR);
		
		this.serviceName = key.substring(0, i);
		
		key = key.substring(i+I_I_SEPERATOR.length());
		key = Utils.getIns().decode(key);
		tmp = key.split(VAL_SEPERATOR);
		
		for(String v : tmp){
			String[] kv = v.split(KV_SEPERATOR);
			if(kv.length != 2) {
				throw new CommonException("ServerItem KEY invalid: "+kv);
			}
			switch(kv[0]){
			/*case "serviceName":
				this.serviceName = kv[1];
				break;*/
			case "host":
				this.host = kv[1];
			break;
			case "port":
				this.port = Integer.parseInt(kv[1]);
			break;
			case "version":
				this.version = kv[1];
			break;
			case "namespace":
				this.namespace = kv[1];
			break;
			}
		}
		//this.serviceName = tmp[0];
		//this.impl = tmp[1];
				
	}

	public String key0(){
		StringBuffer sb = new StringBuffer(ROOT);
		if(!this.serviceName.startsWith(FILE_SEPERATOR)){
			sb.append(FILE_SEPERATOR);
		}
		sb.append(serviceName).append(I_I_SEPERATOR);
		
		StringBuffer val = new StringBuffer();
		
		val.append("host").append(KV_SEPERATOR).append(host).append(VAL_SEPERATOR)
		.append("port").append(KV_SEPERATOR).append(port).append(VAL_SEPERATOR)
		.append("namespace").append(KV_SEPERATOR).append(this.namespace).append(VAL_SEPERATOR)
		.append("version").append(KV_SEPERATOR).append(this.version);

		return sb.append(Utils.getIns().encode(val.toString())).toString();
	}
	
	public String key(){
		StringBuffer sb = new StringBuffer(ROOT);
		if(!this.serviceName.startsWith(FILE_SEPERATOR)){
			sb.append(FILE_SEPERATOR);
		}
		sb.append(serviceName).append(I_I_SEPERATOR);
		
		StringBuffer val = new StringBuffer();
		
		val.append("host").append(KV_SEPERATOR).append(host).append(VAL_SEPERATOR)
		.append("port").append(KV_SEPERATOR).append(port).append(VAL_SEPERATOR)
		.append("namespace").append(KV_SEPERATOR).append(this.namespace).append(VAL_SEPERATOR)
		.append("version").append(KV_SEPERATOR).append(this.version).append(VAL_SEPERATOR)
		.append("time").append(KV_SEPERATOR).append(System.currentTimeMillis());

		return sb.append(Utils.getIns().encode(val.toString())).toString();
	}
	
	public String val(){
		/*StringBuffer sb = new StringBuffer("host=").append(this.host).append(VAL_SEPERATOR);
		sb.append("port=").append(this.port).append(VAL_SEPERATOR)
		.append("port=").append(this.port).append(VAL_SEPERATOR)
		.append("group=").append(this.namespace).append(VAL_SEPERATOR)
		//.append("impl=").append(this.impl).append(VAL_SEPERATOR)
		.append("version=").append(this.version);*/
		StringBuffer sb = new StringBuffer();
		if(!this.params.isEmpty()) {
			for(Map.Entry<String, String> e: this.params.entrySet()){
				if(sb.length() > 0) {
					sb.append(VAL_SEPERATOR);
				}
				sb.append(e.getKey()).append(KV_SEPERATOR).append(e.getValue());
			}
		}
		return Utils.getIns().encode(sb.toString());
	}

	@Override
	public int hashCode() {
		return this.key().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof ServiceItem)) {
			return false;
		}
		return this.key0().equals(((ServiceItem)obj).key());
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}
	
	
}
