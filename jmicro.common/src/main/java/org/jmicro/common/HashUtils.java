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


/**
 * 
 * @author Yulei Ye
 * @date 2018年12月3日 下午11:13:38
 */
public class HashUtils {

	private static final int SEED1 = 16777619;
	private static final int SEED2 =(int)2166136261L;
	
	private HashUtils() {}
	
	/**
	 *  改进的32位FNV算法1
	 * 
	 * @param data
	 *            数组
	 * @return int值
	 */
	public static int FNVHash1(String msg) {
		byte[] data = msg.getBytes();
		final int p = SEED1;
		int hash = SEED2;
		for (byte b : data)
			hash = (hash ^ b) * p;
		hash += hash << 13;
		hash ^= hash >> 7;
		hash += hash << 3;
		hash ^= hash >> 17;
		hash += hash << 5;
		return hash;
	}


}
