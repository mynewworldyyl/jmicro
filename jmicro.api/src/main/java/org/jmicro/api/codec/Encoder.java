package org.jmicro.api.codec;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.dubbo.common.utils.StringUtils;
import org.jmicro.api.exception.CommonException;
import org.jmicro.api.server.RpcResponse;
import org.jmicro.common.Constants;

public class Encoder implements IEncoder{

	@Override
	public byte[] encode(RpcResponse response) {
		ByteBuffer bb = ByteBuffer.allocate(1024*8);
		encodeObject(bb,response);
		bb.flip();
		return bb.array();
	}

	public static <V> void encodeObject(ByteBuffer buffer,V obj){
	
		Class<?> cls = obj.getClass();
		Integer type = Decoder.getType(cls);
		
		if(type == null || type <= 0) {
			buffer.put(Decoder.PREFIX_TYPE_STRING);
			encodeString(buffer,cls.getName());
		}else {
			cls = Decoder.getClass(type);
			buffer.put(Decoder.PREFIX_TYPE_BYTE);
			buffer.put((byte)type.intValue());
		}
		
		Object v = null;
		if(Map.class == cls){
			encodeMap(buffer,(Map<Object,Object>)obj);
		}else if(Collection.class == cls){
			 encodeList(buffer,(Collection)obj);
		}else if(cls == Array.class){
			encodeObjects(buffer,(Object[])obj);
		}else if(cls == String.class) {
			encodeString(buffer,(String)obj);
		}else if(cls == void.class || cls == Void.class) {
			v =  null;
		}else if(cls == int.class || cls == Integer.class){
			buffer.putInt((Integer)obj);
		}else if(cls == byte.class || cls == Byte.class){
			buffer.put((Byte)obj);
		}else if(cls == short.class || cls == Short.class){
			buffer.putShort((Short)obj);
		}else if(cls == long.class || cls == Long.class){
			buffer.putLong((Long)obj);
		}else if(cls == float.class || cls == Float.class){
			buffer.putFloat((Float)obj);
		}else if(cls == double.class || cls == Double.class){
			buffer.putDouble((Double)obj);
		}else if(cls == boolean.class || cls == Boolean.class){
			boolean b = (Boolean)obj;
			buffer.put(b?(byte)1:(byte)0);
		}else if(cls == char.class || cls == Character.class){
			buffer.putChar((Character)obj);
		} else {
			encodeByReflect(buffer,cls,type,obj);
		}
	
	}
	
	private static void encodeByReflect(ByteBuffer buffer, Class<?> cls, Integer type,Object obj) {

		
		int m = cls.getModifiers() ;
		
		if(Modifier.isAbstract(m) || Modifier.isInterface(m)){
			cls = obj.getClass();
			m = cls.getModifiers();
		}
		
		if(!Modifier.isPublic(m)) {
			throw new CommonException("should be public class [" +cls.getName()+"]");
		}
		
		List<String> fieldNames = new ArrayList<>();
		Field[] fs = cls.getDeclaredFields();
		for(Field f: fs){
			if(Modifier.isTransient(f.getModifiers()) || Modifier.isFinal(f.getModifiers())
					|| Modifier.isStatic(f.getModifiers())){
				continue;
			}
			fieldNames.add(f.getName());
		}
		
		fieldNames.sort((v1,v2)->v1.compareTo(v2));
		
		for(int i = 0; i < fieldNames.size(); i++){
			try {
				String fn = fieldNames.get(i);
				Field f = cls.getDeclaredField(fn);
				
				boolean bf = f.isAccessible();
				if(!bf){
					f.setAccessible(true);
				}
				Object v = f.get(obj);
				if(!bf){
					f.setAccessible(false);
				}
				
				encodeObject(buffer,v);
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				throw new CommonException("",e);
			}
		}
		
	}

	private static void encodeList(ByteBuffer buffer, Collection objs) {
		buffer.putInt(objs.size());
		for(Object o: objs){
			encodeObject(buffer,o);
		}
	}

	private static <V> void encodeObjects(ByteBuffer buffer,V[] objs){
		
		int len = objs.length;
		buffer.putInt(len);
		
		if(len <=0) {
			return;
		}
		for(Object o : objs){
			encodeObject(buffer,o);
		}
	}
	
	private static <K,V> void encodeMap(ByteBuffer buffer,Map<K,V> map){
		
		int len = map.size();
		buffer.putInt(len);
		
		if(len <=0) {
			return;
		}
		
		for(Map.Entry<K,V> e: map.entrySet()){
			encodeObject(buffer,e.getKey());
			encodeObject(buffer,e.getValue());
		}
		
	}
	
	private static void encodeString(ByteBuffer buffer,String str){
		if(StringUtils.isEmpty(str)){
			buffer.putInt(0);
			return;
		}
		/*buffer.putInt(str.length());
		for(int i =0; i < str.length(); i++){
			buffer.putChar(str.charAt(i));
		}*/
	    try {
			byte[] data = str.getBytes(Constants.CHARSET);
			buffer.putInt(data.length);
			buffer.put(data);
		} catch (UnsupportedEncodingException e) {
		}
	}

}
