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

import cn.jmicro.common.CommonException;
import cn.jmicro.common.util.StringUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年12月9日 下午5:23:13
 */
public class RaftBaseIdGenerator {

	private String baseDir = null;
	
	private IDataOperator dataOperator = null;
	
	public RaftBaseIdGenerator(String baseDir,IDataOperator dataOperator) {
		if(StringUtils.isEmpty(baseDir)) {
			throw new CommonException("Base ID directory cannot be NULL");
		}
		if(dataOperator == null) {
			throw new CommonException("dataOperator cannot be NULL");
		}
		this.baseDir = baseDir;
		this.dataOperator = dataOperator;
	}
	
	public Integer[] getIntIds(String idKey, int num) {
		if(num > 1) {
			return (Integer[])this.get(idKey,Integer.class,num);
		}else {
			Integer id = (Integer)this.get(idKey,Integer.class,num);
			return new Integer[] {id};
		}
		
	}
	
	public Long[] getLongIds(String idKey, int num) {
		if(num > 1) {
			return (Long[])this.get(idKey,Long.class,num);
		}else {
			Long id = (Long)this.get(idKey,Long.class,num);
			return new Long[] {id};
		}
	}
	
	public String[] getStringIds(String idKey, int num) {
		if(num > 1) {
			return (String[])this.get(idKey,String.class,num);
		}else {
			String id = (String)this.get(idKey,String.class,num);
			return new String[] {id};
		}
	}
	
	public Long getLongId(String idKey) {
		return (Long)this.get(idKey,Long.class,1);
	}

	public String getStringId(String idType) {
		return (String)this.get(idType,String.class,1);
	}

	public Integer getIntId(String idKey) {
		return (Integer)this.get(idKey,Integer.class,1);
	}
	
	private Object get(String idKey,Class<?> clazzType,int num){
		if(num <= 0) {
			throw new CommonException("Req ID num must be more than one");
		}
		
		synchronized(clazzType) {

			String path = baseDir + idKey;
			String idStr = "1";
			if(this.dataOperator.exist(path)){
				 idStr = dataOperator.getData(path);
			} else {
				dataOperator.createNode(path, idStr, false);
			}
			
			Object result = null;
			
			if(clazzType == Long.class) {
				if(num == 1){
					long r = Long.parseLong(idStr);
					result = r;
					idStr = (r+1)+"";
				} else {
					long r = Long.parseLong(idStr);
					Long[] ids = new Long[num];
					for(int i=0; i < num;i++) {
						ids[i] = r+i;
					}
					r += num;
					idStr = r+"";
					result = ids;
				}
			}else if(clazzType == Integer.class) {
				if(num == 1){
					int r = Integer.parseInt(idStr);
					result = r;
					idStr = (r+1)+"";
				} else {
					int r = Integer.parseInt(idStr);
					Integer[] ids = new Integer[num];
					for(int i=0; i < num;i++) {
						ids[i] = r+i;
					}
					r += num;
					idStr = r+"";
					result = ids;
				}
			}else if(clazzType == String.class) {
				if(num == 1){
					long r = Long.parseLong(idStr);
					result = r+"";
					idStr = (r+1)+"";
				} else {
					long r = Long.parseLong(idStr);
					String[] ids = new String[num];
					for(int i=0; i < num;i++) {
						ids[i] = (r+i)+"";
					}
					r += num;
					idStr = r+"";
					result = ids;
				}
			}
			
			dataOperator.setData(path, idStr);
			return result;
		}
	}
}
