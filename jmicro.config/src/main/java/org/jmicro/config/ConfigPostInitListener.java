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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.jmicro.api.Config;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.PostListener;
import org.jmicro.api.exception.CommonException;
import org.jmicro.api.objectfactory.PostInitListenerAdapter;
import org.jmicro.api.objectfactory.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:10:26
 */
@PostListener(true)
public class ConfigPostInitListener extends PostInitListenerAdapter {

	private final static Logger logger = LoggerFactory.getLogger(ConfigPostInitListener.class);
	
	public ConfigPostInitListener() {
	}
	
	@Override
	public void preInit(Object obj) {
		Class<?> cls = ProxyObject.getTargetCls(obj.getClass());
		Field[] fields = cls.getDeclaredFields();
		
		
		for(Field f : fields){
			if(!f.isAnnotationPresent(Cfg.class)){
				continue;
			}
			Cfg cfg = f.getAnnotation(Cfg.class);
			if(StringUtils.isEmpty(cfg.value())){
				throw new CommonException("Class ["+cls.getName()+",Field:"+f.getName()+"],Cfg path is NULL");
			}

			String path = Config.getConfigRoot();
			if(!StringUtils.isEmpty(cfg.root())){
				path = cfg.root();
			}
			if(!cfg.value().startsWith("/")){
				path = path + "/";
			}

			path = path + cfg.value();
	       
	        setValue(f,obj,path);
	        watch(f,obj,path);
		}
		
	}
	
	private void setValue(Field f,Object obj,String path){
        try {
        	 GetDataBuilder getDataBuilder = ZKCon.getIns().getCurator().getData();
			byte[] data = getDataBuilder.forPath(path);
			
			String getName = "set"+f.getName().substring(0,1).toUpperCase()+f.getName().substring(1);
			Method m=null;
			try {
				m = obj.getClass().getMethod(getName, new Class[]{f.getType()});
			} catch (NoSuchMethodException e) {
			}
			
			Cfg cfg = f.getAnnotation(Cfg.class);
			Object v = getValue(f.getType(),new String(data, "UTF-8"),f.getGenericType());
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
			if(e instanceof KeeperException.NoNodeException){
				logger.warn("Path not found:"+path);
			}else {
				throw new CommonException("Class ["+obj.getClass().getName()+",Field:"+f.getName()+"] set value error",e);
			}
			
		}
	}
	
	private void watch(Field f,Object obj,String path){
		GetDataBuilder getDataBuilder = ZKCon.getIns().getCurator().getData();
		try {
			getDataBuilder.forPath(path);
			getDataBuilder.usingWatcher(new Watcher() {
	            @Override
	            public void process(WatchedEvent event) {
	              logger.info("Watcher for '{}' received watched event: {}", path, event);
	              if (event.getType() == EventType.NodeDataChanged) {
	            	  try {
						setValue(f,obj,path);
						watch(f,obj,path);
						notifyChange(f,obj);
					} catch (Exception e) {
						throw new CommonException("Class ["+obj.getClass().getName()+",Field:"+f.getName()+"],Cfg path is NULL");
					}
	              }
	            }
	          });
		} catch (Exception e1) {
			if(e1 instanceof KeeperException.NoNodeException){
				logger.warn("Path not found:"+path);
			}else {
				logger.warn("Path not found:"+e1);
			}
		}
		
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

	private Object getValue(Type type, String str,Type gt) {
		Class<?> cls = null;
		if(type instanceof Class){
			cls = (Class)type;
		}
		Object v = null;
		if(type == Boolean.TYPE){
			v = Boolean.parseBoolean(str);
		}else if(type == Short.TYPE){
			v = Short.parseShort(str);
		}else if(type == Integer.TYPE){
			v = Integer.parseInt(str);
		}else if(type == Long.TYPE){
			v = Long.parseLong(str);
		}else if(type == Float.TYPE){
			v = Float.parseFloat(str);
		}else if(type == Double.TYPE){
			v = Double.parseDouble(str);
		}else if(type == Byte.TYPE){
			v = Byte.parseByte(str);
		}/*else if(type == Character.TYPE){
			v = Character(str);
		}*/else if(cls != null && cls.isArray()) {
			Class<?> ctype = ((Class)type).getComponentType();
			String[] elts = str.split(",");
			Object[] oos = new Object[elts.length];
			int i =0;
			for(String e: elts ){
				oos[i++] = this.getValue(ctype, e,gt);
			}
			v = oos;
		}else if(cls != null && List.class.isAssignableFrom(cls)){
			Class<?> ctype = ((Class)type).getComponentType();
			String[] elts = str.split(",");
			List list = new ArrayList();
			for(String e: elts ){
				list.add(getValue(gt,e,gt));
			}
			v = list;
		}else if(cls != null && Collection.class.isAssignableFrom(cls)){
			String[] elts = str.split(",");
			Set set = new HashSet();
			for(String e: elts ){
				set.add(getValue(gt,e,null));
			}
			v = set;
		}else if(cls != null && Map.class.isAssignableFrom(cls)){
			String[] elts = str.split("&");
			Map<String,String> map = new HashMap<>();
			for(String e: elts ){
				String[] kv = e.split("=");
				map.put(kv[0], kv[1]);
			}
			v = map;
		}else {
			v = str;
		}
		
		return v;
	}

	
}
