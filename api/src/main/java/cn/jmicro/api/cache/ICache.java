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
package cn.jmicro.api.cache;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import cn.jmicro.common.Constants;

/**
 * 
 * 
 * @author Yulei Ye
 * @date 2020年1月11日
 */
public interface ICache {

	public static byte[] keyData(String key) {
		byte[] k = null;
		try {
			k = key.getBytes(Constants.CHARSET);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
		return k;
	}
	
	<T> boolean put(String key, T val);
	
	boolean del(String key);
	
	<T> T get(String key, Class<T> type);
	
	boolean exist(String key);
	
	/**
	 * 
	 * @param key
	 * @param val
	 * @param expire timeout with millisecond
	 * @return
	 */
	<T> boolean put(String key,T val,long expire);
	
	<T> boolean setNx(String key,T val);
	
	/**
	 * 对当前KEY下的值加val,相加后如果结果大于或等于0，则将结果作为新值并返回结果值，否则返回-1,结果不变。
	 * 此操作必须保证原子性
	 * @param key
	 * @param val 可以是负数，表示减法，如果是正数，表示加法
	 * @return
	 */
	int increcement(String key,int val);
	
	
	/**
	 *  超时时间 = expire + randomVal, randomVal是一个限机数最大值，真实生成的值在0到randomVal之间
	 *  设置数据超时时间为一个指定区间的值，以防止缓存雪崩效应
	 * @param key
	 * @param val
	 * @param expire timeout with millisecond
	 * @param randomVal max random value with millisecond
	 * @return
	 */
	<T> boolean put(String key, T val, long expire, int randomVal);
	
	/**
	 * 
	 * @param key
	 * @param expire timeout with millisecond
	 * @return
	 */
	boolean expire(String key,long expire);
	
	/**
	  *  超时时间 = expire + randomVal, randomVal是一个随机数最大值，真实生成的值在0到randomVal之间
	 *  设置数据超时时间为一个指定区间的值，以防止缓存雪崩效应
	 * @param key
	 * @param expire timeout with millisecond
	 * @return
	 */
	boolean expire(String key,long expire,int randomVal);
	
	/**
	 * 
	 * @param key
	 * @param refresher
	 */
	boolean configRefresh(String key,long timerWithMilliseconds, long expire, int randomVal);
	
	/**
	 * 
	 * @param key
	 * @param ref
	 */
	void setReflesher(String key, ICacheRefresher ref);
	
	
	/*********************Map start**************************/
	boolean hput(String key, Map<String,Object> hdata);
	
	boolean hdel(String key, String fname);
	
	<T> T hget(String key, String fname, Class<T> type);
	
	boolean hexist(String key, String fname);

	<T> boolean hput(String key, String fname, T val);
	
	/**********************Map end*************************/
	
}
