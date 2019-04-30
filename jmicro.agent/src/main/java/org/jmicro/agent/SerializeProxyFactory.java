package org.jmicro.agent;

import java.io.ByteArrayInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.jmicro.api.annotation.SO;
import org.jmicro.api.codec.Decoder;
import org.jmicro.api.codec.ISerializeObject;
import org.jmicro.api.codec.TypeCoderFactory;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;

public class SerializeProxyFactory {

	public static final Logger logger = LoggerFactory.getLogger(SerializeProxyFactory.class);
	
	public static byte[] getSerializeData(byte[] classData, Class cls,String className) throws IOException, RuntimeException, NotFoundException, CannotCompileException {

		 ClassPool cp = ClassPool.getDefault();
		 CtClass ct = cp.makeClass(new ByteArrayInputStream(classData));
		 if(!ct.hasAnnotation(SO.class)) {
			 return null;
		 }
		 
		 System.out.println(className);
		 
		 ct.addMethod(CtMethod.make(sameCollectionElts(), ct));
		 
		 ct.addInterface(cp.get(ISerializeObject.class.getName()));
		 
		 ct.addMethod(CtMethod.make(getEncodeMethod(ct), ct));
		 
		 ct.addMethod(CtMethod.make(getDecodeMethod(ct), ct));
		 
		 return ct.toBytecode();
		 
	}

	private static  String getDecodeMethod(CtClass cls) throws NotFoundException, CannotCompileException {

		ClassPool cp = ClassPool.getDefault();
		
		StringBuffer sb = new StringBuffer("public Object decode(java.io.DataInput __buffer) {\n");
		
		sb.append(cls.getName()).append(" __obj =  this;\n ");
		
		CtField[] fields = cls.getDeclaredFields();
		if(fields.length == 0 ) {
			return "";
		}
		
		for(int i = 0; i < fields.length; i++) {
			CtField f = fields[i];
			if(Modifier.isTransient(f.getModifiers())) {
				//transient字段不序列化
				continue;
			}
			
			//cls.addField(f);
			
			CtClass fieldDeclareType = f.getType();
			
			if(fieldDeclareType == CtClass.intType || fieldDeclareType.getName().equals(Integer.class.getName()) ) {
				sb.append(f.getType().getName()).append(" __val1 = ").append(" __buffer.readInt();\n");
			}else if(fieldDeclareType.getName().equals(String.class.getName())) {
				sb.append(f.getType().getName()).append(" __val1 = ").append(" __buffer.readUTF();\n");
			}else if(fieldDeclareType == CtClass.longType || fieldDeclareType.getName().equals(Long.class.getName()) ) {
				sb.append(f.getType().getName()).append(" __val1 = ").append(" __buffer.readLong();\n");
			}else if(fieldDeclareType == CtClass.byteType || fieldDeclareType.getName().equals(Byte.class.getName()) ) {
				sb.append(f.getType().getName()).append(" __val1 = ").append(" __buffer.readByte();\n");
			}else if(fieldDeclareType == CtClass.shortType || fieldDeclareType.getName().equals(Short.class.getName()) ) {
				sb.append(f.getType().getName()).append(" __val1 = ").append(" __buffer.readShort();\n");
			}else  if(fieldDeclareType == CtClass.floatType || fieldDeclareType.getName().equals(Float.class.getName()) ) {
				sb.append(f.getType().getName()).append(" __val1 = ").append(" __buffer.readFloat();\n");
			}else if(fieldDeclareType == CtClass.doubleType || fieldDeclareType.getName().equals(Double.class.getName()) ) {
				sb.append(f.getType().getName()).append(" __val1 = ").append(" __buffer.readDouble();\n");
			}else if(fieldDeclareType == CtClass.booleanType || fieldDeclareType.getName().equals(Boolean.class.getName()) ) {
				sb.append(f.getType().getName()).append(" __val1 = ").append(" __buffer.readBoolean();\n");
			}else if(fieldDeclareType == CtClass.charType || fieldDeclareType.getName().equals(Character.class.getName()) ) {
				sb.append(f.getType().getName()).append(" __val1 = ").append(" __buffer.readChar();\n");
			}else  if(fieldDeclareType.getName().equals(Date.class.getName())) {
				sb.append(f.getType().getName()).append(" __val1 = ").append("new java.util.Date(__buffer.readLong());\n");
			} else {
				
				if(!fieldDeclareType.subtypeOf(cp.get(Collection.class.getName())) &&
					!fieldDeclareType.subtypeOf(cp.get(Map.class.getName()))
					&& !fieldDeclareType.isArray()) {
					SO so = null;
					try {
						so = (SO)fieldDeclareType.getAnnotation(SO.class);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}

					if(so != null) {
						sb.append(f.getType().getName()).append(" __val1 = new ").append(f.getType().getName()).append("();\n");
						sb.append(" ((org.jmicro.api.codec.ISerializeObject)__val1).decode(__buffer);\n");
					} else {
						sb.append(" org.jmicro.api.codec.typecoder.TypeCoder __coder = org.jmicro.api.codec.TypeCoderFactory.getDefaultCoder();\n\n");
						sb.append(fieldDeclareType.getName()).append(" __val1 = (")
						.append(fieldDeclareType.getName()).append(") __coder.decode(__buffer,")
						.append(fieldDeclareType.getName()).append(".class,").append(" null );\n");
					}
				} else {
					sb.append(" org.jmicro.api.codec.typecoder.TypeCoder __coder = org.jmicro.api.codec.TypeCoderFactory.getDefaultCoder();\n\n");
					sb.append(" java.lang.reflect.Field f = ").append("this.getClass().getDeclaredField(\"")
					.append(f.getName()).append("\");\n");
					sb.append(f.getType().getName()).append(" __val1 = ").append(" __coder.decode(__buffer,")
					.append(f.getType().getName()).append(".class,").append(" f.getGenericType() );\n");
					}
				
			}
			sb.append("__obj.").append(f.getName()).append(" = __val1;\n");
		}
		
		sb.append("return __obj; \n}\n");
		
		return sb.toString();
	
	}

