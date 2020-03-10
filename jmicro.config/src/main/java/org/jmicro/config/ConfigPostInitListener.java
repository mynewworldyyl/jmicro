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
package org.jmicro.config;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.zookeeper.KeeperException;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.PostListener;
import org.jmicro.api.config.Config;
import org.jmicro.api.config.IConfigChangeListener;
import org.jmicro.api.objectfactory.PostInitAdapter;
import org.jmicro.api.objectfactory.ProxyObject;
import org.jmicro.common.CommonException;
import org.jmicro.common.Utils;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:10:26
 */
@PostListener(true)
public class ConfigPostInitListener extends PostInitAdapter {

	private final static Logger logger = LoggerFactory.getLogger(ConfigPostInitListener.class);
	
	public ConfigPostInitListener() {
	}
	
	/**
	 * 1。命令行参数中查找，如果找不到，进入2
	 * 2。优先在服务配置中查找配置，如果找不到，进入3
	 * 3。在META-INF/jmicro/*.properties中查找
	 * 4。在全局配置中查找，如果找不到，进入4
	 * 5。在环境系统环境变量中找，如果没找到，返回NULL
	 * 6。从对象中找默认值，如果找到则进入6，如果没找到，判断required值如果是true报错，否则不处理返回
	 * 7。判断defGlobal=true，将值配置到全局配置中，否则配置到局部配置中
	 */
	@Override
	public void preInit(Object obj,Config cfg) {
		
		 Class<?> cls = ProxyObject.getTargetCls(obj.getClass());
		 List<Field> fields = new ArrayList<>();
		 Utils.getIns().getFields(fields, cls);
		 
		if(cls.getName().equals("org.jmicro.main.monitor.Log2DbMonitor")) {
			 logger.debug("preInit");
		 }

		 for(Field f : fields){
			if(!f.isAnnotationPresent(Cfg.class)){
				//不是配置类字段
				continue;
			}
			
			Cfg cfgAnno = f.getAnnotation(Cfg.class);
			if(StringUtils.isBlank(cfgAnno.value())){
				//配置路径不能为NULL或空格
				throw new CommonException("Class ["+cls.getName()+",Field:"+f.getName()+"],Cfg path is NULL");
			}
			
			String prefix = cfgAnno.value();
			if(!prefix.startsWith("/")){
				prefix = "/"+prefix;
			}
			
			String path = null;
					
			if(Map.class.isAssignableFrom(f.getType())) {
				if(!prefix.endsWith("*")) {
					throw new CommonException("Class ["+cls.getName()+",Field:"+f.getName()+"] invalid map path ["+cfgAnno.value()+"] should end with '*'") ;
				}
				Map<String,String> ps = new HashMap<>();
				//符合条件的全部值都要监听，只要有增加进来，都加到Map里面去
				
				//优先类全名组成路径
				path = "/" + cls.getName() + prefix;
				getMapConfig(cfg,path,f,ps);
				watch(f,obj,path,cfg);
				
				path = "/" + cls.getSimpleName() + prefix;
				getMapConfig(cfg,path,f,ps);
				watch(f,obj,path,cfg);
				
				path = prefix;
				getMapConfig(cfg,path,f,ps);
				watch(f,obj,path,cfg);
				
				if(!ps.isEmpty()) {
					setMapValue(f,obj,ps);
				}
				
			} else if(Collection.class.isAssignableFrom(f.getType())) {
				if(!prefix.endsWith("*")) {
					throw new CommonException("Class ["+cls.getName()+",Field:"+f.getName()+"] invalid map path ["+cfgAnno.value()+"] should end with '*'") ;
				}
				//符合条件的全部值都要监听，只要有增加进来，都加到Map里面去
				
				//优先类全名组成路径
				Collection coll = new ArrayList();
				path = "/" + cls.getName() + prefix;
				getCollectionConfig(cfg,path,coll);
				watch(f,obj,path,cfg);
				
				path = "/" + cls.getSimpleName() + prefix;
				getCollectionConfig(cfg,path,coll);
				watch(f,obj,path,cfg);
				
				path = prefix;
				getCollectionConfig(cfg,path,coll);
				watch(f,obj,path,cfg);
				
				if(!coll.isEmpty()) {
					 setCollValue(f,obj,coll);
				}
				
			} else {
				String value = null;
				//优先类全名组成路径
				path = "/" + cls.getName() + prefix;
				
				value = getValueFromConfig(cfg,path,f);
				if(StringUtils.isEmpty(value)) {
					//类简称组成路径
					path = "/" + cls.getSimpleName() + prefix;
					value = getValueFromConfig(cfg,path,f);
				}
				
				if(StringUtils.isEmpty(value)){
					//值直接指定绝对路径
					path = prefix;
					value = getValueFromConfig(cfg,path,f);
				}
				
				if(!StringUtils.isEmpty(value)){
					 setValue(f,obj,value);
				} else {
					Object v = getFieldValue(f,obj);
					path = cfgAnno.value();
					if(v == null ) {
						if(cfgAnno.required()) {
							throw new CommonException("Class ["+cls.getName()+",Field:"+f.getName()+"] value: "+cfgAnno.value()+" is required");
						}
					} else {
						cfg.createConfig(v.toString(), path, cfgAnno.defGlobal());
					}
				}
				watch(f,obj,path,cfg);
			}
		}
	}
	
