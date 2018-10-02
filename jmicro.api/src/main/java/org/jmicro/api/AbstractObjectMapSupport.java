package org.jmicro.api;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.jmicro.api.codec.Decoder;
import org.jmicro.api.codec.Encoder;
import org.jmicro.api.codec.IDecodable;
import org.jmicro.api.codec.IEncodable;

public abstract class AbstractObjectMapSupport implements IEncodable,IDecodable{

	protected Map<String,Object> params = new HashMap<String,Object>();
	
	@SuppressWarnings("unchecked")
	public <T> T getParam(String key,T defautl){
		T v = (T)this.params.get(key);
		if(v == null){
			return defautl;
		}
		return v;
	}
	
	public void decode(byte[] data) {
		ByteBuffer ois = ByteBuffer.wrap(data);
		this.decode(ois);
		this.params.putAll(Decoder.decodeObject(ois));
	}
	
	public byte[] encode() {
		ByteBuffer bb = ByteBuffer.allocate(1024*4);
		this.encode(bb);
		Encoder.encodeObject(bb,this.params);
		bb.flip();
		byte[] data = new byte[bb.limit()];
		bb.get(data);
		return data;
	}

	public abstract void decode(ByteBuffer ois);
	public abstract void encode(ByteBuffer oos);
	
	public Integer getInt(String key,int defautl){
		return this.getParam(key,defautl);
	}
	
	public String getString(String key,String defautl){
		return this.getParam(key,defautl);
	}
	
	public Boolean getBoolean(String key,boolean defautl){
		return this.getParam(key,defautl);
	}
	
	
	public Float getFloat(String key,Float defautl){
		return this.getParam(key,defautl);
	}
	
	public Double getDouble(String key,Double defautl){
		return this.getParam(key,defautl);
	}
	
	public Object getObject(String key,Object defautl){
		return this.getParam(key,defautl);
	}


}
