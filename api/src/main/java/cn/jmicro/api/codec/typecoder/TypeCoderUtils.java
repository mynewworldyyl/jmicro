package cn.jmicro.api.codec.typecoder;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.codec.DecoderConstant;
import cn.jmicro.api.codec.ISerializeObject;
import cn.jmicro.api.codec.JDataInput;
import cn.jmicro.api.codec.JDataOutput;
import cn.jmicro.api.codec.TypeCoderFactoryUtils;
import cn.jmicro.api.codec.TypeUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Utils;

public class TypeCoderUtils {

	public static final Logger logger = LoggerFactory.getLogger(ITypeCoder.class);
	
	public static final Map<String,Class<?>> classCache = new HashMap<>();
	public static final Object loadingLock = new Object();
	
	public static final Map<Class<?>,List<Field>> classFieldsCache = new HashMap<>();
	public static final Object loadingFieldLock = new Object();
	
	/**
	    * 对那些没有指定编码器的类做编码，此类前缀肯定是全类名
	    * 
	    * @param buffer
	    * @param obj
	    * @param fieldDeclareType
		 * @throws IOException 
	    */
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public static void encodeByReflect(DataOutput buffer, Object obj, Class<?> fieldDeclareType,
				Type genericType) throws IOException {

			// 进入此方法，obj必须不能为NULL
			Class<?> cls = obj.getClass();

			if(!Modifier.isPublic(cls.getModifiers())) {
				// 非公有类，不能做序列化
				throw new CommonException("should be public class [" + cls.getName() + "]");
			}

			List<Field> fields = loadClassFieldsFromCache(cls);

			ITypeCoder coder = TypeCoderFactoryUtils.getIns().getDefaultCoder();
			
			for (int i = 0; i < fields.size(); i++) {

				Field f  = fields.get(i);

				//Field f = Utils.getIns().getClassField(cls, fn);
				
				if(Modifier.isTransient(f.getModifiers())) {
					//transient字段不序列化
					continue;
				}
				Object v = TypeUtils.getFieldValue(obj, f);
				if(v != null) {
					coder.encode(buffer, v, f.getType(), f.getGenericType());
				} else {
					Class t = f.getType();
					if(t == byte.class || t == Byte.TYPE || t == Byte.class ) {
						 buffer.writeByte(0);
					}else if(t == short.class || t == Short.TYPE || t == Short.class ) {
						 buffer.writeShort(0);
					}else if(t == int.class || t == Integer.TYPE || t == Integer.class ) {
						 buffer.writeInt(0);
					}else if(t == long.class || t == Long.TYPE || t == Long.class ) {
						 buffer.writeLong(0);
					}else if(t == float.class || t == Float.TYPE || t == Float.class ) {
						 buffer.writeFloat(0);
					}else if(t == double.class || t == Double.TYPE || t == Double.class ) {
						 buffer.writeDouble(0D);
					}else if(t == boolean.class || t == Boolean.TYPE || t == Boolean.class ) {
						 buffer.writeBoolean(false);
					}else if(t == char.class || t == Character.TYPE || t == Character.class ) {
						 buffer.writeChar(' ');
					}else if(t == String.class ) {
						 buffer.writeUTF("");
					}else if(t == Date.class ) {
						 buffer.write(0);
					} else {
						buffer.write(DecoderConstant.PREFIX_TYPE_NULL);
					}
				}
			}
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		public static void encodeCollection(DataOutput buffer, Collection objs, 
				Class<?> declareFieldType, Type genericType) throws IOException {
			if(objs == null || objs.isEmpty()) {
				putLength(buffer,0);
				return;
			}
			
			 Class<?> valueType = null;
			 if(genericType != null) {
				 valueType = TypeUtils.finalParameterType((ParameterizedType)genericType,0);
			 }
			 putLength(buffer,objs.size());
			
			ITypeCoder coder = TypeCoderFactoryUtils.getIns().getDefaultCoder();
			for(Object o: objs){
				coder.encode(buffer, o, valueType, null);
			}
			
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public static <K,V> void encodeMap(DataOutput buffer,Map<K,V> map, 
				ParameterizedType genericType) throws IOException{
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
			
			ITypeCoder coder = TypeCoderFactoryUtils.getIns().getDefaultCoder();
			
			for(Map.Entry<K,V> e: map.entrySet()){
				if(e.getKey() == null) {
					buffer.writeByte(DecoderConstant.PREFIX_TYPE_NULL);
				}else {
					buffer.writeByte(DecoderConstant.PREFIX_TYPE_PROXY);
					coder.encode(buffer, e.getKey(), keyType, null);
				}
				
				if(e.getValue() == null) {
					buffer.writeByte(DecoderConstant.PREFIX_TYPE_NULL);
				}else {
					buffer.writeByte(DecoderConstant.PREFIX_TYPE_PROXY);
					coder.encode(buffer, e.getValue(), valueType, null);
				}
				
			}
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public static <V> void encodeArray(DataOutput buffer, Object objs, Class<?> fieldDeclareType
				, Type genericType) throws IOException{
			
			if(objs == null) {
				putLength(buffer,0);
				return;
			}
			int len = Array.getLength(objs);
			putLength(buffer,len);
			
			if(len <=0) {
				return;
			}
			
			JDataOutput jo = (JDataOutput)buffer;
			
			byte flag = 0;
			int flagIndex = jo.position();
			jo.write(flag);
			
			boolean writeEvery;
		   
			boolean sameElt = sameArrayTypeEles(objs);
		    boolean finalCls = seriaFinalClass(objs);
		    
		    if(sameElt && finalCls) {
		    	Class firstCls = Array.get(objs, 0).getClass();
		    	flag |= cn.jmicro.common.Constants.HEADER_ELETMENT;
		    	Short code = cn.jmicro.api.codec.TypeCoderFactory.getIns().getCodeByClass(firstCls);
		    	writeEvery = false;
		    	if(code == null) {
		    		 flag |= cn.jmicro.common.Constants.ELEMENT_TYPE_CODE;
		    		 buffer.writeUTF(firstCls.getName());
		    	} else {
		    		 buffer.writeShort(code);
		    	}
		    } else {
		    	writeEvery = true;
		    }
			
			ITypeCoder coder = TypeCoderFactoryUtils.getIns().getDefaultCoder();
			for(int i = 0; i < len; i++){
				Object v = Array.get(objs, i);
				
				if(writeEvery) {
					if(v == null) {
						 buffer.writeByte(DecoderConstant.PREFIX_TYPE_NULL);
						 continue;
					} else {
						Short code = cn.jmicro.api.codec.TypeCoderFactory.getIns().getCodeByClass(v.getClass());
						if(code == null) {
							 buffer.writeByte(DecoderConstant.PREFIX_TYPE_FULL_CLASS_STRING_NAME);
				    		 buffer.writeUTF(v.getClass().getName());
				    	} else {
				    		 buffer.writeByte(DecoderConstant.PREFIX_TYPE_SHORT);
				    		 buffer.writeShort(code);
				    	}
					}
				}
				
				if(v == null) {
					buffer.writeByte(DecoderConstant.PREFIX_TYPE_NULL);
					continue;
				}
				
				coder.encode(buffer, v, v.getClass(), null);
			}
			
			//写标志位
			jo.write(flagIndex,flag);
			
		}
		
		public static boolean seriaFinalClass(Object arrays) {

			if(arrays == null) {
				return false;
			}
			 int len = Array.getLength(arrays);
			 if(len == 0) {
				 return false;
			 }
			 
			 for(int i = 0; i < len; i++) {
				 Object elt = Array.get(arrays, i);
				 if(elt == null) {
					 return false;
				 }
				 boolean f = java.lang.reflect.Modifier.isFinal(elt.getClass().getModifiers()) ||cn.jmicro.api.codec.ISerializeObject.class.isAssignableFrom(elt.getClass());
			     if(!f) {
			    	 return false;
			     }
			 }
			 
			return true;
			
		}
		
		public static boolean hasNullElement(Object arrays) {

			if(arrays == null) {
				return false;
			}
			 int len = Array.getLength(arrays);
			 if(len == 0) {
				 return false;
			 }
			 
			 for(int i = 0; i < len; i++) {
				 Object elt = Array.get(arrays, i);
				 if(elt == null) {
					return true;
				 }
			 }
			 
			return false;
			
		}
		
		
		public static boolean sameArrayTypeEles(Object coll) {
			int len = Array.getLength(coll);
			if(coll == null || len == 0 || len == 1) {
				return true;
			}
			
			Object pre = null;
			int beginIndex = 1;
			for(int i = 0; i < len; i++) {
				pre = Array.get(coll, 0);
				if(pre != null) {
					beginIndex = 1;
					break;
				}
			}
			
			if(pre == null) {
				//all element is null
				return true;
			}
			
			Object cur = null;
			boolean same = true;
			
			for(int i = beginIndex; i < len; i++) {
				cur = Array.get(coll, i);
				if(cur == null) {
					continue;
				}
				if(cur.getClass() != pre.getClass()) {
					same = false;
					break;
				}
				pre = cur;
			}
			return same;
		}
		
		public static ParameterizedType genericType(Type genericType){
			if(!(genericType instanceof ParameterizedType)) {
				genericType=null;
			}
			return (ParameterizedType)genericType;
		}
		
		
		
		/**********************************解码相关代码开始**************************************/
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public static Object decodeByReflect(DataInput buffer, Class<?> cls,Type genericType) {
			
			int m = cls.getModifiers();
			
			if(Modifier.isAbstract(m) || Modifier.isInterface(m) || !Modifier.isPublic(m)){
				//logger.warn("decodeByReflect class [{}] not support decode",cls.getName());
				throw new CommonException("decodeByReflect class not support decode: "+ cls.getName());
			}
			
			Object obj = null;
			try {
				obj = cls.newInstance();
			} catch (InstantiationException | IllegalAccessException e1) {
				throw new CommonException("fail to instance class [" +cls.getName()+"]",e1);
			}
			
			List<Field> fields = loadClassFieldsFromCache(cls);
			
			ITypeCoder coder = TypeCoderFactoryUtils.getIns().getDefaultCoder();
			for(int i =0; i < fields.size(); i++){

				Field f = fields.get(i);
				
				if(Modifier.isTransient(f.getModifiers())) {
					continue;
				}
				try {
					//System.out.println(cls.getName() + "."+f.getName());
					Object v = coder.decode(buffer, f.getType(), f.getGenericType());
					TypeUtils.setFieldValue(obj, v, f);
				} catch (CommonException e) {
					e.setOthers(e.getOthers()+" | fieldName:"+f.getName()+", obj:"+obj.toString());
				    //logger.error("",e);
					throw e;
				}
				
			}
			return obj;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public static Map<Object,Object> decodeMap(DataInput buffer,Class<?> mapType, ParameterizedType paramType){
			int len = 0;
			try {
				len = getLength(buffer);
			} catch (IOException e) {
				//logger.error("decodeMap",e);
				System.err.println(e.getStackTrace());
			}
			if(len <= 0) {
				return Collections.EMPTY_MAP;
			}
			
			Class<?> keyType = TypeUtils.finalParameterType(paramType,0);
			Class<?> valType = TypeUtils.finalParameterType(paramType,1);
			
			Map<Object,Object> map = null;
			ITypeCoder coder = TypeCoderFactoryUtils.getIns().getDefaultCoder();
			if(mapType != null && !Modifier.isAbstract(mapType.getModifiers()) 
					&& Map.class.isAssignableFrom(mapType)) {
				try {
					map = (Map<Object,Object>)mapType.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					//logger.error("",e);
					e.printStackTrace();
				}
			}
			
			if(map == null) {
				map = new HashMap<>();
			}
			
			for(; len > 0; len--) {
				try {
					byte preCode = buffer.readByte();
					Object key =  null;
					if(preCode != DecoderConstant.PREFIX_TYPE_NULL) {
						key = coder.decode(buffer, keyType, null);
					}
					
					Object value =  null;
					preCode = buffer.readByte();
					if(preCode != DecoderConstant.PREFIX_TYPE_NULL) {
						value = coder.decode(buffer, valType, null);
					}
					map.put(key, value);
					
				} catch (IOException e) {
					//logger.error("decodeMap",e);
					e.printStackTrace();
				}
			}
			
			return map;
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public static void decodeCollection(DataInput buffer, Collection coll, Class<?> declareType,Type genericType){
			int len=0;
			try {
				len = getLength(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(len <= 0) {
				return ;
			}
			
			Class<?> valueType = null;
			 if(genericType != null) {
				 valueType = TypeUtils.finalParameterType((ParameterizedType)genericType,0);
			 }
			 
			 ITypeCoder coder = TypeCoderFactoryUtils.getIns().getDefaultCoder();
			 
			for(int i =0; i <len; i++){
				Object v = coder.decode(buffer, valueType, null);
				if(v != null) {
					coll.add(v);
				}
			}
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public static Object decodeArray(DataInput buffer, Class<?> fieldDeclareType, Type genericType){

			/*if(JMicroContext.get().getInt(JMicroContext.REMOTE_INS_ID, 0) == 6) {
				logger.info("Payment service error");
			}*/
			
			int len=0;
			byte flag = 0;
			try {
				len = getLength(buffer);
				if(len <= 0) {
					return null;
				}
				flag = buffer.readByte();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			boolean readEvery = true;
			Class eltType = null;
			if(0 != (cn.jmicro.common.Constants.HEADER_ELETMENT & flag)) {
				readEvery = false;
				if(0 != (cn.jmicro.common.Constants.ELEMENT_TYPE_CODE & flag)) {
					eltType = getType(buffer);
				}else {
					try {
						Short c = buffer.readShort();
						eltType = cn.jmicro.api.codec.TypeCoderFactory.getIns().getClassByCode(new Short(c));
					} catch (IOException e) {
						throw new CommonException("readShort",e);
					}
				}
			}
			
			ITypeCoder coder = TypeCoderFactoryUtils.getIns().getDefaultCoder();
			 
			Object objs = null;
			if(readEvery) {
				objs = Array.newInstance(Object.class, len);
			}else {
				if(Map.class.isAssignableFrom(eltType)) {
					objs = Array.newInstance(Map.class, len);
				}else if(List.class.isAssignableFrom(eltType)) {
					objs = Array.newInstance(List.class, len);
				}else if(Set.class.isAssignableFrom(eltType)) {
					objs = Array.newInstance(Set.class, len);
				}else {
					objs = Array.newInstance(eltType, len);
				}
				
			}
			/*if(!readEvery) {
				if(Map.class.isAssignableFrom(eltType)) {
					objs = Array.newInstance(Map.class, len);
				}else if(List.class.isAssignableFrom(eltType)) {
					objs = Array.newInstance(List.class, len);
				}else if(Set.class.isAssignableFrom(eltType)) {
					objs = Array.newInstance(Set.class, len);
				}else {
					objs = Array.newInstance(eltType, len);
				}
			}*/
			
			for(int i = 0; i < len; i++){
				if(readEvery) {
					try {
						byte prefixCode = buffer.readByte();
						if(prefixCode ==DecoderConstant.PREFIX_TYPE_NULL ) {
							continue;
						}
						if(DecoderConstant.PREFIX_TYPE_FULL_CLASS_STRING_NAME == prefixCode) {
							eltType = getType(buffer);
						} else {
							Short c = buffer.readShort();
							eltType = cn.jmicro.api.codec.TypeCoderFactory.getIns().getClassByCode(new Short(c));
						}
					} catch (IOException e) {
						throw new CommonException("readByte",e);
					}
				}
				Object o = coder.decode(buffer, eltType, null);
				Array.set(objs, i, o);
			}
			
			if(len > 0 && readEvery && sameArrayTypeEles(objs)) {
				Object o = null;
				for(int i = 0; i < len; i++) {
					o = Array.get(objs, i);
					if(o != null) {
						break;
					}
				}
				
				if(o != null) {
					Object narr = Array.newInstance(o.getClass(), len);
					for(int i = 0; i < len; i++) {
						Array.set(narr, i, Array.get(objs, i));
					}
					objs = narr;
				}
				
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
		
		public static void putStringType(DataOutput buffer,String clazz) throws IOException {
			//类型前缀类型
			buffer.write(DecoderConstant.PREFIX_TYPE_FULL_CLASS_STRING_NAME);
			//类名称
			//encodeString(buffer, clazz);
			buffer.writeUTF(clazz);
		}
		
		public static void putLength(DataOutput buffer,int len) throws IOException {
			buffer.writeInt(len);
		}
		
		public static int getLength(DataInput buffer) throws IOException {
			return buffer.readInt();
		}
		
		public static void encodeString(DataOutput buffer,String str) throws IOException{
			buffer.writeUTF(str);
		}
		
		public static Class<?> getType(JDataInput buffer, Class<?> declareFieldType, Type genericType) {
			if(declareFieldType == null || !TypeUtils.isFinal(declareFieldType)) {
				return getType(buffer);
			}
			return declareFieldType;
		}
		
		public static Class<?> getType(DataInput buffer) {

			String clsName="";
			try {
				clsName = buffer.readUTF();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(Utils.isEmpty(clsName)) {
				throw new CommonException("Decode invalid class name!");
			}
			Class<?> cls = null;

			if(clsName.startsWith("[L")) {
				clsName = clsName.substring(2,clsName.length()-1);
				cls = loadClassFromCache(clsName);
				cls = Array.newInstance(cls, 0).getClass();
			} else {
				cls = loadClassFromCache(clsName);
			}
		
		    return cls;
		}
		
		public static Class<?> loadClassFromCache(String clazzName) {
			if(classCache.containsKey(clazzName)) {
				return classCache.get(clazzName);
			} else {
				synchronized(loadingLock) {
					if(classCache.containsKey(clazzName)) {
						return classCache.get(clazzName);
					}
					Class<?> cls=null;
					try {
						//logger.info("Try to lodad class: " + clazzName);
						cls = Thread.currentThread().getContextClassLoader().loadClass(clazzName);
					} catch (Throwable e) {
						try {
							cls = ITypeCoder.class.getClassLoader().loadClass(clazzName);
						} catch (ClassNotFoundException e1) {
							throw new CommonException("class not found:" + clazzName,e);
						}
					}
					if(cls == null) {
						throw new CommonException("class not found:" + clazzName);
					}
					classCache.put(clazzName, cls);
					
					return cls;
				}
			}
			
		}
		
		public static List<Field> loadClassFieldsFromCache(Class<?> cls) {
			if(classFieldsCache.containsKey(cls)) {
				return classFieldsCache.get(cls);
			}
			synchronized(loadingFieldLock) {
				if(classFieldsCache.containsKey(cls)) {
					return classFieldsCache.get(cls);
				}
				List<Field> fields = new ArrayList<>();
				//long time = System.currentTimeMillis();
				Utils.getIns().getFields(fields, cls);
				//System.out.println("Get Fields time:"+(System.currentTimeMillis()-time));
				if(!fields.isEmpty()) {
					//time = System.currentTimeMillis();
					//fields.sort((v1, v2) -> v1.getName().compareTo(v2.getName()));
					sort(fields,0,fields.size()-1);
					//System.out.println("Sort Fields time:"+(System.currentTimeMillis()-time));
				}
				classFieldsCache.put(cls, fields);
				return fields;
			}
			
		}
		
		public static void sort(List<Field> list, int low, int high){
		    //start是list的第一位，end是list的最后一位，start和end都是list的坐标；
		        int start = low;
		        int end = high;
		        //value作为参考值，取未排序的list第一位value的首字母作为参考
		        //下方的算法大体思路，就是拿list的第一位和value比较，排序，
		        //value值前都比value小，value值后都比value大
		        String key = list.get(low).getName();

		        //char valueStart = list.get(start).charAt(0);
		        //char valueEnd = list.get(end).charAt(0);

		        while(end>start){
		            //从后往前比较
		            //list.get(end).charAt(0)是list最后一个值的首字母
		            while(end>start && list.get(end).getName().compareTo(key) >= 0) {
		                end--;
		            }
		            if(list.get(end).getName().compareTo(key)<=0){
		                //此时list第一位和最后一位需要调换位置，先将list第一位的值保存起来
		                Field keyStarts = list.get(start);
		                //此处调换位置，使用list的set方法，由于第一位放入了最后一个值，
		                //所以最后一位需要放入之前的第一位的值
		                list.set(start, list.get(end));
		                list.set(end, keyStarts);
		            }
		            //从前往后比较
		            while(end>start && list.get(start).getName().compareTo(key)<=0) start++;
		            if(list.get(start).getName().compareTo(key) >= 0){
		                // 同理从后往前比较，需要将第一位的值先保存，方便调换
		                Field keyStarts = list.get(start);
		                list.set(start, list.get(end));
		                list.set(end, keyStarts);
		            }

		        if(start > low) sort(list, low, start-1);
		        if(end < high) sort(list, end+1, high);
		    }
		}

		public static void encodeListElement(DataOutput buffer, Object val) throws IOException {
			//val impossible to be null
			Class valCls = val.getClass();

			if(valCls == byte.class || valCls == Byte.TYPE || valCls == Byte.class ) {
				buffer.writeByte((byte)val);
				return;
			}else if(valCls == short.class || valCls == Short.TYPE || valCls == Short.class ) {
				buffer.writeShort((short)val);
				return;
			}else if(valCls == int.class || valCls == Integer.TYPE || valCls == Integer.class ) {
				buffer.writeInt((int)val);
				return;
			}else if(valCls == long.class || valCls == Long.TYPE || valCls == Long.class ) {
				buffer.writeLong((long)val);
				return;
			}else if(valCls == float.class || valCls == Float.TYPE || valCls == Float.class ) {
				buffer.writeFloat((float)val);
				return;
			}else if(valCls == double.class || valCls == Double.TYPE || valCls == Double.class ) {
				//buffer.write(Decoder.PREFIX_TYPE_DOUBLE);
				buffer.writeDouble((double)val);
				return;
			}else if(valCls == boolean.class || valCls == Boolean.TYPE || valCls == Boolean.class ) {
				buffer.writeBoolean((boolean)val);
				return;
			}else if(valCls == char.class || valCls == Character.TYPE || valCls == Character.class ) {
				buffer.writeChar((char)val);
				return;
			}else if(valCls == String.class ) {
				buffer.writeUTF((String)val);
				return;
			}else if(valCls == Date.class ) {
				buffer.writeLong(((Date)val).getTime());
				return;
			}
			
			if(val != null && val instanceof ISerializeObject) {
				//System.out.println("Use Instance "+valCls.getName());
				/*buffer.write(Decoder.PREFIX_TYPE_PROXY);
				short code = TypeCoderFactory.getIns().getCodeByClass(valCls);
				buffer.writeShort(code);*/
				((ISerializeObject)val).encode(buffer);
				return;
			} else {
				TypeCoderFactoryUtils.getIns().getDefaultCoder().encode(buffer, val, null, null);
			}
		
		}
		
		public static Object decodeListElement(DataInput buffer, Class valCls) throws IOException {
			//val impossible to be null

			if(valCls == byte.class || valCls == Byte.TYPE || valCls == Byte.class ) {
				return buffer.readByte();
			}else if(valCls == short.class || valCls == Short.TYPE || valCls == Short.class ) {
				return buffer.readShort();
			}else if(valCls == int.class || valCls == Integer.TYPE || valCls == Integer.class ) {
				return buffer.readInt();
			}else if(valCls == long.class || valCls == Long.TYPE || valCls == Long.class ) {
				return buffer.readLong();
			}else if(valCls == float.class || valCls == Float.TYPE || valCls == Float.class ) {
				return buffer.readFloat();
			}else if(valCls == double.class || valCls == Double.TYPE || valCls == Double.class ) {
				return buffer.readDouble();
			}else if(valCls == boolean.class || valCls == Boolean.TYPE || valCls == Boolean.class ) {
				return buffer.readBoolean();
			}else if(valCls == char.class || valCls == Character.TYPE || valCls == Character.class ) {
				return buffer.readChar();
			}else if(valCls == String.class ) {
				return buffer.readUTF();
			}else if(valCls == Date.class ) {
				return buffer.readLong();
			}
			
			if(ISerializeObject.class.isAssignableFrom(valCls)) {
				//System.out.println("Use Instance "+valCls.getName());
				/*buffer.write(Decoder.PREFIX_TYPE_PROXY);
				short code = TypeCoderFactory.getIns().getCodeByClass(valCls);
				buffer.writeShort(code);*/
				Object val;
				try {
					val = valCls.newInstance();
					((ISerializeObject)val).decode(buffer);
					return val;
				} catch (InstantiationException | IllegalAccessException e) {
					e.printStackTrace();
					return null;
				}
			} else {
				return TypeCoderFactoryUtils.getIns().getDefaultCoder().decode(buffer, valCls, null);
			}
		
		}
		
		/*******************************STATIC METHOD END****************************/
}
