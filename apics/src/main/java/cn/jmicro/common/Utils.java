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
package cn.jmicro.common;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import cn.jmicro.api.monitor.MC;
import cn.jmicro.common.util.JsonUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:09:00
 */
public class Utils {

	// 邮箱验证规则
	private String EMAIL_EX = "[a-zA-Z_]{0,}[0-9]{0,}@(([a-zA-z0-9]-*){1,}\\.){1,3}[a-zA-z\\-]{1,}";
	// 编译正则表达式
	private Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_EX);

	private DecimalFormat DECIMALFORMAT = new DecimalFormat("#.##");

	private static Utils ins = new Utils();

	private Utils() {
	}

	public static Utils getIns() {
		return ins;
	}

	public static boolean isEmpty(String str) {
		if (str == null || str.length() == 0)
			return true;
		return false;
	}
	
	public static boolean formSystemPackagePermission(int idx) {
		StackTraceElement se = Thread.currentThread().getStackTrace()[idx];
		String dcls = se.getClassName();
		for(String pname : Constants.SYSTEM_PCK_NAME_PREFIXES) {
			if(dcls.startsWith(pname)) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean callPathExistPackage(String pckPrefix) {
		StackTraceElement[] ses = Thread.currentThread().getStackTrace();
		for(StackTraceElement se:ses ) {
			
			if(se.getClassName().startsWith(pckPrefix)) {
				return true;
			}
		}
		return false;
	}

	public String toString(Object[] arr) {
		StringBuilder sb = new StringBuilder("[");
		for (Object o : arr) {
			if (o != null) {
				sb.append(o.toString()).append(",");
			}
		}
		return sb.substring(0, sb.length() - 1).toString() + "]";
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

	public void waitForShutdown() {
		synchronized (Utils.ins) {
			try {
				ins.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void shutdown() {
		synchronized (Utils.ins) {
			Utils.ins.notifyAll();
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
						if (ip.startsWith("127.")) {
							continue;
						}
						ipList.add(ip);
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		if (ipList.isEmpty()) {
			ipList.add("127.0.0.1");
		}
		return ipList;
	}

	public void getMethods(List<Method> methods, Class<?> clazz) {
		Method[] ms = clazz.getDeclaredMethods();
		for (Method f : ms) {
			if (Modifier.isStatic(f.getModifiers()) || f.getDeclaringClass() == Object.class) {
				continue;
			}
			methods.add(f);
		}

		if (clazz.getSuperclass() != Object.class) {
			getMethods(methods, clazz.getSuperclass());
		}
	}

	public void getFields(List<Field> fields, Class<?> clazz) {
		Field[] fs = clazz.getDeclaredFields();
		for (Field f : fs) {
			if (Modifier.isFinal(f.getModifiers()) || Modifier.isStatic(f.getModifiers())
					|| f.getDeclaringClass() == Object.class) {
				continue;
			}
			fields.add(f);
		}

		if (clazz.getSuperclass() != Object.class) {
			getFields(fields, clazz.getSuperclass());
		}
	}

	public void getFieldNames(List<String> fieldNames, Class cls) {
		if (cls == null) {
			return;
		}
		Field[] fs = cls.getDeclaredFields();
		for (Field f : fs) {
			if (Modifier.isTransient(f.getModifiers()) || Modifier.isFinal(f.getModifiers())
					|| Modifier.isStatic(f.getModifiers()) || f.getDeclaringClass() == Object.class) {
				continue;
			}
			fieldNames.add(f.getName());
		}

		if (cls.getSuperclass() != Object.class) {
			getFieldNames(fieldNames, cls.getSuperclass());
		}

	}

	public Field getClassField(Class<?> cls, String fn) {
		Field f = null;
		try {
			return cls.getDeclaredField(fn);
		} catch (NoSuchFieldException e) {
			cls = cls.getSuperclass();
			if (cls == Object.class) {
				return null;
			} else {
				return getClassField(cls, fn);
			}
		}
	}

	public Object getValue(Type type, String str, Type gt) {
		Class<?> cls = null;
		if (type instanceof Class) {
			cls = (Class) type;
		}
		Object v = null;
		if (type == Boolean.TYPE || type == Boolean.class) {
			v = Boolean.parseBoolean(str);
		} else if (type == Short.TYPE || type == Short.class) {
			v = Short.parseShort(str);
		} else if (type == Integer.TYPE || type == Integer.class) {
			v = Integer.parseInt(str);
		} else if (type == Long.TYPE || type == Long.class) {
			v = Long.parseLong(str);
		} else if (type == Float.TYPE || type == Float.class) {
			v = Float.parseFloat(str);
		} else if (type == Double.TYPE || type == Double.class) {
			v = Double.parseDouble(str);
		} else if (type == Byte.TYPE || type == Byte.class) {
			v = Byte.parseByte(str);
		} /*
			 * else if(type == Character.TYPE){ v = Character(str); }
			 */else if (cls != null && cls.isArray()) {
			Class<?> ctype = ((Class) type).getComponentType();
			String[] elts = str.split(",");
			Object arr = Array.newInstance(ctype, elts.length);
			int i = 0;
			for (int j = 0; j < elts.length; j++) {
				Object vv = this.getValue(ctype, elts[j], gt);
				Array.set(arr, j, vv);

			}
			v = arr;
		} else if (cls != null && List.class.isAssignableFrom(cls)) {
			Class<?> ctype = ((Class) type).getComponentType();
			String[] elts = str.split(",");
			List list = new ArrayList();
			for (String e : elts) {
				list.add(getValue(gt, e, gt));
			}
			v = list;
		} else if (cls != null && Collection.class.isAssignableFrom(cls)) {
			String[] elts = str.split(",");
			Set set = new HashSet();
			for (String e : elts) {
				set.add(getValue(gt, e, null));
			}
			v = set;
		} else if (cls != null && Map.class.isAssignableFrom(cls)) {
			String[] elts = str.split("&");
			Map<String, String> map = new HashMap<>();
			for (String e : elts) {
				String[] kv = e.split("=");
				map.put(kv[0], kv[1]);
			}
			v = map;
		} else if (cls == String.class) {
			v = str;
		} else {
			v = JsonUtils.getIns().fromJson(str, type);
		}

		return v;
	}

	public boolean checkEmail(String str) {
		return EMAIL_PATTERN.matcher(str).find();
	}

	public String defaultVal(Class<?> cl) {
		if (cl.isPrimitive()) {
			if (Boolean.TYPE == cl)
				return "false";
			if (Byte.TYPE == cl)
				return "0";
			if (Character.TYPE == cl)
				return "' '";
			if (Double.TYPE == cl)
				return "0D";
			if (Float.TYPE == cl)
				return "0F";
			if (Integer.TYPE == cl)
				return "0";
			if (Long.TYPE == cl)
				return "0";
			if (Short.TYPE == cl)
				return "0";
			throw new RuntimeException(cl.getName() + " is unknown primitive type.");
		}
		return "null";
	}

	public String bestDataSizeVal(double val) {
		String unit = "";
		Double retVal = 0D;
		if (val < KB) {
			unit = "B";
			retVal = val;
		} else if (val < MB) {
			unit = "KB";
			retVal = val / KB;
		} else if (val < GB) {
			unit = "MB";
			retVal = val / MB;
		} else if (val < TB) {
			unit = "GB";
			retVal = val / GB;
		} else if (val < PB) {
			unit = "TB";
			retVal = val / TB;
		} else {
			unit = "PB";
			retVal = val / PB;
		}
		return DECIMALFORMAT.format(retVal) + unit;
	}

	private final double B = 1;

	private final double KB = 1024;

	private final double MB = KB * 1024;

	private final double GB = MB * 1024;

	private final double TB = GB * 1024;

	private final double PB = TB * 1024;

}
