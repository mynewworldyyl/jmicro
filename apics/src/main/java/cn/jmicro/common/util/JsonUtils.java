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
package cn.jmicro.common.util;

import java.lang.reflect.Type;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:01:07
 */
public class JsonUtils {

	private static JsonUtils instance = new JsonUtils();
	
	private static final DateTimeFormatter  ISO_LOCAL_DATE_TIME = new DateTimeFormatterBuilder()
             .parseCaseInsensitive()
             .append(DateTimeFormatter.ISO_LOCAL_DATE)
             .appendLiteral(' ')
             .append(DateTimeFormatter.ISO_LOCAL_TIME)
             .toFormatter();
	 
	 //序列化
   /* final static JsonSerializer<LocalDateTime> jsonSerializerDateTime = (localDateTime, type, jsonSerializationContext) -> new JsonPrimitive(localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    final static JsonSerializer<LocalDate> jsonSerializerDate = (localDate, type, jsonSerializationContext) -> new JsonPrimitive(localDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
  
    //反序列化
    final static JsonDeserializer<LocalDateTime> jsonDeserializerDateTime = (jsonElement, type, jsonDeserializationContext) -> {
    	String strd = jsonElement.getAsJsonPrimitive().getAsString();
    	//System.out.println(JsonUtils.class.getName()+": " + strd);
    	if(strd.indexOf("T") > 0) {
    		return LocalDateTime.parse(strd, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    	}else if(strd.indexOf("E") > 0) {
    		//浮点数长整数
    		Double d = Double.parseDouble(strd);
    		return LocalDateTime.ofInstant(Instant.ofEpochMilli(d.longValue()), ZoneId.systemDefault());
    	}else {
    		return LocalDateTime.parse(strd, ISO_LOCAL_DATE_TIME);
    	}
    };
    
    final static JsonDeserializer<LocalDate> jsonDeserializerDate = (jsonElement, type, jsonDeserializationContext) -> LocalDate.parse(jsonElement.getAsJsonPrimitive().getAsString(), DateTimeFormatter.ISO_LOCAL_DATE);
*/
	
	private JsonUtils() {}
	
	public synchronized static JsonUtils getIns() {
		return instance;
	}
	
	/*public GsonBuilder builder() {
		GsonBuilder builder = b();
		//builder.registerTypeAdapter(MessageType.class, new MessageTypeAdapter());
		//builder.registerTypeAdapter(MessageState.class, new MessageStateAdapter());
		//builder.registerTypeAdapter(MsgHeader.class, new MessageHeaderAdapter());
		//builder.registerTypeAdapter(MessageState.class, new MessageStateAdapter());
		
		return builder;
	}
	
	private GsonBuilder b() {
		return new GsonBuilder().registerTypeAdapter(LocalDateTime.class, jsonSerializerDateTime)
        .registerTypeAdapter(LocalDate.class, jsonSerializerDate)
        .registerTypeAdapter(LocalDateTime.class, jsonDeserializerDateTime)
        .registerTypeAdapter(LocalDate.class, jsonDeserializerDate)
		.enableComplexMapKeySerialization().serializeNulls();
	}*/
	
	
	public<T> T fromJson(String json, Class<T> c) {
		//GsonBuilder builder = b();
		//builder.registerTypeAdapter(MessageType.class, new MessageTypeAdapter());
		//builder.registerTypeAdapter(MessageState.class, new MessageStateAdapter());
		T obj = JSON.parseObject(json, c);
		return obj;
	}
	
	public <T> T fromJson(String json, java.lang.reflect.Type type) {
		//GsonBuilder builder = b();
		//System.out.println(this.getClass().getName()+": " + json);
		//TypeReference r = new TypeReference(type);
		T obj = JSON.parseObject(json, type);
		return obj;
	}
	
	public Map<String,String> getStringMap(String json) {
		Type type = new TypeReference<HashMap<String,String>>(){}.getType();
		Map<String,String> m = this.fromJson(json, type);
		return m;
	}
	
	public Map<String,Object> getStringKeyMap(String json) {
		Type type = new TypeReference<HashMap<String,Object>>(){}.getType();
		Map<String,Object> m = this.fromJson(json, type);
		return m;
	}
	
	public List<String> getStringValueList(String json,boolean innerJson) {
		Type type = new TypeReference<List<String>>(){}.getType();
		List<String> m = this.fromJson(json, type);
		return m;
	}
	
	public Set<String> getStringValueSet(String json,boolean innerJson) {
		Type type = new TypeReference<Set<String>>(){}.getType();
		return JsonUtils.getIns().fromJson(json, type);
	}
	
	public Object[] getObjectArray(String json,boolean innerJson) {
		Type type = new TypeReference<Object[]>(){}.getType();
		return JsonUtils.getIns().fromJson(json, type);
	}
	
	public String toJsonCheckProxy(Object obj) {
		if(obj == null) {
			return "";
		}
		//GsonBuilder builder = b();
		//builder.registerTypeAdapter(MessageType.class, new MessageTypeAdapter());
		//builder.registerTypeAdapter(MessageState.class, new MessageStateAdapter());
		//String json = builder.create().toJson(obj);
		return  toJson( obj);
	}
	
	public String toJson(Object obj) {
		if(obj == null) {
			return "";
		}
		//GsonBuilder builder = b();
		//builder.registerTypeAdapter(MessageType.class, new MessageTypeAdapter());
		//builder.registerTypeAdapter(MessageState.class, new MessageStateAdapter());
		//String json = builder.create().toJson(obj);
		return JSON.toJSONString(obj);
	}
	
	public String toJson(Object obj,java.lang.reflect.Type type) {
		if(obj == null) {
			return "";
		}
		//GsonBuilder builder = b();
		//builder.registerTypeAdapter(MessageType.class, new MessageTypeAdapter());
		//builder.registerTypeAdapter(MessageState.class, new MessageStateAdapter());
		//String json = builder.create().toJson(obj,type);
		return JSON.toJSONString(obj);
	}
	
	 public String processToJson(String json) {
	    	json = json.replaceAll("\\{", "(");
	    	json = json.replaceAll("\\}", ")");
	    	json = json.replaceAll("\"", "@@");
	    	return json;
	 }
	 
	 public String processFromJson(String json) {
	    	json = json.replaceAll("\\(","\\{");
	    	json = json.replaceAll("\\)","\\}");
	    	json = json.replaceAll( "@@","\"");
	    	return json;
	 }
	 
	 public String modifyGjonBug(String content) {
			content=content.substring(1, content.length()-1);
			content = content.replaceAll("\\\\", "");
			return content;
	 }
}
