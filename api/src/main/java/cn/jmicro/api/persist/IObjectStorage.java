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

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.data.AbstractReportElt;
import cn.jmicro.api.data.ReportDataJRso;
import cn.jmicro.api.storage.FileJRso;
import cn.jmicro.common.Utils;

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
	
	<T> boolean  save(String table,T val,Class<T> cls,boolean async);
	
	<T> boolean  saveSync(String table,T val,Class<T> cls);
	
	<T> boolean  save(String table,List<T> val,Class<T> cls,boolean async);
	
	<T> boolean  save(String table,T[] val,Class<T> cls,boolean async);
	
	<T> boolean updateById(String table,T val,Class<T> targetClass,String idName, boolean async);
	
	<T> boolean updateOrSaveById(String table, T val,Class<T> cls,String tidName,boolean async);
	
	<T> int update(String table, Object filter, Object updater, Class<T> cls);
	
	boolean deleteById(String table,Object id,String idName);
	
	<T> Set<T>  distinct(String table, String fieldName,Class<T> cls);
	
    int deleteByQuery(String table, Object query);
	
	<T> List<T> query(String table,Map<String,Object> filter, Class<T> targetClass,int pageSize,int curPage);
	
	<T> List<T> query(String table, Map<String, Object> filter, Class<T> targetClass);
	
	<T> List<T> query(String table, Map<String, Object> filter, Class<T> targetClass, String orderBy,Integer asc);
	
	<T> List<T> query(String table,Map<String,Object> filter, Class<T> targetClass,int pageSize,int curPage, String orderBy,Integer asc);
	
	<T> List<T> query(String table,Map<String,Object> filter, Class<T> targetClass,int pageSize,int curPage,String[] colums, String orderBy,Integer asc);
	
	int count(String table,Map<String,Object> filter);
	
	<T> T getOne(String table,Map<String,Object> filter,Class<T> targetClass);
	
	<T> Set<T> getDistinctField(String table, Map<String, Object> filter, String fieldName, Class<T> resultClass);
	List<Map<String, Object>> getFields(String table,Map<String, Object> filter, String...fields);
	
	RespJRso<String> saveFile2Db(FileJRso pr);
	
	RespJRso<String> saveSteam2Db(FileJRso pr,InputStream is);
	
	RespJRso<String> saveByteArray2Db(FileJRso pr,byte[] byteData);
	
	ReportDataJRso<AbstractReportElt> statisDataByCreatedDate(Map<String,Object> filter, String table,
			String[] keys, String grpFieldName, Byte dayNum, Byte timeLen, Boolean counter);
	
	ReportDataJRso<AbstractReportElt> statisDataByGroupName(
			Map<String,Object> filter, String table, String[] keys, String grpFieldName, Boolean counter);
	
	boolean fileSystemEnable();
	
	/**
	 * 
	 * @param sort
	 * @param def
	 * @return 1:升序， -1： 倒序
	 */
	public static int getOrderVal(String sort, int def) {
		if(Utils.isEmpty(sort)) return def;
		String lo = sort.toLowerCase();
		if("asc".equals(lo)) return 1;
		if("desc".equals(lo)) return -1;
		try {
			int v = Integer.parseInt(lo);
			if(v >= 0) return 1; else return -1;
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
