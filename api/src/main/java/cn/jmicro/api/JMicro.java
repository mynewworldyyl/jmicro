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
package cn.jmicro.api;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.impl.StaticLoggerBinder;

import cn.jmicro.api.classloader.RpcClassLoader;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月17日-上午10:21:03
 */
public class JMicro {

	public static void main(String[] args) {
		System.out.println(Arrays.asList(args));
		getObjectFactoryAndStart(args);
		// JMicro.getObjectFactoryAndStart(args);
		// waitForShutdown();
	}

	public static final void waitForShutdown() {
		try {
			Thread.sleep(Long.MAX_VALUE);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		;
	}

	public static Object getObjectFactoryAndStart(String[] args) {
		try {
			boolean isLogInit = false;
			for (String arg : args) {
				if (arg.startsWith("-D")) {
					String ar = arg.substring(2);
					if (ar == null || "".equals(ar.trim())) {
						continue;
					}

					ar = ar.trim();
					String key = "";
					String val = "";

					if (ar.indexOf("=") > 0) {
						String[] ars = ar.split("=");
						key = ars[0].trim();
						val = ars[1].trim();
					}

					if (key != null && "log4j.configuration".equals(key)) {
						System.out.println(val);
						if (val != null && !"".equals(val.trim())) {
							isLogInit = true;
							if (val.endsWith("xml")) {
								DOMConfigurator.configure(val);
							} else {
								PropertyConfigurator.configure(val);
							}
						}
						break;
					}
				}
			}

			if(!isLogInit) {
				StaticLoggerBinder.getSingleton();
			}

			ClassLoader cl = null;
			ClassLoader parent = JMicro.class.getClassLoader();
			if (!parent.getClass().getName().equals(RpcClassLoader.class.getName())) {
				parent = Thread.currentThread().getContextClassLoader();
				if (!parent.getClass().getName().equals(RpcClassLoader.class.getName())) {
					cl = new RpcClassLoader(JMicro.class.getClassLoader());
				}
			}
			
			if(cl == null) cl = parent;
			
			Thread.currentThread().setContextClassLoader(cl);
			
			/*Class<?> cls = cl.loadClass("cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder");
			Class<?> itemFromRpcClassLoader = cl.loadClass("cn.jmicro.api.registry.ServiceItemJRso");
			Class<?> itemFromParent = parent.loadClass("cn.jmicro.api.registry.ServiceItemJRso");
			parent.loadClass("cn.jmicro.api.registry.ServiceItemJRso");
			
			System.out.println(cls.getName());*/
			
			Class<?> emClass = cl.loadClass("cn.jmicro.api.EnterMain");
			Object em = emClass.newInstance();
			Method m = emClass.getMethod("getObjectFactoryAndStart", new String[0].getClass());
			Object obj = args;
			Object ret = m.invoke(em, obj);
			return ret;
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException
				| SecurityException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			/*
			 * if(e instanceof InvocationTargetException && ((InvocationTargetException)
			 * e).getTargetException().toString().endsWith(
			 * "liquibase/servicelocator/PackageScanClassResolver")) { return null; }else {
			 * e.printStackTrace(); }
			 */
		}
		return null;
	}

}
