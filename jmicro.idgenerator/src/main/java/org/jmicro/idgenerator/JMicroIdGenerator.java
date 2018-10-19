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

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.idgenerator.IIdGenerator;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.common.Constants;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:11:16
 */
@Component(value=Constants.DEFAULT_IDGENERATOR,level=20)
public class JMicroIdGenerator implements IIdGenerator {
	
	private static String ID_IDR = Constants.CFG_ROOT + "/id/";
	
	@Inject(required=true)
	private IDataOperator dataOperator;
	
	public void init(){
		
	}
	
	@Override
	public long getLongId(Class<?> idType) {
		String idStr = this.get(idType);
		Long id = Long.parseLong(idStr)+1;
		setData(idType,id+"");
		return id;
	}
	
	@Override
	public String getStringId(Class<?> idType) {
		String idStr = this.getLongId(idType)+"";
		return idStr;
	}

	private void setData(Class<?> idType, String id) {
		String path = ID_IDR + idType.getName();
		dataOperator.setData(path, id);
	}
	
	private String get(Class<?> idType){
		String path = ID_IDR + idType.getName();
		String idStr = "0";
		if(this.dataOperator.exist(path)){
			 idStr = dataOperator.getData(path);
		} else {
			dataOperator.createNode(path, idStr, false);
		}
		return idStr;
	}

}
