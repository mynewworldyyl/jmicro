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

import java.util.HashSet;
import java.util.Set;

import cn.jmicro.api.config.Config;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.StringUtils;
import lombok.Data;
import lombok.Serial;

/**
 * 服务标识 
 * {@link ServiceItemJRso}
 * @author Yulei Ye
 * @date 2018年12月2日 下午11:22:38
 */
@Serial
@Data
public final class UniqueServiceKeyJRso {
	
	public static final int INDEX_LEN = 10;
	
	public static final int INDEX_SN = 0;
	public static final int INDEX_NS = 1;
	public static final int INDEX_VER = 2;
	
	public static final int INDEX_INS = 3;
	public static final int INDEX_INS_ID = 4;
	
	public static final int INDEX_HOST = 5;
	public static final int INDEX_PORT = 6;
	
	public static final int INDEX_ACT = 7;
	public static final int INDEX_ACT_ID = 8;
	public static final int INDEX_CLIENT_ID = 9;
	
	public static final int INDEX_METHOD = 10;
	
	public static final int INDEX_METHOD_PARAMS = 11;

	public static final String SEP = "##";
	
	private String instanceName;
	private String host;
	private String port;
	
	private String serviceName;
	private String namespace = null;
	private String version = Constants.VERSION;
	
	private int snvHash;
	
	private int clientId = 0;//权限范围
	private String actName;//创建者账号名
	private int createdBy = 0;//创建者的UserId
	private int insId  = 0;//实例ID
	
	//private int insHash;
	
	private transient String cacheFullKey = null;
	private transient String cacheSrvKey = null;
	
	private transient String snvKey = null;
	
	//缓存本服务的全部方法Hash
	private transient Set<Integer> methodHash = new HashSet<>();
	
	public UniqueServiceKeyJRso() {}
	
	public UniqueServiceKeyJRso(String serviceName,String namespace,String version) {
		this.serviceName = serviceName;
		this.namespace = namespace;
		this.version = version;
	}
	
	public void form(UniqueServiceKeyJRso k) {
		this.host = k.host;
		this.instanceName = k.instanceName;
		this.namespace = k.namespace;
		this.port = k.port;
		this.serviceName = k.serviceName;
		this.version = k.version;
		this.snvHash = k.snvHash;
		
		this.actName = k.actName;
		this.clientId = k.clientId;
		this.insId = k.insId;
		this.createdBy = k.createdBy;
		
	}
	
	public String path(String root){
		StringBuffer sb = new StringBuffer(root);
		sb.append(ServiceItemJRso.FILE_SEPERATOR);
		sb.append(fullStringKey());
		return sb.toString();
	}
	
	public String toSnv() {
		if(this.snvKey != null) {
			return snvKey;
		}else {
			this.snvKey = serviceName(this.serviceName,this.namespace,this.version);
			return this.snvKey;
		}
	}
	
	public String fullStringKey() {
		if(this.cacheFullKey != null) return this.cacheFullKey;
		else return toKey(true,true,true,true,true,true,true);
	}
	
	public String serviceID() {
		if(this.cacheFullKey != null) return this.cacheFullKey;
		else return toKey(false,false,false,false,false,false,true);//建者和权限唯一标识一个服务
	}
	
	 String toKey(boolean ins,boolean insId,boolean host,boolean port,
			boolean act,boolean createdBy,boolean clientId) {
		
		if(this.cacheFullKey != null && ins && insId && host && port && act && createdBy && clientId ) {
			return this.cacheFullKey;
		}else if(this.cacheSrvKey != null && !ins  && !insId && !host && !port && !act && !createdBy && clientId) {
			return this.cacheSrvKey;
		}
		
		if(port && this.port == null) {
			throw new CommonException("Port is not set yet");
		}
		
		if(host && this.host == null) {
			throw new CommonException("Host is not set yet");
		}
		
		StringBuilder sb = new StringBuilder();
		serviceName(sb,this.serviceName);
		namespace(sb,this.namespace);
		version(sb,this.version);
		
		instanceName(ins,sb,this.instanceName);
		insId(insId,sb,this.insId+"");
		host(host,sb,this.host);
		port(port,sb,this.port);
		
		appendOne(act,sb,this.actName);
		appendOne(createdBy,sb,this.createdBy+"");
		appendOne(clientId,sb,this.clientId+"");
		 
		/*actName(act,sb,this.actName);
		createdBy(createdBy,sb,this.createdBy);
		clientId(clientId,sb,this.clientId);*/

		String key = sb.substring(0, sb.length() - SEP.length());
		if(ins && host && port) {
			return this.cacheFullKey = key;
		}else if( !ins && !host && !port) {
			return this.cacheSrvKey = key;
		}else {
			return key;
		}
		
	}
	
	private void insId(boolean insId2, StringBuilder sb, String insId3) {
		 appendOne(insId2,sb,insId3);
	}

	public static UniqueServiceKeyJRso fromKey(String key) {
		String[] strs = key.split(SEP);
		if(strs.length < UniqueServiceKeyJRso.INDEX_LEN ) {
			throw new CommonException("Invalid unique service method key: " + key);
		}
		UniqueServiceKeyJRso usk = UniqueServiceMethodKeyJRso.fromKey(strs);
		return usk;
	}
	
