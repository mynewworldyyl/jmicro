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
package cn.jmicro.common.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年12月3日 下午11:13:38
 */
public class HashUtils {

	private static final long FNV_64_INIT = 0xcbf29ce484222325L;
	private static final long FNV_64_PRIME = 0x100000001b3L;

	public static final int FNV_32_INIT = 0x811c9dc5;
	public static final int FNV_32_PRIME = 0x01000193;

	private HashUtils() {}
	
	public static int mcode(String sn,String namespace,String version,String method,Integer cid) {
		return FNVHash1(mkey(sn,namespace,version,method,cid));
	}
	
	public static String mkey(String sn,String namespace,String version,String method,Integer cid) {
		return sn + "##" + namespace + "##" + version + "##############" + cid + "##" + method;
	}
	
	public static int argHash(Object[] args) {
		int h = 0;//无参数或参数都为空时
		if(args != null && args.length > 0) {
			for(Object a : args) {
				if(a != null) {
					//只有非空字段才参数hash
					h ^=  a.hashCode();
					h *= FNV_32_PRIME;
				}
			}
		}
		return h;
	}
	
	public static int hash32(final ByteBuffer data) {
		return hash32(data.array());
	}
	
	public static long hash64(final ByteBuffer data) {
		return hash64(data.array());
	}

	public static int hash32(final byte[] data) {
		
		/*int hash = FNV_32_INIT;
		//int hash = 0;
		for (int i = 0; i < data.length; i++) {
            hash = hash ^ data[i];
            hash = hash + (hash << 1) + (hash << 4) + (hash << 7) + (hash << 8) + (hash << 24);
        }
        return hash;*/
		
		int rv = FNV_32_INIT;
		final int len = data.length;
		for (int i = 0; i < len; i++) {
			rv = rv ^ data[i];
			rv = rv * FNV_32_PRIME;
		}
		return rv;
	}
	
	public static int FNVHash1(final String k) {
		try {
			byte[] data = k.getBytes(Constants.CHARSET);
			return hash32(data);
		} catch (UnsupportedEncodingException e) {
			throw new CommonException(k,e);
		}
	}

	public static long hash64(final byte[] k) {
		long rv = FNV_64_INIT;
		final int len = k.length;
		for (int i = 0; i < len; i++) {
			rv ^= k[i];
			rv *= FNV_64_PRIME;
		}
		return rv;
	}

	public static long hash64(final String k) {
		try {
			byte[] data = k.getBytes(Constants.CHARSET);
			return hash32(data);
		} catch (UnsupportedEncodingException e) {
			throw new CommonException(k,e);
		}
	}

}
