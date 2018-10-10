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
package org.jmicro.config;

import java.util.List;
import java.util.Map;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.config.Config;
import org.jmicro.api.config.IConfigLoader;
import org.jmicro.api.raft.IDataListener;
import org.jmicro.common.util.StringUtils;
import org.jmicro.zk.ZKDataOperator;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:10:42
 */
@Component
public class ZKConfigLoader implements IConfigLoader{

	private IDataListener dataListener = null;
	
	@Override
	public void load(Map<String, String> params) {
		dataListener = new IDataListener(){
			@Override
			public void dataChanged(String path, String data) {
				updateData(path,data,params);
			}
		};
		 String root = Config.getConfigRoot();
		 List<String> children = ZKDataOperator.getIns().getChildren(root);
		 for(String child: children){
			 String p = root+"/"+child;
			 load(p,params);
		 }
	}

	private void updateData(String path, String data,Map<String,String> params) {
		String root = Config.getConfigRoot();
		String key = path.substring(root.length());
		params.put(key, data);
	}

	private void load(String p,Map<String,String> params) {
		String data = ZKDataOperator.getIns().getData(p);
		if(StringUtils.isEmpty(data)){
			List<String> children = ZKDataOperator.getIns().getChildren(p);
			 for(String child: children){
				 String pa = p + "/"+child;
				 load(pa,params);
			 }
		} else {
			updateData(p,data,params);
			ZKDataOperator.getIns().addDataListener(p, this.dataListener);
		}
	}
	
}
