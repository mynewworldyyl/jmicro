package org.jmicro.registry.zk;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class JsonUtils {

	private static JsonUtils instance = new JsonUtils();
	
	private JsonUtils() {}
	
	public synchronized static JsonUtils getIns() {
		return instance;
	}
	
	public GsonBuilder builder() {
		GsonBuilder builder = new GsonBuilder();
		//builder.registerTypeAdapter(MessageType.class, new MessageTypeAdapter());
		//builder.registerTypeAdapter(MessageState.class, new MessageStateAdapter());
		//builder.registerTypeAdapter(MsgHeader.class, new MessageHeaderAdapter());
		//builder.registerTypeAdapter(MessageState.class, new MessageStateAdapter());
		return builder;
	}
	
	
	public<T> T fromJson(String json, Class<T> c) {
		GsonBuilder builder = new GsonBuilder();
		//builder.registerTypeAdapter(MessageType.class, new MessageTypeAdapter());
		//builder.registerTypeAdapter(MessageState.class, new MessageStateAdapter());
		T obj = builder.create().fromJson(json, c);
		return obj;
	}
	
	public <T> T fromJson(String json, java.lang.reflect.Type type) {
		GsonBuilder builder = new GsonBuilder();
		T obj = builder.create().fromJson(json, type);
		return obj;
	}
	
	public Map<String,String> getStringMap(String json) {
		Type type = new TypeToken<HashMap<String,String>>(){}.getType();
		Map<String,String> m = this.fromJson(json, type);
		return m;
	}
	
	public List<String> getStringValueList(String json,boolean innerJson) {
		Type type = new TypeToken<List<String>>(){}.getType();
		List<String> m = this.fromJson(json, type);
		return m;
	}
	
	public String toJson(Object obj) {
		GsonBuilder builder = new GsonBuilder();
		//builder.registerTypeAdapter(MessageType.class, new MessageTypeAdapter());
		//builder.registerTypeAdapter(MessageState.class, new MessageStateAdapter());
		String json = builder.create().toJson(obj);
		return json;
	}
	
	public String toJson(Object obj,java.lang.reflect.Type type) {
		GsonBuilder builder = new GsonBuilder();
		//builder.registerTypeAdapter(MessageType.class, new MessageTypeAdapter());
		//builder.registerTypeAdapter(MessageState.class, new MessageStateAdapter());
		String json = builder.create().toJson(obj,type);
		return builder.create().toJson(obj,type);
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
