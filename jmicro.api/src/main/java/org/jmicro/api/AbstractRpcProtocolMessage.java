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

import java.nio.ByteBuffer;

import org.jmicro.api.codec.Decoder;
import org.jmicro.api.codec.Encoder;
import org.jmicro.api.codec.IDecodable;
import org.jmicro.api.codec.IEncodable;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-上午11:54:37
 */
public abstract class AbstractRpcProtocolMessage extends AbstractObjectMapSupport implements IEncodable,IDecodable{

	protected String serviceName;
	
	protected String method;
	
	protected Object[] args;
	
	protected String namespace;
	
	protected String version;
	
	@Override
	public void decode(ByteBuffer ois) {
		super.decode(ois);
		this.version = Decoder.decodeObject(ois);
		this.serviceName = Decoder.decodeObject(ois);
		this.method = Decoder.decodeObject(ois);
		this.namespace = Decoder.decodeObject(ois);
		this.args = Decoder.decodeObject(ois);// (Object[])ois.readObject();
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer oos = super.encode();
		Encoder.encodeObject(oos, this.version);
		Encoder.encodeObject(oos, this.serviceName);
		Encoder.encodeObject(oos, this.method);
		Encoder.encodeObject(oos, this.namespace);
		Encoder.encodeObject(oos, this.args);
		return oos;
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
