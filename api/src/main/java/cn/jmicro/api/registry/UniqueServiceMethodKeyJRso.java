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
package cn.jmicro.api.registry;

import com.alibaba.dubbo.common.serialize.kryo.utils.ReflectUtils;

import cn.jmicro.common.CommonException;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.HashUtils;
import cn.jmicro.common.util.JsonUtils;
import lombok.Data;
import lombok.Serial;

/**
 * 在服务标识基础上加上方法签名
 *  {@link ServiceItemJRso}
 * @author Yulei Ye
 * @date 2018年12月2日 下午11:22:50
 */
@Serial
@Data
public final class UniqueServiceMethodKeyJRso {

	public static final String SEP = UniqueServiceKeyJRso.SEP;
	public static final String PSEP = ",";
	
	private UniqueServiceKeyJRso usk = new UniqueServiceKeyJRso();
	
	private String method;
	private String paramsStr;
	
	private String returnParam;
	
	private int snvHash;
	
	private transient String cacheFullKey = null;
	
	private transient String cacheMethodKey = null;
	
	public Class<?>[] getParameterClasses() {
		return paramsClazzes(this.paramsStr);
	}
	
	public Class<?> getReturnParamClass() {
		Class<?>[] rs = paramsClazzes(this.returnParam);
		if(rs == null || rs.length == 0) {
			return null;
		} else {
			return rs[0];
		}
	}
	
	public void form(UniqueServiceMethodKeyJRso k) {
		this.method = k.method;
		this.paramsStr = k.paramsStr;
		this.snvHash = k.snvHash;
		this.usk.form(k.usk);
	}
	
	/*
	 public static String paramsStr(String[] clazzes) {
		if(clazzes == null || clazzes.length == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		int offset = clazzes.length - 1;
		for(int i = 0; i < offset; i++){
		    sb.append(clazzes[i]).append(PSEP);
		}
		sb.append(clazzes[offset]);
		return sb.toString();
	 }
	*/
	
	public static String paramsStr(Class<?>[] args) {
		return ReflectUtils.getDesc(args);
	}
	
	public static String paramsStr(Object[] args) {
		if(args == null || args.length == 0) {
			return "";
		}
		
		Class<?>[] cs = paramsClazzes(args);
		
		return paramsStr(cs);
	}
	
	/**
	 *     根据参数类型串解析出参数对像数组
	 * @param paramsStr 参数类型数符串
	 * @param argStr 参数字符串
	 * @return
	 */
	public static Class<?>[] paramsClazzes(Object[] args) {
		Class<?>[] cs = new Class<?>[args.length];
		for(int i = 0; i < cs.length; i++){
			if(args[i] != null) {
				cs[i] = args[i].getClass();
			}else {
				cs[i] = Void.class;
			}
		}
		return cs;
	}
	
