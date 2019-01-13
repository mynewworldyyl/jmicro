package org.jmicro.api.codec.typecoder;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmicro.api.codec.Decoder;
import org.jmicro.api.codec.TypeCoderFactory;
import org.jmicro.api.codec.TypeUtils;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface TypeCoder<T> extends Comparable<TypeCoder<T>>{
	
	public static final Logger logger = LoggerFactory.getLogger(TypeCoder.class);
	
	/**
	 * 指定值的类型编码
	 * 编码的时间，value.getClass()即为值的类型
	 * 
	 * 解码的时候，从ByteBuffer读取的前缀类型即为此类型。
	 * 
	 * 获取到类型后，即可取得对应类型的编码解码器
	 * @return
	 */
	Class<T> type();
	
	short code();
	
	byte prefixCode();

	/**
	 * 
	 * @param buffer
	 * @param val
	 * @param type
	 */
	void encode(ByteBuffer buffer, T val, Class<?> fieldDeclareType, Type genericType);
	
	T decode(ByteBuffer buffer, Class<?> fieldDeclareType, Type genericType);
	
	boolean canSupport(Class<?> clazz);
	
	/**
    * 对那些没有指定编码器的类做编码，此类前缀肯定是全类名
    * 
    * @param buffer
    * @param obj
    * @param fieldDeclareType
    */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void encodeByReflect(ByteBuffer buffer, Object obj, Class<?> fieldDeclareType,Type genericType) {

		// 进入此方法，obj必须不能为NULL
		Class<?> cls = obj.getClass();

		if(!Modifier.isPublic(cls.getModifiers())) {
			// 非公有类，不能做序列化
			throw new CommonException("should be public class [" + cls.getName() + "]");
		}

		List<String> fieldNames = new ArrayList<>();
		Utils.getIns().getFieldNames(fieldNames, cls);
		fieldNames.sort((v1, v2) -> v1.compareTo(v2));

		TypeCoder coder = TypeCoderFactory.getDefaultCoder();
		
		for (int i = 0; i < fieldNames.size(); i++) {

			String fn = fieldNames.get(i);

			Field f = Utils.getIns().getClassField(cls, fn);
			
			if(Modifier.isTransient(f.getModifiers())) {
				//transient字段不序列化
				continue;
			}
			Object v = TypeUtils.getFieldValue(obj, f);
			coder.encode(buffer, v, f.getType(), f.getGenericType());

		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void encodeCollection(ByteBuffer buffer, Collection objs, Class<?> declareFieldType, Type genericType) {
		if(objs == null || objs.isEmpty()) {
			putLength(buffer,0);
			return;
		}
		
		 Class<?> valueType = null;
		 if(genericType != null) {
			 valueType = TypeUtils.finalParameterType((ParameterizedType)genericType,0);
		 }
		 putLength(buffer,objs.size());
		
		TypeCoder coder = TypeCoderFactory.getDefaultCoder();
		for(Object o: objs){
			coder.encode(buffer, o, valueType, null);
		}
		
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <K,V> void encodeMap(ByteBuffer buffer,Map<K,V> map, ParameterizedType genericType){
		if(map == null || map.size() == 0) {
			putLength(buffer,0);
			return;
		}
		putLength(buffer,map.size());
		
		Class<?> keyType = null;
		Class<?> valueType = null;

		if (genericType != null) {
			keyType = TypeUtils.finalParameterType((ParameterizedType) genericType, 0);
			valueType = TypeUtils.finalParameterType((ParameterizedType) genericType, 1);
		}
		
		TypeCoder coder = TypeCoderFactory.getDefaultCoder();
		
		for(Map.Entry<K,V> e: map.entrySet()){
			coder.encode(buffer, e.getKey(), keyType, null);
			coder.encode(buffer, e.getValue(), valueType, null);
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <V> void encodeArray(ByteBuffer buffer, Object objs, Class<?> fieldDeclareType
			, Type genericType){
		if(objs == null) {
			putLength(buffer,0);
			return;
		}
		int len = Array.getLength(objs);
		putLength(buffer,len);
		
		if(len <=0) {
			return;
		}
		TypeCoder coder = TypeCoderFactory.getDefaultCoder();
		for(int i = 0; i < len; i++){
			Object v = Array.get(objs, i);
			coder.encode(buffer, v, fieldDeclareType, genericType);
		}
	}
	
	public static ParameterizedType genericType(Type genericType){
		if(!(genericType instanceof ParameterizedType)) {
			genericType=null;
		}
		return (ParameterizedType)genericType;
	}
	
	
	
	/**********************************解码相关代码开始**************************************/
	
	public static String decodeString(ByteBuffer buffer){
		int len = buffer.getShort();
		if(len <= 0) {
			return null;
		}

		try {
			byte[] data = new byte[len];
			buffer.get(data,0,len);
			return new String(data,Constants.CHARSET);
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object decodeByReflect(ByteBuffer buffer, Class<?> cls,Type genericType) {
		
		int m = cls.getModifiers();
		
		if(Modifier.isAbstract(m) || Modifier.isInterface(m) || !Modifier.isPublic(m)){
			logger.warn("decodeByReflect class [{}] not support decode",cls.getName());
			throw new CommonException("invalid class modifier: "+ cls.getName());
		}
		
		Object obj = null;
		try {
			obj = cls.newInstance();
		} catch (InstantiationException | IllegalAccessException e1) {
			throw new CommonException("fail to instance class [" +cls.getName()+"]",e1);
		}
		
		List<String> fieldNames = new ArrayList<>();
		Utils.getIns().getFieldNames(fieldNames,cls);
		fieldNames.sort((v1,v2)->v1.compareTo(v2));
		
		TypeCoder coder = TypeCoderFactory.getDefaultCoder();
		for(int i =0; i < fieldNames.size(); i++){

			Field f = Utils.getIns().getClassField(cls, fieldNames.get(i));
			
			if(Modifier.isTransient(f.getModifiers())) {
				continue;
			}
			try {
				Object v = coder.decode(buffer, f.getType(), f.getGenericType());
				TypeUtils.setFieldValue(obj, v, f);
			} catch (CommonException e) {
				e.setOthers(e.getOthers()+" | fieldName:"+f.getName()+", obj:"+obj.toString());
			    logger.error("",e);
				throw e;
			}
			
		}
		return obj;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map<Object,Object> decodeMap(ByteBuffer buffer, ParameterizedType paramType){
		int len = buffer.getShort();
		if(len <= 0) {
			return Collections.EMPTY_MAP;
		}
		
		Class<?> keyType = TypeUtils.finalParameterType(paramType,0);
		Class<?> valType = TypeUtils.finalParameterType(paramType,1);
		
		TypeCoder coder = TypeCoderFactory.getDefaultCoder();
		Map<Object,Object> map = new HashMap<>();
		for(; len > 0; len--) {
			Object key = coder.decode(buffer, keyType, null);
			Object value = coder.decode(buffer, valType, null);
			map.put(key, value);
		}
		
		return map;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void decodeCollection(ByteBuffer buffer, Collection coll, Class<?> declareType,Type genericType){
		int len = buffer.getShort();
		if(len <= 0) {
			return ;
		}
		
		Class<?> valueType = null;
		 if(genericType != null) {
			 valueType = TypeUtils.finalParameterType((ParameterizedType)genericType,0);
		 }
		 
		 TypeCoder coder = TypeCoderFactory.getDefaultCoder();
		 
		for(int i =0; i <len; i++){
			Object v = coder.decode(buffer, valueType, null);
			if(v != null) {
				coll.add(v);
			}
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object decodeArray(ByteBuffer buffer, Class<?> fieldDeclareType, Type genericType){

		int len = buffer.getShort();
		if(len <= 0) {
			return null;
		}
		
		TypeCoder coder = TypeCoderFactory.getDefaultCoder();
		 
		Object objs = Array.newInstance(fieldDeclareType, len);
		for(int i = 0; i < len; i++){
			Array.set(objs, i, coder.decode(buffer, fieldDeclareType, null));
		}
		
		return objs;
	}
	
	/**********************************解码相关代码结束**************************************/
	
/*******************************STATIC METHOD****************************/
	
	public static void putCodeType(ByteBuffer buffer,byte prefixType,short code) {
		//类型前缀编码
		buffer.put(prefixType);
		buffer.putShort(code);
	}
	
	public static void putStringType(ByteBuffer buffer,String clazz) {
		//类型前缀类型
		buffer.put(Decoder.PREFIX_TYPE_STRING);
		//类名称
		encodeString(buffer, clazz);
	}
	
	public static void putLength(ByteBuffer buffer,int len) {
		buffer.putShort((short)len);
	}
	
	public static void encodeString(ByteBuffer buffer,String str){
		if(StringUtils.isEmpty(str)){
			putLength(buffer,0);
			return;
		}
	    try {
	    	/*ServiceMethod sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY,null);
			if(sm != null && "intrest".equals(sm.getKey().getMethod()) && str.startsWith("[L")) {
				logger.debug("eltType: {}",str);
			}*/
			byte[] data = str.getBytes(Constants.CHARSET);
			putLength(buffer,data.length);
			buffer.put(data);
		} catch (UnsupportedEncodingException e) {
			throw new CommonException("encodeString error: "+str);
		}
	}
	
	public static Class<?> getType(ByteBuffer buffer, Class<?> declareFieldType, Type genericType) {
		if(declareFieldType == null || !TypeUtils.isFinal(declareFieldType)) {
			return getType(buffer);
		}
		return declareFieldType;
	}
	
	public static Class<?> getType(ByteBuffer buffer) {

		String clsName = TypeCoder.decodeString(buffer);
		if(clsName == null) {
			throw new CommonException("Decode invalid class full name!");
		}
		Class<?> cls = null;
		try {
			if(clsName.startsWith("[L")) {
				clsName = clsName.substring(2,clsName.length()-1);
				cls = Thread.currentThread().getContextClassLoader().loadClass(clsName);
				cls = Array.newInstance(cls, 0).getClass();
			} else {
				cls = Thread.currentThread().getContextClassLoader()
						.loadClass(clsName);
			}
		} catch (ClassNotFoundException e) {
			throw new CommonException("class not found:" + clsName,e);
		}
	    return cls;
	}
	
	/*******************************STATIC METHOD END****************************/
	
}