	private void setCollValue(Field f, Object obj, Collection<String> newColl) {
		Collection oldColl = (Collection)this.getFieldValue(f, obj);
		if(oldColl == null) {
			if(Set.class.isAssignableFrom(f.getType())) {
				oldColl = new HashSet();
			}else if(List.class.isAssignableFrom(f.getType())) {
				oldColl = new ArrayList();
			}else {
				throw new CommonException("Class ["+obj.getClass().getName()+",Field:"+f.getName()+"] not support field type["+f.getType().getName()+"]") ;
			}
		}
		
		ParameterizedType genericType = (ParameterizedType) f.getGenericType();
		if(genericType == null){
			throw new CommonException("Must be ParameterizedType for cls:"+ f.getDeclaringClass().getName()+",field: "+f.getName());
		}
		Class<?> valType = (Class<?>)genericType.getActualTypeArguments()[0];
		
		for(String strv : newColl) {
			Object v = Utils.getIns().getValue(valType, strv, null);
			if(v != null) {
				oldColl.add(v);
			}
		}
		
		setObjectVal(obj,f,oldColl);
	}

	private void setMapValue(Field f, Object obj, Map<String, String> ps) {
		if(ps == null || ps.isEmpty()) {
			return ;
		}
		
		ParameterizedType genericType = (ParameterizedType) f.getGenericType();
		if(genericType == null){
			throw new CommonException("Must be ParameterizedType for cls:"+ f.getDeclaringClass().getName()+",field: "+f.getName());
		}
		Class<?> keyType = (Class<?>)genericType.getActualTypeArguments()[0];
		if(keyType != String.class) {
			throw new CommonException("Map config key only support String as key");
		}
		
		Class<?> valueType = (Class<?>)genericType.getActualTypeArguments()[1];
		
		Map map = (Map) getFieldValue(f,obj);
		
		if(map == null){
			map = new HashMap();
		}
		
		for(Map.Entry<String, String> e: ps.entrySet()) {
			Object v = Utils.getIns().getValue(valueType,e.getValue(),null);
			map.put(e.getKey(), v);
		}
		
		setObjectVal(obj,f,map);
		
	}
	
	private void setObjectVal(Object obj,Field f,Object srv) {

		String setMethodName = "set"+f.getName().substring(0, 1).toUpperCase()+f.getName().substring(1);
		Method m = null;
		try {
			 m = obj.getClass().getMethod(setMethodName, f.getType());
			 m.invoke(obj, srv);
		} catch (InvocationTargetException | NoSuchMethodException e1) {
		    boolean bf = f.isAccessible();
			if(!bf) {
				f.setAccessible(true);
			}
			try {
				f.set(obj, srv);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new CommonException("",e);
			}
			if(!bf) {
				f.setAccessible(bf);
			} 
		}catch(SecurityException | IllegalAccessException | IllegalArgumentException e1){
			throw new CommonException("Class ["+obj.getClass().getName()+"] field ["+ f.getName()+"] dependency ["+f.getType().getName()+"] error",e1);
		}
	
	}

	private void getMapConfig(Config cfg,String key,Field f,Map<String,String> params) {
		Map<String,String> result = cfg.getParamByPattern(key);
		if(result != null && !result.isEmpty()) {
			params.putAll(result);
		}
	}
	
	private void getCollectionConfig(Config cfg,String key,Collection l) {
		Map<String,String> result = cfg.getParamByPattern(key);
		if(result != null && !result.isEmpty()) {
			l.addAll(result.values());
		}
	}
	
