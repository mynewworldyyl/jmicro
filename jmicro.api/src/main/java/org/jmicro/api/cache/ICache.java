package org.jmicro.api.cache;

/**
 * 
 * 
 * @author Yulei Ye
 * @date 2020年1月11日
 */
public interface ICache {

	boolean put(String key,Object val);
	
	boolean del(String key);
	
	<T> T get(String key);
	
	/**
	 * 
	 * @param key
	 * @param val
	 * @param expire timeout with millisecond
	 * @return
	 */
	<T> boolean put(String key,T val,long expire);
	
	/**
	 *  超时时间 = expire + randomVal, randomVal是一个限机数最大值，真实生成的值在0到randomVal之间
	 *  设置数据超时时间为一个指定区间的值，以防止缓存雪崩效应
	 * @param key
	 * @param val
	 * @param expire timeout with millisecond
	 * @param randomVal max random value with millisecond
	 * @return
	 */
	<T> boolean put(String key,T val,long expire,int randomVal);
	
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
}
