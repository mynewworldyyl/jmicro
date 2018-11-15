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
package org.jmicro.idgenerator;

import java.util.HashSet;
import java.util.Set;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.idgenerator.IIdGenerator;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:11:16
 */
@Component(value=Constants.DEFAULT_IDGENERATOR,level=9)
public class JMicroIdGenerator implements IIdGenerator {
	
	private static final String ID_IDR = Constants.CFG_ROOT + "/id/";
	
	@Inject(required=true)
	private IDataOperator dataOperator;
	
	public void init(){
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Set<Integer> getIntId(Class<?> idType, int num) {
		if(num > 1) {
			return (Set<Integer>)this.get(idType,Integer.class,num);
		}else {
			Set<Integer> set = new HashSet<>();
			Integer id = (Integer)this.get(idType,Integer.class,num);
			set.add(id);
			return set;
		}
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Set<Long> getLongId(Class<?> idType, int num) {
		if(num > 1) {
			return (Set<Long>)this.get(idType,Long.class,num);
		}else {
			Set<Long> set = new HashSet<>();
			Long id = (Long)this.get(idType,Long.class,num);
			set.add(id);
			return set;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Set<String> getStringId(Class<?> idType, int num) {
		if(num > 1) {
			return (Set<String>)this.get(idType,Long.class,num);
		}else {
			Set<String> set = new HashSet<>();
			String id = (String)this.get(idType,Long.class,num);
			set.add(id);
			return set;
		}
	}
	
	@Override
	public Long getLongId(Class<?> idType) {
		return (Long)this.get(idType,Long.class,1);
	}

	@Override
	public String getStringId(Class<?> idType) {
		return (String)this.get(idType,String.class,1);
	}

	@Override
	public Integer getIntId(Class<?> idType) {
		return (Integer)this.get(idType,Integer.class,1);
	}
	
	private Object get(Class<?> idType,Class<?> clazzType,int num){
		if(num <= 0) {
			throw new CommonException("Req ID num must be more than one");
		}
		
		String path = ID_IDR + idType.getName();
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
				Set<Long> ids = new HashSet<Long>();
				for(int i=0; i < num;i++) {
					ids.add(r+i);
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
				Set<Integer> ids = new HashSet<Integer>();
				for(int i=0; i < num;i++) {
					ids.add(r+i);
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
				Set<String> ids = new HashSet<String>();
				for(int i=0; i < num;i++) {
					ids.add((r+i)+"");
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