	private String getValueFromConfig(Config cfg,String key,Field f) {
		//boolean isGlobal = f.getAnnotation(Cfg.class).defGlobal();
		String val = Config.getCommandParam(key, String.class, null);
		if(!StringUtils.isEmpty(val)) {
			//在配置中心中建立配置，以便能动态修改，在系统 关闭后，配置会自动删除，以使下次还从命令行读取初始值
			logger.info("class:{} Field:{} Config from command args:{}={}",f.getDeclaringClass().getName(),f.getName(),key,val);
			cfg.createConfig(val, key, false,true);
			return val;
		}
		
		val = cfg.getServiceParam(key, String.class, null);
		if(!StringUtils.isEmpty(val)) {
			return val;
		}
		
		val = cfg.getGlobalParam(key, String.class, null);
		if(!StringUtils.isEmpty(val)) {
			return val;
		}
		
		val = Config.getExtParam(key);
		if(!StringUtils.isEmpty(val)) {
			//在配置中心中建立配置，以便能动态修改，在系统 关闭后，配置会自动删除，以使下次还从配置文件读取初始值
			logger.info("class:{} Field:{} Config from extension:{}={}",f.getDeclaringClass().getName(),f.getName(),key,val);
			cfg.createConfig(val, key, false,true);
			return val;
		}
		
		 val = Config.getEnvParam(key);
		if(!StringUtils.isEmpty(val)) {
			//在配置中心中建立配置，以便能动态修改，在系统 关闭后，配置会自动删除，以使下次还从命令行读取初始值
			logger.info("class:{} Field:{} Config from system env:{}={}",f.getDeclaringClass().getName(),f.getName(),key,val);
			cfg.createConfig(val, key, false,true);
			return val;
		}
		return null;
	
		
	}

	private Object getFieldValue(Field f, Object obj) {
		boolean flag = f.isAccessible();
		if(!flag) {
			f.setAccessible(true);
		}
		Object v = null;
		try {
			v = f.get(obj);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			logger.error("",e);
		}
		if(!flag) {
			f.setAccessible(false);
		}
		
		if(v != null && f.getType().isArray()) {
			Object[] vas = (Object[])v;
			if(vas.length == 0) {
				return "";
			}
			
			StringBuffer sb = new StringBuffer();
			for(int i = 0; i < vas.length; i++) {
				sb.append(vas[i].toString());
				if(i < vas.length) {
					sb.append(",");
				}
			}
			v = sb.toString();
		}
		
		return v;
	}

	private void setValue(Field f,Object obj,String value){
        try {

			String getName = "set"+f.getName().substring(0,1).toUpperCase()+f.getName().substring(1);
			Method m=null;
			try {
				m = obj.getClass().getMethod(getName, new Class[]{f.getType()});
			} catch (NoSuchMethodException e) {
			}
			
			Cfg cfg = f.getAnnotation(Cfg.class);
			
			Object v = null;
			if(value != null) {
				v = Utils.getIns().getValue(f.getType(),value,f.getGenericType());
			}
			 
			if(v == null){
				if(cfg.required()){
					throw new CommonException("Class ["+obj.getClass().getName()+",Field:"+f.getName()+"] is required");
				}
				return;
			}
			
			if(m != null){
				 m.invoke(obj,v);
			}else {
				if(!f.isAccessible()){
					f.setAccessible(true);
				}
				f.set(obj, v);
			}
		
        } catch (Exception e) {
        	String msg = "Class ["+obj.getClass().getName()+",Field:"+f.getName()+"] set value= " + value + " error";
			if(e instanceof KeeperException.NoNodeException){
				logger.warn(msg,e);
			}else {
				throw new CommonException(msg,e);
			}
		}
	}
	
	private void watch(Field f,Object obj,String path,Config cfg){
		IConfigChangeListener lis = (String path1,String data)->{
			if(StringUtils.isEmpty(data)) {
				return ;
			}
			logger.debug("Config changed key:"+path+"="+data);
			if(Map.class.isAssignableFrom(f.getType())) {
				Map<String,String> ps = new HashMap<>();
				ps.put(path1, data);
				this.setMapValue(f, obj, ps);
			} if(Collection.class.isAssignableFrom(f.getType())){
				
			}else {
				setValue(f,obj,data);
			}
			notifyChange(f,obj);
		};
		cfg.addConfigListener(path, lis);
	}

	protected void notifyChange(Field f,Object obj) {
		Cfg cfg = f.getAnnotation(Cfg.class);
		if(cfg == null || cfg.changeListener()== null || cfg.changeListener().trim().equals("")){
			return;
		}
		Method m =  null;
		try {
			 m =  ProxyObject.getTargetCls(obj.getClass())
					.getMethod(cfg.changeListener(),new Class[]{String.class} );
			 if(m != null){
				 m.invoke(obj,f.getName());
			 }
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			 try {
				m =  ProxyObject.getTargetCls(obj.getClass())
							.getMethod(cfg.changeListener(),new Class[0] );
				if(m != null){
					 m.invoke(obj);
				}
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
			}
		}
		
	}

}
