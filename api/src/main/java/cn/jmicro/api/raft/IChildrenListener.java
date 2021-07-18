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

import cn.jmicro.api.IListener;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月5日-下午6:33:03
 */
public interface IChildrenListener extends IListener{

	/**
	 * 
	 * @param type 子结点增加或删除
	 * @param parent 父路径，全路径
	 * @param child 子结点名称，不包括父路径
	 * @param data  子结点数据
	 */
	void childrenChanged(int type,String parent,String child/*,String data*/);
}
