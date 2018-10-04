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
package org.jmicro.common;

import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:09:00
 */
public class Utils {

	private static Utils ins = new Utils();

	private Utils() {
	}

	public static Utils getIns() {
		return ins;
	}

	public void setClasses(Set<Class<?>> clses, Map<String, Class<?>> classMap) {
		Iterator<Class<?>> ite = clses.iterator();
		while (ite.hasNext()) {
			Class<?> c = ite.next();
			String key = c.getName();
			classMap.put(key, c);
		}
	}

	public String encode(String value) {
		if (value == null || value.length() == 0) {
			return "";
		}
		try {
			return URLEncoder.encode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public String decode(String value) {
		if (value == null || value.length() == 0) {
			return "";
		}
		try {
			return URLDecoder.decode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	public static void waitForShutdown(){
		 synchronized(Utils.ins){
			 try {
				 ins.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		 }
	}
	
	public List<String> getLocalIPList() {
        List<String> ipList = new ArrayList<String>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            NetworkInterface networkInterface;
            Enumeration<InetAddress> inetAddresses;
            InetAddress inetAddress;
            String ip;
            while (networkInterfaces.hasMoreElements()) {
                networkInterface = networkInterfaces.nextElement();
                inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    inetAddress = inetAddresses.nextElement();
                    if (inetAddress != null && inetAddress instanceof Inet4Address) { // IPV4
                        ip = inetAddress.getHostAddress();
                        if(ip.startsWith("127.")){
                        	continue;
                        }
                        ipList.add(ip);
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return ipList;
    }

}
