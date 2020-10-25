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
	public static final String _ID = "_id";
	
	public static final byte UPDATE_TYPE_ID = 1;
	public static final byte UPDATE_TYPE_FILTER = 2;
	
	<T> boolean  save(String table,T val,Class<T> cls,boolean async,boolean toDocument);
	
	<T> boolean  save(String table,List<T> val,Class<T> cls,boolean async,boolean toDocument);
	
	<T> boolean  save(String table,T[] val,Class<T> cls,boolean async,boolean toDocument);
	
	<T> boolean updateById(String table,T val,Class<T> targetClass,String idName, boolean async);
	
	<T> boolean updateOrSaveById(String table, T val,Class<T> cls,String tidName,boolean async);
	
	<T> boolean update(String table, Object filter, Object updater, Class<T> cls);
	
	boolean deleteById(String table,Object id,String idName);
	
	<T> Set<T>  distinct(String table, String fieldName,Class<T> cls);
	
    int deleteByQuery(String table, Object query);
	
	<T> List<T> query(String table,Map<String,Object> queryConditions, Class<T> targetClass,int pageSize,int curPage);
	
	<T> List<T> query(String table, Map<String, Object> queryConditions, Class<T> targetClass);
	
	long count(String table,Map<String,Object> queryConditions);
	
	<T> T getOne(String table,Map<String,Object> queryConditions,Class<T> targetClass);
	
}