	public static StringBuilder appendOne(boolean flag,StringBuilder sb,String val) {
		if(flag) {
			sb.append(val==null?"":val);
		}
		sb.append(SEP);
		return sb;
	}
	
	public static StringBuilder instanceName(boolean ins,StringBuilder sb,String instanceName) {
		return appendOne(ins,sb,instanceName);
	}
	
	public static StringBuilder host(boolean ins,StringBuilder sb,String host) {
		return appendOne(ins,sb,host);
	}
	
	public static StringBuilder port(boolean ins,StringBuilder sb,String port) {
		if(StringUtils.isEmpty(port)) {
			return appendOne(ins,sb,null);
		}
		return appendOne(ins,sb,port);
	}
	
	public static StringBuilder namespace(StringBuilder sb,String namespace){
		return appendOne(true,sb,namespace(namespace));
	}
	
	public static String namespace(String namespace){
		if(StringUtils.isEmpty(namespace)){
			namespace = Config.getNamespace();
		}
		return namespace;
	}
	
	public static String version(String version){
		if(StringUtils.isEmpty(version)){
			version = Constants.VERSION;
		}
		return version;
	}
	
	public static StringBuilder version(StringBuilder sb,String version){
		if(version == null || "".equals(version)){
			version = Constants.VERSION;
		}
		return appendOne(true,sb,version);
	}
	
	public static StringBuilder serviceName(StringBuilder sb,String serviceName){
		if(StringUtils.isEmpty(serviceName)) {
			throw new CommonException("Service name cannot be null");
		}
		return appendOne(true,sb,serviceName);
	}
	
	public static String serviceName(String sn, String ns, String v) {
		StringBuilder snv = version(snnsPrefix(sn,ns),v);
		snv.delete(snv.length()-2, snv.length());
		return snv.toString();
	}
	
	public static StringBuilder snnsPrefix(String sn, String ns) {
		StringBuilder sb = new StringBuilder();
		serviceName(sb,sn);
		namespace(sb,ns);
		return sb;
	}
	
	/**
	 *   1.0.0
	 *   
	 *   1.0.0 < v < 2.0.3
	 *   1.0.0 < v
	 *   v < 2.0.3
	 *  
	 *   1.0.0 <= v <= 2.0.3
	 *   1.0.0 <= v
	 *   v <= 2.0.3
	 *   
	 *   x.*.*
	 *   *
	 *   *.x.*
	 *   
	 *   *.*.*
	 *   
	 * @param macher
	 * @param version
	 * @return
	 */
	public static boolean matchVersion(String macher, String version) {
		boolean result = false;
		if(StringUtils.isEmpty(macher) || version.equals(macher)) {
			return true;
		}
		
		if(StringUtils.isEmpty(version)) {
			return macher.equals("*");
		}
		
		if(macher.indexOf("<=") > 0) {
			String[] arr = macher.split("<=");
			if(arr.length == 3) {
				result= compare(arr[0],version)<=0 && compare(version,arr[1])<=0;
			}else if(arr.length == 2) {
				if(arr[0].indexOf(".") > 0) {
					result=  compare(arr[0],version)<=0;
				}else if(arr[1].indexOf(".") > 0) {
					result=  compare(version,arr[1])<=0;
				}
			}
		}else if(macher.indexOf("<") > 0) {
			String[] arr = macher.split("<");
			if(arr.length == 3) {
				result= compare(arr[0],version)<0 && compare(version,arr[1])<0;
			}else if(arr.length == 2) {
				if(arr[0].indexOf(".") > 0) {
					result=  compare(arr[0],version)<0;
				}else if(arr[1].indexOf(".") > 0) {
					result=  compare(version,arr[1])<0;
				}
			}
		}else if(macher.indexOf("*") >= 0) {
			if("*".equals(macher.trim())) {
				result = true;
			} else {
				result = true;
				String[] arr = macher.split(".");
				String[] varr = version.split(".");
				for(int i=0; i < arr.length; i++) {
					if(!arr[i].equals("*") && !arr[i].equals(varr[i])) {
						result = false;
						break;
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * 处理以*号结尾匹配
	 * @param macher
	 * @param namespace
	 * @return
	 */
	public static boolean matchNamespace(String macher, String namespace) {
		if(StringUtils.isEmpty(macher)) {
			return false;
		}
		
		if(StringUtils.isEmpty(namespace)) {
			return  macher.equals("*");
		}
		
		if(macher.endsWith("*") && macher.length() > 1) {
			macher = macher.substring(0,macher.length()-1);
			if( namespace == null) {
				return false;
			}else {
				return namespace.startsWith(macher);
			}
		} else if(macher.trim().equals("*")) {
			//单*号完全匹配模式
			return true;
		} else {
			//无*号匹配模式
			return macher.equals(namespace);
		}
	}
	
	public static int compare(String first, String second) {
		return first.compareTo(second);
	}
	
	/*public String toString() {
		return this.fullStringKey();
	}
	
	@Override
	public int hashCode() {
		return this.fullStringKey().hashCode();
	}*/

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof UniqueServiceKeyJRso)) {
			return false;
		}
		return hashCode() == obj.hashCode();
	}

	@Override
	protected UniqueServiceKeyJRso clone() throws CloneNotSupportedException {
		return (UniqueServiceKeyJRso) super.clone();
	}


}
