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
package cn.jmicro.api.persist;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * 
 * @author Yulei Ye
 * @date 2020年1月12日
 */
public interface IObjectStorage {

	public static final String UPDATED_TIME = "updatedTime";
	
	public static final String CREATED_TIME = "createdTime";
	
	public static final String ID = "id";
	
	<T> boolean  save(String table,T val,boolean async);
	
	<T> boolean  save(String table,Set<T> val,boolean async);
	
	<T> boolean  save(String table,T[] val,boolean async);
	
	boolean update(String table,Object id,Object val,boolean async);
	
	boolean updateOrSave(String table,Object id,Object val,boolean async);
	
	boolean update(String table, Object filter, Object updater);
	
	boolean deleteById(String table,Object id);
	
	<T> Set<T>  distinct(String table, String fieldName,Class<T> cls);
	
    int deleteByQuery(String table, Object query);
	
	<T> List<T> query(String table,Map<String,Object> queryConditions,int offset,int pageSize);
}