	public static Class<?>[] paramsClazzes(String paramDesc) {
		try {
			if(Utils.isEmpty(paramDesc)) {
				return new Class[0];
			}
			return ReflectUtils.desc2classArray(paramDesc);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * 根据参数类型串解析出参数对像数组
	 * @param paramsStr 参数类型数符串
	 * @param argStr 参数字符串
	 * @return
	 */
/*	public static Object[] paramsArg(String paramsStr,String argStr) {
		Class<?>[] clazzes = paramsClazzes(paramsStr);
		
		return null;
	}*/
	
	public String toSnvm() {
		StringBuilder sb = new StringBuilder();
		sb.append(usk.toSnv());
		sb.append(SEP).append(this.method);
		//sb.append(SEP.append(""/*this.paramsStr*/);
		return sb.toString();
	}
	
	public String fullStringKey() {
		if(this.cacheFullKey != null) return this.cacheFullKey;
		else return toKey(true,true,true,true,true,true,true);
	}
	
	/*public String serviceID() {
		if(this.cacheFullKey != null) return this.cacheFullKey;
		else return toKey(false,false,false,false,false,true,false);//建者和权限唯一标识一个服务
	}*/
	
   String toKey(boolean ins,boolean insId,boolean host,boolean port,
			boolean act,boolean actId,boolean clientId) {
		if(this.cacheFullKey != null && ins && host && port) {
			return cacheFullKey;
		} else if(this.cacheMethodKey != null && !ins && !host && !port) {
			return this.cacheMethodKey;
		} else {
			StringBuilder sb = new StringBuilder(usk.toKey(ins,insId, host, port,act,actId,clientId));
			sb.append(SEP).append(this.method);
			//sb.append(SEP).append(""/*this.paramsStr==null ? "":this.paramsStr*/);
			String r = sb.toString();
			if(ins && host && port && insId && act && actId && clientId) {
				cacheFullKey = r;
			}else if(!ins && !host && !port && !insId && !act && !actId && clientId){
				cacheMethodKey = r;
			}
			return r;
		}
	}
	
	public static UniqueServiceKeyJRso fromKey(String[] strs) {
		if(strs.length < UniqueServiceKeyJRso.INDEX_LEN ) {
			throw new CommonException("Invalid unique service method key: " + JsonUtils.getIns().toJson(strs));
		}
		UniqueServiceKeyJRso usk = new UniqueServiceKeyJRso();
		
		int idx = UniqueServiceKeyJRso.INDEX_SN;
		
		usk.setServiceName(strs[idx]);
		usk.setNamespace(strs[++idx]); 
		usk.setVersion(strs[++idx]);
		
		usk.setInstanceName(strs[++idx]);
		usk.setInsId(Integer.parseInt(strs[++idx]));
		
		usk.setHost(strs[++idx]);
		usk.setPort(strs[++idx]);
	
		usk.setActName(strs[++idx]);
		usk.setCreatedBy(Integer.parseInt(strs[++idx]));
		usk.setClientId(Integer.parseInt(strs[++idx]));
		
		usk.setSnvHash(HashUtils.FNVHash1(usk.serviceID()));
		
		return usk;
	}
	
	public static UniqueServiceMethodKeyJRso fromKey(String key) {
		String[] strs = key.split(SEP);
		if(strs.length < 11 ) {
			throw new CommonException("Invalid unique service method key: " + key);
		}
		
		UniqueServiceMethodKeyJRso usk = new UniqueServiceMethodKeyJRso();
		UniqueServiceKeyJRso srvUsk = fromKey(strs);
		usk.setUsk(srvUsk);
		
		usk.setMethod(strs[UniqueServiceKeyJRso.INDEX_METHOD]);
		
		if(strs.length > UniqueServiceKeyJRso.INDEX_METHOD_PARAMS) {
			usk.setParamsStr(strs[UniqueServiceKeyJRso.INDEX_METHOD_PARAMS]);
		}
		
		usk.setSnvHash(HashUtils.FNVHash1(usk.methodID()));
		
		return usk;
	}
	
	/**
	 * 服务方法逻辑标识，由发者及权限标识
	 * @return
	 */
	public String methodID() {
		return this.toKey(false, false, false,false, false, false,true);
	}
	
	public String getServiceName() {
		return this.getUsk().getServiceName();
	}
	public void setServiceName(String serviceName) {
		this.getUsk().setServiceName(serviceName);
	}
	public String getNamespace() {
		return this.getUsk().getNamespace();
	}
	public void setNamespace(String namespace) {
		this.getUsk().setNamespace(namespace);
	}
	public String getVersion() {
		return this.getUsk().getVersion();
	}
	public void setVersion(String version) {
		this.getUsk().setVersion(version);
	}
	public String getInstanceName() {
		return this.getUsk().getInstanceName();
	}
	public void setInstanceName(String instanceName) {
		this.getUsk().setInstanceName(instanceName);
	}
	public String getHost() {
		return this.getUsk().getHost();
	}
	public void setHost(String host) {
		this.getUsk().setHost(host);
	}
	public String getPort() {
		return this.getUsk().getPort();
	}
	
	public void setPort(String port) {
		this.getUsk().setPort(port);
	}
	
	/*public String toString() {
		return toKey(true,true,true,true,true,true,true);
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}*/

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof UniqueServiceMethodKeyJRso)) {
			return false;
		}
		return hashCode() == obj.hashCode();
	}

	@Override
	protected UniqueServiceMethodKeyJRso clone() throws CloneNotSupportedException {
		return (UniqueServiceMethodKeyJRso) super.clone();
	}

}