	private static String getEncodeMethod(CtClass cls) throws NotFoundException, CannotCompileException {
		StringBuffer sb = new StringBuffer("public void encode(java.io.DataOutput __buffer,Object obj) { \n");
		sb.append(cls.getName()).append(" __obj =  this;\n ");
		
		ClassPool cp = ClassPool.getDefault();
		
		/*List<Field> fields = new ArrayList<>();
		Utils.getIns().getFields(fields, cls);
		if(fields.isEmpty()) {
			return "";
		}*/
		
		CtField[] fields = cls.getDeclaredFields();
		
		for(int i = 0; i < fields.length; i++) {
			CtField f = fields[i];
			if(Modifier.isTransient(f.getModifiers())) {
				//transient字段不序列化
				continue;
			}
			
			CtClass fieldDeclareType = f.getType();
			
			//cls.addField(f);
			
			sb.append(" ").append(fieldDeclareType.getName()).append(" __val"+i).append("= __obj.").append(f.getName()).append(";\n");
			
			if(fieldDeclareType == CtClass.intType || fieldDeclareType.getName().equals(Integer.class.getName()) ) {
				sb.append(" __buffer.writeInt(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType.getName().equals(String.class.getName())) {
				sb.append(" __buffer.writeUTF(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType == CtClass.longType || fieldDeclareType.getName().equals(Long.class.getName()) ) {
				sb.append(" __buffer.writeLong(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType == CtClass.byteType || fieldDeclareType.getName().equals(Byte.class.getName()) ) {
				sb.append(" __buffer.writeByte(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType == CtClass.shortType || fieldDeclareType.getName().equals(Short.class.getName()) ) {
				sb.append(" __buffer.writeShort(").append(" __val").append(i).append("); \n");
			}else  if(fieldDeclareType == CtClass.floatType || fieldDeclareType.getName().equals(Float.class.getName()) ) {
				sb.append(" __buffer.writeFloat(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType == CtClass.doubleType || fieldDeclareType.getName().equals(Double.class.getName()) ) {
				sb.append(" __buffer.writeDouble(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType == CtClass.booleanType || fieldDeclareType.getName().equals(Boolean.class.getName()) ) {
				sb.append(" __buffer.writeBoolean(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType == CtClass.charType || fieldDeclareType.getName().equals(Character.class.getName()) ) {
				sb.append(" __buffer.writeChar(").append(" __val").append(i).append("); \n");
			}else  if(fieldDeclareType.getName().equals(Date.class.getName())) {
				sb.append(" __buffer.writeLong(").append(" __val").append(i).append(".getTime()); \n");
			} else {
				sb.append("if(__val"+i).append(" == null)  { __buffer.write(org.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL);\n} else {\n ");
				
			   if(!fieldDeclareType.subtypeOf(cp.get(Collection.class.getName())) &&
					!fieldDeclareType.subtypeOf(cp.get(Map.class.getName()))
					&& !fieldDeclareType.isArray()) {
				    SO so = null;
					try {
						so = (SO)fieldDeclareType.getAnnotation(SO.class);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}

					if(fieldDeclareType.hasAnnotation(SO.class)) {
						sb.append(" ((org.jmicro.api.codec.ISerializeObject)__val"+i+").encode(__buffer,null);\n");
					} else {
						sb.append(" org.jmicro.api.codec.typecoder.TypeCoder __coder = org.jmicro.api.codec.TypeCoderFactory.getDefaultCoder(); \n");
						sb.append(" __coder.encode(__buffer,__val").append(i).append(",").append(fieldDeclareType.getName()).append(".class,").append(" null );\n\n");
					}
			   } else {
				    sb.append(" org.jmicro.api.codec.typecoder.TypeCoder __coder = org.jmicro.api.codec.TypeCoderFactory.getDefaultCoder(); \n");
				    if(fieldDeclareType.subtypeOf(cp.get(Collection.class.getName()))) {
				    	
				    	sb.append(" org.jmicro.api.codec.JDataOutput out = (org.jmicro.api.codec.JDataOutput)__buffer;\n");
				    	sb.append(" byte flag = 0; \n");
				    	sb.append(" int flagIndex = out.position(); \n");
				    	
				    	if(fieldDeclareType.isInterface() || Modifier.isAbstract(fieldDeclareType.getModifiers())) {
				    		sb.append(" __buffer.writeUTF(__val"+i+".getClass().getName());\n");
				    	}
				    	
				    	sb.append(" int size = __val"+i+".size(); \n");
				    	sb.append(" __buffer.writeShort(size); \n");
				    	
				    	sb.append(" if(size > 0) { //if block1 \n");
				    	
				    	//Ljava/util/Set<Lorg/jmicro/api/test/Person;>
				    	String gs = f.getGenericSignature();
				    	Class genericType = getGenericType(gs);
				    	if(genericType != null && seriaFinalClass(genericType)) {
				    		sb.append(" flag |= 1<<5  ;\n");//能从泛型中能获取到足够的列表元素类型信息
				    		sb.append(" boolean writeEvery = false;\n");
				    	} else {
				    		//从值中获取元素类型信息
				    		sb.append(" boolean sameElt = sameCollectionTypeEles(__val"+i+"); if(sameElt) \n");//是否是同种类型的对象
					    	sb.append(" boolean isFinal = org.jmicro.agent.SerializeProxyFactory.seriaFinalClass(__val"+i+".iterator().next().class);\n");
					    	sb.append(" if(sameElt && isFinal) { \n");
						    	sb.append(" __buffer.writeUTF(__val"+i+".iterator().next().class.getName());\n");
						    	sb.append(" boolean writeEvery = false;\n");
					    	sb.append(" } else { \n");
					    		sb.append(" boolean writeEvery = true;\n");
					    	sb.append(" } ");
				    	}
				    	
				    	sb.append(" if(writeEvery) flag |= 1<<7; \n");//第一个元素 是否是抽象类，sameElt=1时有效
				    		
				    	sb.append(" java.util.Iterator ite = __val"+i+".iterator();\n while(ite.hasNext()) { //loop block2 \n");
				    			//v cannot be null
				    			sb.append(" Object v = ite.next(); \n");
					    		sb.append(" if(writeEvery) {__buffer.writeUTF(v.getClass().getName());\n}");
					    		sb.append(" org.jmicro.agent.SerializeProxyFactory.encodeListElement(__buffer,v); \n");
					    sb.append(" } //end for loop block2 \n ");
				    	sb.append(" } // end if block1 \n ");
				    	
				    	sb.append(" out.write(flagIndex,flag);\n");
				    	
				    }else if(fieldDeclareType.isArray()) {
				    	
				    }else {
				    	//map class
				    	
				    }
				    
/*					sb.append(" try { \n");
					sb.append(" java.lang.reflect.Field __f = ").append("__obj.getClass().getDeclaredField(\"").append(f.getName()).append("\");\n");
					sb.append(" __coder.encode(buffer,__val").append(i).append(",").append(fieldDeclareType.getName()).append(".class,").append(" __f.getGenericType() );\n");
					sb.append(" catch(NoSuchFieldException | SecurityException e) { e.printStackTrace(); }\n");
*/				}
				sb.append(" } //end else block \n");
			}
		}
		
		sb.append("}");
		
		return sb.toString();
	}
	
	private static Class getGenericType(String gs) {
		//Ljava/util/Set<Lorg/jmicro/api/test/Person;>
		if(StringUtils.isEmpty(gs) || !gs.contains("<L") || !gs.endsWith(";>;")) {
			return null;
		}
		
		String clsName = gs.substring(gs.indexOf("<L")+2);
		clsName = clsName.substring(0,clsName.length()-3);
		
		if(StringUtils.isEmpty(gs)) {
			return null;
		}
		try {
			clsName = clsName.replaceAll("/", "\\.");
			return SerializeProxyFactory.class.getClassLoader().loadClass(clsName);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	private static String sameCollectionElts() {
		
		return "private boolean sameCollectionTypeEles(java.util.Collection coll) {\r\n" + 
				"		java.util.Iterator ite = coll.iterator();\r\n" + 
				"		Object pre = null , cur = null;\r\n" + 
				"		boolean same = true;\r\n" + 
				"		if(ite.hasNext()) {\r\n" + 
				"			pre = ite.next();\r\n" + 
				"		}\r\n" + 
				"		\r\n" + 
				"		while(ite.hasNext()) {\r\n" + 
				"			cur = ite.next();\r\n" + 
				"			if(cur.getClass() != pre.getClass()) {\r\n" + 
				"				same = false;\r\n" + 
				"				break;\r\n" + 
				"			}\r\n" + 
				"			pre = cur;\r\n" + 
				"		}\r\n" + 
				"		\r\n" + 
				"		return same;\r\n" + 
				"	}";
	}
	
	private boolean sameCollectionTypeEles(Collection coll) {
		Iterator ite = coll.iterator();
		Object pre = null , cur = null;
		boolean same = true;
		if(ite.hasNext()) {
			pre = ite.next();
		}
		
		while(ite.hasNext()) {
			cur = ite.next();
			if(cur.getClass() != pre.getClass()) {
				same = false;
				break;
			}
			pre = cur;
		}
		
		return same;
	}
	
	private static boolean seriaFinalClass(Class cls) {
		return Modifier.isFinal(cls.getModifiers()) ||
				org.jmicro.api.codec.ISerializeObject.class.isAssignableFrom(cls);
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
			buffer.write(Decoder.PREFIX_TYPE_DOUBLE);
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
		
		if(val instanceof ISerializeObject) {
			//System.out.println("Use Instance "+valCls.getName());
			buffer.write(Decoder.PREFIX_TYPE_PROXY);
			short code = TypeCoderFactory.getCodeByClass(valCls);
			buffer.writeShort(code);
			((ISerializeObject)val).encode(buffer, null);
			return;
		} else {
			TypeCoderFactory.getDefaultCoder().encode(buffer, val, null, null);
		}
	
	}

	

}
