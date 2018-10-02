package org.jmicro.api;

import java.nio.ByteBuffer;

import org.jmicro.api.codec.Decoder;
import org.jmicro.api.codec.Encoder;
import org.jmicro.api.codec.IDecodable;
import org.jmicro.api.codec.IEncodable;

public abstract class AbstractRpcProtocolMessage extends AbstractObjectMapSupport implements IEncodable,IDecodable{

	protected String serviceName;
	
	protected String method;
	
	protected Object[] args;
	
	protected String namespace;
	
	protected String version;
	
	@Override
	public void decode(ByteBuffer ois) {
		this.version = Decoder.decodeObject(ois);
		this.serviceName = Decoder.decodeObject(ois);
		this.method = Decoder.decodeObject(ois);
		this.namespace = Decoder.decodeObject(ois);
		this.args = Decoder.decodeObject(ois);// (Object[])ois.readObject();
	}

	@Override
	public void encode(ByteBuffer oos) {
		Encoder.encodeObject(oos, this.version);
		Encoder.encodeObject(oos, this.serviceName);
		Encoder.encodeObject(oos, this.method);
		Encoder.encodeObject(oos, this.namespace);
		Encoder.encodeObject(oos, this.args);
	}
	
	public void setVersion(String version){
		this.version=version;
	}
	
	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getVersion() {
		return version;
	}

	public void Namespace(String version) {
		this.version = version;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

}
