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
package org.jmicro.api.raft;

import java.util.Set;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月5日-下午6:43:31
 */
public interface IDataOperator {

	void addListener(IConnectionStateChangeListener lis);
	
	void addDataListener(String path,IDataListener lis);
	
	void addChildrenListener(String path,IChildrenListener lis);
	
	void addNodeListener(String path,INodeListener lis);
	
	void removeNodeListener(String path,INodeListener lis);
	
	void removeDataListener(String path,IDataListener lis);
	
	void removeChildrenListener(String path,IChildrenListener lis);
	
	boolean exist(String path);
	 
	String getData(String path);
	
	void setData(String path,String data);
	
	Set<String> getChildren(String path);
	
	void createNode(String path,String data,boolean elp);
	
	void deleteNode(String path);
	
	void init();

}
