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
import org.jmicro.api.config.IConfigChangeListener;
import org.jmicro.api.config.IConfigLoader;
import org.jmicro.api.raft.IDataListener;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.common.CommonException;
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
	
	private IConfigChangeListener lis = null;
	
	private IDataOperator dataOperator;
	
	@Override
	public void load(String root,Map<String, String> params) {
		dataListener = new IDataListener(){
			@Override
			public void dataChanged(String path, String data) {
				String supath = path.substring(root.length(),path.length());
				updateData(supath,data,params);
				if(lis != null){
					lis.configChange(path, data);
				}
			}
		};
		
		 //String globalRoot = Config.CfgDir+"/config";
		 List<String> children = dataOperator.getChildren(root);
		 for(String child: children){
			 loadOne(root,child,params);
		 }
	}

	private void updateData(String path, String data,Map<String,String> params) {
		params.put(path, data);
	}

	public void setDataOperator(IDataOperator dataOperator) {
		this.dataOperator = dataOperator;
	}

	private void loadOne(String root,String child,Map<String,String> params) {
		String fullpath = root+"/"+child;
		String data = dataOperator.getData(fullpath);
		if(!StringUtils.isEmpty(data)){
			updateData("/"+child,data,params);
			dataOperator.addDataListener(fullpath, this.dataListener);
		} 
		List<String> children = dataOperator.getChildren(fullpath);
		 for(String ch: children){
			 String pa = child + "/"+ch;
			 loadOne(root,pa,params);
		 }
	}

	@Override
	public void setConfigChangeListener(IConfigChangeListener lis) {
		if(this.lis != null){
			throw new CommonException("Listener have been set:" + this.lis.getClass().getName());
		}
		this.lis = lis;
	}
	
}
