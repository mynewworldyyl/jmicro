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
package org.jmicro.api.idgenerator;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.api.raft.RaftBaseIdGenerator;
import org.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年12月9日 下午5:24:20
 */
@Component("uniqueIdGenerator")
public class UniqueIdGenerator implements IIdGenerator,IIdServer,IIdClient{

	private static final String ID_IDR = Constants.CFG_ROOT + "/id/";
	
	@Inject(required=true)
	private IDataOperator dataOperator;
	
	private RaftBaseIdGenerator idg = null;
	
	public void init(){
		idg = new RaftBaseIdGenerator(ID_IDR,this.dataOperator);
	}
	
	public Integer[] getIntIds(String idKey, int num) {
		return idg.getIntIds(idKey, num);
	}
	
	public Long[] getLongIds(String idKey, int num) {
		return idg.getLongIds(idKey, num);
	}
	
	public String[] getStringIds(String idKey, int num) {
		return this.idg.getStringIds(idKey, num);
	}
	
	@Override
	public Long getLongId(String idKey) {
		return this.idg.getLongId(idKey);
	}

	@Override
	public String getStringId(String idType) {
		return this.idg.getStringId(idType);
	}

	@Override
	public Integer getIntId(String idKey) {
		return this.idg.getIntId(idKey);
	}

}
