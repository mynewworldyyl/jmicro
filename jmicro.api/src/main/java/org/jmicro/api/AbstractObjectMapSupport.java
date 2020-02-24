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
package org.jmicro.api;

import java.util.HashMap;
import java.util.Map;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-上午11:54:29
 */
public abstract class AbstractObjectMapSupport /*implements IEncoder,IDecoder*/{

	protected Map<String,Object> params = new HashMap<String,Object>();
	
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


}
