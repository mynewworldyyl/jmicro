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
package cn.jmicro.api.raft;

import java.util.Set;

import cn.jmicro.api.objectfactory.IObjectFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月5日-下午6:43:31
 */
public interface IDataOperator {
	  
    public static final int PERSISTENT = 0;
   
    public static final int PERSISTENT_SEQUENTIAL = 2;
   
    public static final int EPHEMERAL= 1;
   
    public static final int EPHEMERAL_SEQUENTIAL = 3;
    
	void addListener(IConnectionStateChangeListener lis);
	
	void removeListener(IConnectionStateChangeListener lis);
	
	void addDataListener(String path,IDataListener lis);
	
	void addChildrenListener(String path,IChildrenListener lis);
	
	void addNodeListener(String path,INodeListener lis);
	
	void removeNodeListener(String path,INodeListener lis);
	
	void removeDataListener(String path,IDataListener lis);
	
	void removeChildrenListener(String path,IChildrenListener lis);
	
	boolean exist(String path);
	 
	String getData(String path);
	
	void setData(String path,String data);
	
	Set<String> getChildren(String path,boolean fromCache);
	
	void createNodeOrSetData(String path,String data,boolean elp);
	
	void createNodeOrSetData(String path,String data,int model);
	
	void deleteNode(String path);
	
	void init0();
	
	void objectFactoryStarted(IObjectFactory of);

}
