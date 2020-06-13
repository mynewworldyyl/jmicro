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
package cn.jmicro.api.monitor;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月28日 下午12:50:36
 */
public interface IServiceCounter<T> {

	void stop();
	
	void start();
	/**
	 *  取指定类型的时间窗口内的统计总数
	 * @param type
	 * @return
	 */
	long get(T type);
	
	boolean add(T type,long val);
	
	/**
	 * 计数器启动以来的总数
	 * @param type
	 * @return
	 */
	Long getTotal(T... type);
	
	boolean increment(T type);
	
	boolean addCounter(T type);	
	
	long getByTypes(Short...types);
	
	boolean existType(Short type);
	
}
