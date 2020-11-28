/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.jmicro.api.net;

import java.util.HashMap;
import java.util.Map;

import cn.jmicro.api.AbstractObjectMapSupport;
import cn.jmicro.api.annotation.SO;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:07:23
 */
@SO
public final class RpcResponse /*extends AbstractObjectMapSupport*/ implements IResponse /*IEncoder,IResponse,*/{
	
	protected Map<String,Object> params = new HashMap<String,Object>();
	
	private transient Message msg;
	
	private Long reqId;
	
	private Object result;
	
	private boolean isMonitorEnable = false;
	
	private boolean success = true;
	
	
	public RpcResponse(long reqId,Object result){
		this.reqId = reqId;
		this.result = result;
	}
	
	public RpcResponse(long reqId){
		this.reqId = reqId;
	}
	
	public RpcResponse(){
	}
	
	/*@Override
	public void decode(ByteBuffer ois) {
		super.decode(ois);
		this.id = ois.getLong();
		this.reqId = ois.getLong();
		this.success = ois.get()==0?false:true;
		this.result = Decoder.decodeObject(ois);
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer bb = super.encode();
		bb.putLong(this.id);
		bb.putLong(this.reqId);
		bb.put(this.success?(byte)1:(byte)0);
		Encoder.encodeObject(bb, result);
		bb.flip();
		return bb;
	}

	@Override
	public ByteBuffer newBuffer() {
		return ByteBuffer.allocate(bufferSize);
	}*/

	public Message getMsg() {
		return msg;
	}

	public void setMsg(Message msg) {
		this.msg = msg;
	}

	public Long getRequestId() {
		return reqId;
	}

	public void setReqId(Long reqId) {
		this.reqId = reqId;
	}

	public boolean isMonitorEnable() {
		return isMonitorEnable;
	}

	public void setMonitorEnable(boolean isMonitorEnable) {
		this.isMonitorEnable = isMonitorEnable;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public Long getReqId() {
		return reqId;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getParam(String key,T defautl){
		T v = (T)this.params.get(key);
		if(v == null){
			return defautl;
		}
		return v;
	}
	
	/*public void decode(ByteBuffer ois) {
		//ByteBuffer ois = ByteBuffer.wrap(data);
		this.params.putAll(Decoder.decodeObject(ois));
	}
	
	public ByteBuffer encode() {
		ByteBuffer bb = newBuffer();
		Encoder.encodeObject(bb,this.params);
		return bb;
	}*/
	
	public Map<String,Object> getParams(){
		return this.params;
	}
	
	//public abstract ByteBuffer newBuffer();
	
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
	
	public void putObject(String key,Object value){
		 this.params.put(key, value);
	}
	
	@Override
	public String toString() {
		return "RpcResponse [reqId=" + reqId + ", result=" + result + ", isMonitorEnable="
				+ isMonitorEnable + ", success=" + success + "]";
	}

}
