package org.jmicro.api.cache;

/**
 * 
 * 
 * @author Yulei Ye
 * @date 2020年1月11日
 */
@FunctionalInterface
public interface ICacheRefresher {

	Object get(String key);
}
