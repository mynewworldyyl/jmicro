package org.jmicro.agent;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
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
import org.jmicro.common.CommonException;
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
		 
		 //ct.addMethod(CtMethod.make(sameCollectionElts(), ct));
		 
		 ct.addInterface(cp.get(ISerializeObject.class.getName()));
		 
		 ct.addMethod(CtMethod.make(getEncodeMethod(ct), ct));
		 
		 ct.addMethod(CtMethod.make(getDecodeMethod(ct), ct));
		 
		 return ct.toBytecode();
		 
	}

	private static  String getDecodeMethod(CtClass cls) throws NotFoundException, CannotCompileException {

		ClassPool cp = ClassPool.getDefault();
		
		StringBuffer sb = new StringBuffer(" public void decode(java.io.DataInput __buffer)  throws java.io.IOException {\n");
		
		sb.append(cls.getName()).append(" __obj =  this;\n ");
		
		sb.append(" org.jmicro.api.codec.JDataInput in = (org.jmicro.api.codec.JDataInput)__buffer;\n");
		
		CtField[] fields = cls.getDeclaredFields();
		if(fields.length == 0 ) {
			return "";
		}
		
		for(int i = 0; i < fields.length; i++) {
			CtField f = fields[i];
			if(Modifier.isTransient(f.getModifiers())  || Modifier.isStatic(f.getModifiers()) 
					 || Modifier.isFinal(f.getModifiers()) ) {
				//transient字段不序列化
				continue;
			}
			
			//cls.addField(f);
			
			CtClass fieldDeclareType = f.getType();
			sb.append(f.getType().getName()).append(" __val"+i+"; \n");
			
			String varName = " __val"+i;
			
			if(fieldDeclareType == CtClass.intType || fieldDeclareType.getName().equals(Integer.class.getName()) ) {
				sb.append(varName).append(" = in.readInt();\n");
			}else if(fieldDeclareType.getName().equals(String.class.getName())) {
				sb.append(varName).append(" = __buffer.readUTF();\n");
			}else if(fieldDeclareType == CtClass.longType ) {
				sb.append(varName).append(" = in.readLong();\n");
			}else if(fieldDeclareType.getName().equals(Long.class.getName()) ) {
				sb.append(varName).append(" = new java.lang.Long(in.readLong());\n");
			}else if(fieldDeclareType == CtClass.byteType || fieldDeclareType.getName().equals(Byte.class.getName()) ) {
				sb.append(varName).append(" = in.readByte();\n");
			}else if(fieldDeclareType == CtClass.shortType || fieldDeclareType.getName().equals(Short.class.getName()) ) {
				sb.append(varName).append(" = in.readShort();\n");
			}else  if(fieldDeclareType == CtClass.floatType || fieldDeclareType.getName().equals(Float.class.getName()) ) {
				sb.append(varName).append(" = in.readFloat();\n");
			}else if(fieldDeclareType == CtClass.doubleType || fieldDeclareType.getName().equals(Double.class.getName()) ) {
				sb.append(varName).append(" = in.readDouble();\n");
			}else if(fieldDeclareType == CtClass.booleanType || fieldDeclareType.getName().equals(Boolean.class.getName()) ) {
				sb.append(varName).append(" = in.readBoolean();\n");
			}else if(fieldDeclareType == CtClass.charType || fieldDeclareType.getName().equals(Character.class.getName()) ) {
				sb.append(varName).append(" = in.readChar();\n");
			}else  if(fieldDeclareType.getName().equals(Date.class.getName())) {
				sb.append(" long tv"+i+" = __buffer.readLong(); \n ");
				sb.append(varName).append(" = tv"+i+" == 0L ? null : new java.util.Date(tv"+i+");\n");
			} else {
				String flagName = "flagName" + i;
				//读一个byte的标志位
				sb.append(" byte "+flagName+" = __buffer.readByte(); \n");
				//sb.append(" System.out.println(\"Decoder Flag: \" + ").append(flagName).append(");\n");
				sb.append(varName).append("  = null;\n ");

				sb.append(" if(0 != (org.jmicro.common.Constants.NULL_VAL & " + flagName +")) { ");
				sb.append(varName).append("  = null;\n } else { // block0 \n");
				
				if(!fieldDeclareType.subtypeOf(cp.get(Collection.class.getName())) &&
					!fieldDeclareType.subtypeOf(cp.get(Map.class.getName()))
					&& !fieldDeclareType.isArray()) {

					if(fieldDeclareType.hasAnnotation(SO.class)) {
						sb.append(varName).append(" = new ").append(f.getType().getName()).append("();\n");
						sb.append(" ((org.jmicro.api.codec.ISerializeObject)"+varName+").decode(__buffer);\n");
					} else {
						sb.append(" org.jmicro.api.codec.typecoder.TypeCoder __coder = org.jmicro.api.codec.TypeCoderFactory.getDefaultCoder();\n\n");
						sb.append(varName).append(" = (")
						.append(fieldDeclareType.getName()).append(") __coder.decode(__buffer,")
						.append(fieldDeclareType.getName()).append(".class,").append(" null );\n");
					}
				} else {
					
					sb.append(" org.jmicro.api.codec.typecoder.TypeCoder __coder = org.jmicro.api.codec.TypeCoderFactory.getDefaultCoder(); \n");
				    
					if(fieldDeclareType.subtypeOf(cp.get(Collection.class.getName()))) {
						sb.append(" String clsName=null; short c = 0; \n");
				    	if(fieldDeclareType.isInterface() || Modifier.isAbstract(fieldDeclareType.getModifiers())) {
				    		sb.append(" if(0 != (org.jmicro.common.Constants.TYPE_VAL & "+flagName+")) { \n");
				    		sb.append(" clsName = __buffer.readUTF(); \n");
				    		sb.append(" } \n else { \n");
				    		sb.append(" c = __buffer.readShort(); \n");
				    		sb.append(" } \n");
				    		
				    		sb.append(" if( __obj.").append(f.getName()).append(" == null ) { //block0 \n");
				    		sb.append(" Class cls = null; \n");
				    		sb.append(" if(clsName != null) {  \n");
				    		sb.append("  cls = org.jmicro.agent.SerializeProxyFactory.loadClazz(clsName); \n");
				    		sb.append(" } else {  \n");
				    		sb.append(" cls = org.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c)); \n");
				    		sb.append(" } \n");
				    		sb.append(varName).append("=(").append(fieldDeclareType.getName()).append(") org.jmicro.agent.SerializeProxyFactory.newInstance(cls); \n");
				    		sb.append(" __obj.").append(f.getName()).append(" = ").append(varName).append(";");
				    		sb.append(" } // block0 \n  else {  // block1 \n");
					    	sb.append(varName).append(" = __obj.").append(f.getName()).append(";");
					    	sb.append(" } // block1 \n");
				    	}
				    	
				    	sb.append(" int size = __buffer.readShort(); \n");
				    	sb.append(" if(size > 0) { //block2 \n");
				    	
				    	String gs = f.getGenericSignature();
				    	String genericType = getGenericType(gs);
				    	
				    	sb.append(" boolean readEvery = true;\n ");
				    	sb.append(" Class eleCls = null ;\n  clsName = null; \n c = 0; \n");
				    	
				    	sb.append(" if(0 == (org.jmicro.common.Constants.GENERICTYPEFINAL & "+flagName+")) { //blockgenic \n");
				    	
				    	sb.append(" if(0 != (org.jmicro.common.Constants.HEADER_ELETMENT & "+flagName+")) { \n");
				    	sb.append(" readEvery = false;\n ");
				    	sb.append(" if(0 != (org.jmicro.common.Constants.ELEMENT_TYPE_CODE & "+flagName+")) { \n");
				    	sb.append(" c = __buffer.readShort(); \n");
				    	sb.append(" eleCls = org.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c)); \n");
				    	sb.append(" } \n else { \n");
				    	sb.append(" clsName = __buffer.readUTF(); \n");
				    	sb.append(" eleCls = org.jmicro.agent.SerializeProxyFactory.loadClazz(clsName); \n");
				    	sb.append(" } \n");
				    	
				    	sb.append(" } ");
				    	sb.append(" } //blockgenic \n else { \n ");
				    	sb.append(" eleCls=").append(genericType).append(".class;");
				    	sb.append(" readEvery = false;\n ");
				    	sb.append(" } \n");
			    		
			    		sb.append(" int cnt = 0; \n");
			    		sb.append(" while( cnt < size) { //block5 \n ++cnt; \n");
			    		sb.append(" if(readEvery) { //block6 \n");
			    		sb.append("  c =  __buffer.readShort(); \n");
			    		sb.append(" eleCls = org.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c)); \n ");
			    		sb.append(" } //block6 \n");
			    		sb.append(" Object elt = org.jmicro.agent.SerializeProxyFactory.decodeListElement(__buffer,eleCls); \n");
			    		sb.append(" if(elt != null) { //block7 \n");
			    		sb.append(varName).append(".add(elt); \n");
			    		sb.append(" } //block7 \n");
			    		sb.append(" } //block5 \n");
				    	
				    	sb.append(" } //block2 \n");
				    	
				    } else if(fieldDeclareType.isArray()) {
						sb.append(" String clsName = null; short c = 0; \n");
				    	sb.append(" int size = __buffer.readShort(); \n");
				    	sb.append(" if(size > 0) { //block2 \n");
				    	
				    	CtClass arrEleType = fieldDeclareType.getComponentType();
				    	
				    	String genericType = arrEleType.getName();
				    	
				    	sb.append(" boolean readEvery = true;\n ");
				    	sb.append(" Class eleCls = null ;\n clsName = null; \n c = 0; \n");
				    	
				    	sb.append(" if(0 == (org.jmicro.common.Constants.GENERICTYPEFINAL & "+flagName+")) { //blockgenic,不能从泛型获取足够信息  \n");
				    	
				    	sb.append(" if(0 != (org.jmicro.common.Constants.HEADER_ELETMENT & "+flagName+")) { \n");
				    	sb.append(" readEvery = false;\n ");
				    	sb.append(" if(0 == (org.jmicro.common.Constants.ELEMENT_TYPE_CODE & "+flagName+")) { \n");
				    	sb.append(" c = __buffer.readShort(); \n");
				    	sb.append(" eleCls = org.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c)); \n");
				    	sb.append(" } \n else { \n");
				    	sb.append(" clsName = __buffer.readUTF(); \n");
				    	sb.append(" eleCls = org.jmicro.agent.SerializeProxyFactory.loadClazz(clsName); \n");
				    	sb.append(" } \n");
				    	
				    	sb.append(" } ");
				    	sb.append(" } //blockgenic \n else { \n ");
				    	sb.append(" eleCls = ").append(genericType).append(".class;");
				    	sb.append(" readEvery = false;\n ");
				    	sb.append(" } \n");
				    	
				    	sb.append(varName+" = new ").append(genericType).append("[size];\n");
			    		
			    		sb.append(" for(int i = 0; i < size; i++) { //block5 \n ");
			    		sb.append(" if(readEvery) { //block6 \n");
			    		sb.append("  c =  __buffer.readShort(); \n");
			    		sb.append("  eleCls = org.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c)); \n ");
			    		sb.append(" } //block6 \n");
			    		sb.append(genericType).append(" elt = ("+genericType+") org.jmicro.agent.SerializeProxyFactory.decodeListElement(__buffer,eleCls); \n");
			    		sb.append(" if(elt != null) { //block7 \n");
			    		sb.append(varName).append("[i] = ").append("elt; \n");
			    		sb.append(" } //block7 \n");
			    		sb.append(" } //block5 \n");
				    	
				    	sb.append(" } //block2 \n");
				    	
				    }else if(fieldDeclareType.subtypeOf(cp.get(Map.class.getName()))) {
						sb.append(" String clsName = null; short c = 0; \n");
				    	if(fieldDeclareType.isInterface() || Modifier.isAbstract(fieldDeclareType.getModifiers())) {
				    		sb.append(" if(0 != (org.jmicro.common.Constants.TYPE_VAL & "+flagName+")) { \n");
				    		sb.append(" clsName = __buffer.readUTF(); \n");
				    		sb.append(" } \n else { \n");
				    		sb.append(" c = __buffer.readShort(); \n");
				    		sb.append(" } \n");
				    		
				    		sb.append(" if( __obj.").append(f.getName()).append(" == null ) { //block0 \n");
				    		sb.append(" Class cls = null; \n");
				    		sb.append(" if(clsName != null) {  \n");
				    		sb.append("  cls = org.jmicro.agent.SerializeProxyFactory.loadClazz(clsName); \n");
				    		sb.append(" } else {  \n");
				    		sb.append(" cls = org.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c)); \n");
				    		sb.append(" } \n");
				    		sb.append(varName).append("=(").append(fieldDeclareType.getName()).append(") org.jmicro.agent.SerializeProxyFactory.newInstance(cls); \n");
				    		sb.append(" __obj.").append(f.getName()).append(" = ").append(varName).append(";");
				    		sb.append(" } // block0 \n  else {  // block1 \n");
					    	sb.append(varName).append(" = __obj.").append(f.getName()).append(";");
					    	sb.append(" } // block1 \n");
				    	}
				    	
				    	sb.append(" int size = __buffer.readShort(); \n");
				    	sb.append(" if(size > 0) { //block2 \n");
				    	
				    	//Ljava/util/Map<Ljava/lang/String;Lorg/jmicro/api/test/Person;>;
				    	String gs = f.getGenericSignature();
				    	String[] genericType = getMapGenericType(gs);
				    	
				    	String keyType = genericType.length == 2? genericType[0]:null;
				    	String valType = genericType.length == 2? genericType[1]:null;
				    	
				    	sb.append(" boolean readKeyEvery = false;\n  boolean readValEvery = false;\n");
				    	
				    	sb.append(" Class keyEleCls = null ;\n String keyClsName = null; \n c = 0; \n");
				    	
				    	sb.append(" if(0 == (org.jmicro.common.Constants.GENERICTYPEFINAL & "+flagName+")) { //blockgenic 从头部中读取类型信息 \n");
				    	
				    	sb.append(" if(0 != (org.jmicro.common.Constants.HEADER_ELETMENT & "+flagName+")) { \n");
				    	sb.append(" readKeyEvery = false;\n ");
				    	sb.append(" if(0 != (org.jmicro.common.Constants.ELEMENT_TYPE_CODE & "+flagName+")) { \n");
				    	sb.append(" c = __buffer.readShort(); \n");
				    	sb.append(" keyEleCls = org.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c)); \n");
				    	sb.append(" } \n else { \n");
				    	sb.append(" keyClsName = __buffer.readUTF(); \n");
				    	sb.append(" keyEleCls = org.jmicro.agent.SerializeProxyFactory.loadClazz(keyClsName); \n");
				    	sb.append(" } \n");
				    	
				    	sb.append(" } ");
				    	sb.append(" } //blockgenic \n else { \n ");
				    	sb.append(" keyEleCls=").append(keyType).append(".class;");
				    	sb.append(" readKeyEvery = false; \n ");
				    	sb.append(" } \n");
			    		
				    	
				    	sb.append(" Class valEleCls = null ;\n String valClsName = null; \n c = 0; \n");
				    	sb.append(" if(0 == (org.jmicro.common.Constants.EXT0 & "+flagName+")) { //blockgenic 不能从泛型参数获取类型信息 \n");
				    	
				    	sb.append(" if(0 != (org.jmicro.common.Constants.EXT1 & "+flagName+")) { \n");
				    	sb.append(" readValEvery = false;\n ");
				    	sb.append(" if(0 != (org.jmicro.common.Constants.SIZE_NOT_ZERO & "+flagName+")) { \n");
				    	sb.append(" c = __buffer.readShort(); \n");
				    	sb.append(" valEleCls = org.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c)); \n");
				    	sb.append(" } \n else { \n");
				    	sb.append(" valClsName = __buffer.readUTF(); \n");
				    	sb.append(" valEleCls = org.jmicro.agent.SerializeProxyFactory.loadClazz(valClsName); \n");
				    	sb.append(" } \n");
				    	
				    	sb.append(" } else { \n readValEvery = true; \n }\n");
				    	sb.append(" } //blockgenic \n else { \n ");
				    	sb.append(" valEleCls=").append(valType).append(".class;");
				    	sb.append(" readValEvery = false;\n ");
				    	sb.append(" } \n");
				    	
				    	
			    		sb.append(" int cnt = 0; \n");
			    		sb.append(" while( cnt < size) { //block5 \n ++cnt; \n");
			    		
			    		sb.append(" if(readKeyEvery) { //block6 \n");
			    		sb.append("  c =  __buffer.readShort(); \n");
			    		sb.append(" keyEleCls = org.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c)); \n ");
			    		sb.append(" } //block6 \n");
			    		sb.append(" Object key = org.jmicro.agent.SerializeProxyFactory.decodeListElement(__buffer,keyEleCls); \n");
			    		
			    		sb.append(" if(readValEvery) { //block6 \n");
			    		sb.append("  c =  __buffer.readShort(); \n");
			    		sb.append(" valEleCls = org.jmicro.api.codec.TypeCoderFactory.getClassByCode(new Short(c)); \n ");
			    		sb.append(" } //block6 \n");
			    		sb.append(" Object val = org.jmicro.agent.SerializeProxyFactory.decodeListElement(__buffer,valEleCls); \n");
			    		
			    		sb.append(" if(key != null) { //block7 \n");
			    		sb.append(varName).append(".put(key,val); \n");
			    		sb.append(" } //block7 \n");
			    		sb.append(" } //block5 \n");
				    	
				    	sb.append(" } //block2 \n");
				    	
				    } else {
				    	throw new CommonException("Not support encode");
				    }
					
				/*	sb.append(" org.jmicro.api.codec.typecoder.TypeCoder __coder = org.jmicro.api.codec.TypeCoderFactory.getDefaultCoder();\n\n");
					sb.append(" java.lang.reflect.Field f = ").append("this.getClass().getDeclaredField(\"")
					.append(f.getName()).append("\");\n");
					sb.append(f.getType().getName()).append(" __val1 = ").append(" __coder.decode(__buffer,")
					.append(f.getType().getName()).append(".class,").append(" f.getGenericType() );\n");*/
					}
				sb.append("} //block0 \n");
			}
			
			sb.append("__obj.").append(f.getName()).append(" = "+varName+";\n\n");
		}
		
		sb.append(" return;\n }\n");
		
		System.out.println("\n\n");
		System.out.println(sb.toString());
		
		return sb.toString();
	
	}

	private static String getEncodeMethod(CtClass cls) throws NotFoundException, CannotCompileException {
		StringBuffer sb = new StringBuffer("public void encode(java.io.DataOutput __buffer,Object obj) throws java.io.IOException { \n");
		sb.append(cls.getName()).append(" __obj =  this;\n ");
		sb.append(" org.jmicro.api.codec.JDataOutput out = (org.jmicro.api.codec.JDataOutput)__buffer;\n");
		sb.append(" org.jmicro.api.codec.typecoder.TypeCoder __coder = org.jmicro.api.codec.TypeCoderFactory.getDefaultCoder(); \n");
		
		ClassPool cp = ClassPool.getDefault();
		
		/*List<Field> fields = new ArrayList<>();
		Utils.getIns().getFields(fields, cls);
		if(fields.isEmpty()) {
			return "";
		}*/
		
		CtField[] fields = cls.getDeclaredFields();
		
		for(int i = 0; i < fields.length; i++) {
			CtField f = fields[i];
			if(Modifier.isTransient(f.getModifiers()) || Modifier.isStatic(f.getModifiers())
				|| Modifier.isFinal(f.getModifiers())) {
				//transient字段不序列化
				continue;
			}
			
			CtClass fieldDeclareType = f.getType();
			
			//cls.addField(f);
			
			sb.append(" ").append(fieldDeclareType.getName()).append(" __val"+i).append("= __obj.").append(f.getName()).append(";\n");
			
			if(fieldDeclareType == CtClass.intType || fieldDeclareType.getName().equals(Integer.class.getName()) ) {
				sb.append(" out.writeInt(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType.getName().equals(String.class.getName())) {
				sb.append(" out.writeUTF(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType == CtClass.longType || fieldDeclareType.getName().equals(Long.class.getName()) ) {
				sb.append(" out.writeLong(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType == CtClass.byteType || fieldDeclareType.getName().equals(Byte.class.getName()) ) {
				sb.append(" out.writeByte(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType == CtClass.shortType || fieldDeclareType.getName().equals(Short.class.getName()) ) {
				sb.append(" out.writeShort(").append(" __val").append(i).append("); \n");
			}else  if(fieldDeclareType == CtClass.floatType || fieldDeclareType.getName().equals(Float.class.getName()) ) {
				sb.append(" out.writeFloat(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType == CtClass.doubleType || fieldDeclareType.getName().equals(Double.class.getName()) ) {
				sb.append(" out.writeDouble(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType == CtClass.booleanType || fieldDeclareType.getName().equals(Boolean.class.getName()) ) {
				sb.append(" out.writeBoolean(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType == CtClass.charType || fieldDeclareType.getName().equals(Character.class.getName()) ) {
				sb.append(" out.writeChar(").append(" __val").append(i).append("); \n");
			}else  if(fieldDeclareType.getName().equals(Date.class.getName())) {
				sb.append("if(__val"+i+" == null)  __buffer.writeLong(0L) ;") ;
				sb.append(" else __buffer.writeLong(").append(" __val").append(i).append(".getTime()); \n");
			}else {
				
			    String flagName = "flag"+i;
			    sb.append(" byte "+flagName+" = 0; \n");
			    sb.append(" int flagIndex"+i+" = out.position(); \n");
			    //在头部为标志位保留一个字节的位置
			    sb.append(" __buffer.writeByte(0); // forward one byte  \n");
			    
				sb.append("if(__val"+i).append(" == null)  { "+flagName+" |= org.jmicro.common.Constants.NULL_VAL; \n out.write(flagIndex"+i+","+flagName+");\n}  ");
				sb.append(" else { //block0 \n ");
				
				//sb.append("System.out.println(\"out:\"+out);\n");
				//sb.append("System.out.println(\"__buffer:\"+__buffer);\n");
				//sb.append("System.out.println(\"__val:\"+__val"+i+");\n");
				//sb.append("System.out.println(\"out:\"+out);\n");
				
			   if(!fieldDeclareType.subtypeOf(cp.get(Collection.class.getName())) &&
					!fieldDeclareType.subtypeOf(cp.get(Map.class.getName()))
					&& !fieldDeclareType.isArray()) {
					if(fieldDeclareType.hasAnnotation(SO.class)) {
						sb.append(" ((org.jmicro.api.codec.ISerializeObject)__val"+i+").encode(__buffer,null);\n");
					} else {
						sb.append(" __coder.encode(__buffer,__val").append(i).append(",").append(fieldDeclareType.getName()).append(".class,").append(" null );\n\n");
					}
					 sb.append(" out.write(flagIndex"+i+","+flagName+");\n");
			   } else {
				   
				    if(fieldDeclareType.subtypeOf(cp.get(Collection.class.getName()))) {
				    	
				    	if(fieldDeclareType.isInterface() || Modifier.isAbstract(fieldDeclareType.getModifiers())) {
				    		sb.append(" Short c = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(__val"+i+".getClass()); \n");
				    		sb.append(" if(c == null) { "+flagName+" |= org.jmicro.common.Constants.TYPE_VAL; \n __buffer.writeUTF(__val"+i+".getClass().getName());\n } \n");
				    		sb.append(" else { \n __buffer.writeShort(c.intValue());} \n");
				    	}
				    	
				    	sb.append(" int size = __val"+i+".size(); \n");
				    	sb.append(" __buffer.writeShort(size); \n");
				    	
				    	sb.append(" if(size > 0) { //if block1 \n");
				    	
				    	sb.append(" boolean writeEvery = false;\n");
				    	
				    	//Ljava/util/Set<Lorg/jmicro/api/test/Person;>
				    	String gs = f.getGenericSignature();
				    	String genericType = getGenericType(gs);
				    	
				    	sb.append("if(").append(genericType==null ? "null" : genericType + ".class").append("!= null && (java.lang.reflect.Modifier.isFinal(" + genericType + ".class.getModifiers()) ||org.jmicro.api.codec.ISerializeObject.class.isAssignableFrom("+genericType+".class))) { \n"); 
				    	sb.append(" "+flagName+" |= org.jmicro.common.Constants.GENERICTYPEFINAL; \n");//能从泛型中能获取到足够的列表元素类型信息
			    		sb.append(" } else { // block2 \n");
			    		//从值中获取元素类型信息
			    		sb.append(" Class firstEltCls = __val"+i+".iterator().next().getClass();\n");
			    		sb.append(" boolean sameElt = org.jmicro.agent.SerializeProxyFactory.sameCollectionTypeEles(__val"+i+"); \n");//是否是同种类型的对象
				    	sb.append(" boolean isFinal = org.jmicro.agent.SerializeProxyFactory.seriaFinalClass(firstEltCls);\n");
				    	
				    	sb.append(" if(sameElt && isFinal) { //block3 \n");
				    		sb.append(" "+flagName+" |= org.jmicro.common.Constants.HEADER_ELETMENT; \n");//第一个元素 是否是抽象类，sameElt=true时有效
					    	//sb.append(" __buffer.writeUTF(__val"+i+".iterator().next().getClass().getName());\n");
				    		sb.append(" writeEvery = false; \n");
				    		sb.append(" Short c"+i+" = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(firstEltCls);\n");
				    		sb.append(" if(c"+i+" == null) {");
				    		sb.append(" "+flagName+" |= org.jmicro.common.Constants.ELEMENT_TYPE_CODE; \n");
				    		sb.append("  __buffer.writeUTF(firstEltCls.getName()); ");
				    		sb.append(" } // \n else { \n");
				    		sb.append(" __buffer.writeShort(c"+i+".intValue());\n");
				    		sb.append(" } ");
				    	sb.append(" } //block3 \n");
				    		sb.append(" else { //block4 \n");
				    		sb.append(" writeEvery = true;\n");
				    	sb.append(" } // block4 \n");
				    	sb.append(" } // block2 \n");
				    		
				    	sb.append(" java.util.Iterator ite = __val"+i+".iterator();\n")
				    	.append(" while(ite.hasNext()) { //loop block5 \n");
				    	//v cannot be null
				    	sb.append(" Object v = ite.next(); \n");
					   // sb.append(" if(writeEvery) {__buffer.writeUTF(v.getClass().getName());\n}");
					    sb.append(" if(writeEvery) { \n Short cc"+i+" = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(v.getClass()); \n");
					    sb.append(" if(cc"+i+"==null) org.jmicro.agent.SerializeProxyFactory.errorToSerializeObjectCode(v.getClass().getName()); \n  else ");
					    sb.append("  __buffer.writeShort(cc"+i+".intValue()); \n");
					    sb.append("  \n } \n");
					    
					    sb.append(" org.jmicro.agent.SerializeProxyFactory.encodeListElement(__buffer,v); \n");
					    sb.append(" } //end for loop block5 \n ");
					    
					   /* sb.append(" System.out.println(\"Endoce Flag: \" + ")
					    .append(flagName).append("+\"    \" + obj.getClass().getName()").append(");\n");*/
					    
					    sb.append(" out.write(flagIndex"+i+","+flagName+");\n");
				    	
					    sb.append(" } // end if block1 \n ");
				    	
				    }else if(fieldDeclareType.isArray()) {
				    	
				    	sb.append(" int size = __val"+i+".length; \n");
				    	sb.append(" __buffer.writeShort(size); \n");
				    	
				    	sb.append(" if(size > 0) { //if block1 \n");
				    	
				    	sb.append(" boolean writeEvery = false;\n");
				    	
				    	//Ljava/util/Set<Lorg/jmicro/api/test/Person;>
				    	CtClass arrEleType = fieldDeclareType.getComponentType();
				    	
				    	String genericType = arrEleType.getName();
				    	
				    	sb.append("if(").append(genericType==null ? "null" : genericType + ".class").append("!= null && (java.lang.reflect.Modifier.isFinal(" + genericType + ".class.getModifiers()) ||org.jmicro.api.codec.ISerializeObject.class.isAssignableFrom("+genericType+".class))) { \n"); 
				    	sb.append(" "+flagName+" |= org.jmicro.common.Constants.GENERICTYPEFINAL; \n");//能从泛型中能获取到足够的列表元素类型信息
			    		sb.append(" } else { // block2 \n");
			    		//从值中获取元素类型信息
			    		sb.append(" boolean sameElt = org.jmicro.agent.SerializeProxyFactory.sameArrayTypeEles(__val"+i+"); \n");//是否是同种类型的对象
				    	sb.append(" boolean isFinal = org.jmicro.agent.SerializeProxyFactory.seriaFinalClass(__val"+i+"[0].getClass());\n");
				    	
				    	sb.append(" if(sameElt && isFinal) { //block3 \n");
				    		sb.append(" "+flagName+" |= org.jmicro.common.Constants.HEADER_ELETMENT; \n");//第一个元素 是否是抽象类，sameElt=true时有效
					    	//sb.append(" __buffer.writeUTF(__val"+i+".iterator().next().getClass().getName());\n");
				    		sb.append(" writeEvery = false; \n");
				    		sb.append(" Short c"+i+" = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(__val"+i+"[0].getClass());\n");
				    		sb.append(" if(c"+i+" == null) {");
				    		sb.append(" "+flagName+" |= org.jmicro.common.Constants.ELEMENT_TYPE_CODE; \n");
				    		sb.append("  __buffer.writeUTF(__val"+i+"[0].getClass().getName()); ");
				    		sb.append(" } // \n else { \n");
				    		sb.append(" __buffer.writeShort(c"+i+".intValue());\n");
				    		sb.append(" } ");
				    	sb.append(" } //block3 \n");
				    		sb.append(" else { //block4 \n");
				    		sb.append(" writeEvery = true;\n");
				    	sb.append(" } // block4 \n");
				    	sb.append(" } // block2 \n");
				    		
				    	//sb.append(" java.util.Iterator ite = __val"+i+".iterator();\n")
				    	sb.append(" for(int i = 0;  i < size; i++) { //loop block5 \n");
				    	//v cannot be null
				    	sb.append(" Object v = __val" + i + "[i];");
					    // sb.append(" if(writeEvery) {__buffer.writeUTF(v.getClass().getName());\n}");
					    sb.append(" if(writeEvery) { \n Short cc"+i+" = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(v.getClass()); \n ");
					    sb.append("if(cc"+i+"==null) org.jmicro.agent.SerializeProxyFactory.errorToSerializeObjectCode(v.getClass().getName()); \n  else ");
					    sb.append("__buffer.writeShort(cc"+i+".intValue());\n}\n");
					    
					    sb.append(" org.jmicro.agent.SerializeProxyFactory.encodeListElement(__buffer,v); \n");
					    sb.append(" } //end for loop block5 \n ");
					    
					   /* sb.append(" System.out.println(\"Endoce Flag: \" + ")
					    .append(flagName).append("+\"    \" + obj.getClass().getName()").append(");\n");*/
					    
					    sb.append(" out.write(flagIndex"+i+","+flagName+");\n");
				    	
					    sb.append(" } // end if block1 \n ");
				    	
				    
				    }else  if(fieldDeclareType.subtypeOf(cp.get(Map.class.getName()))) {
				    	
				    	if(fieldDeclareType.isInterface() || Modifier.isAbstract(fieldDeclareType.getModifiers())) {
				    		sb.append(" Short c = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(__val"+i+".getClass()); \n");
				    		sb.append(" if(c == null) { "+flagName+" |= org.jmicro.common.Constants.TYPE_VAL; \n __buffer.writeUTF(__val"+i+".getClass().getName());\n } \n");
				    		sb.append(" else { \n __buffer.writeShort(c.intValue());} \n");
				    	}
				    	
				    	sb.append(" int size = __val"+i+".size(); \n");
				    	sb.append(" __buffer.writeShort(size); \n");
				    	
				    	sb.append(" if(size > 0) { //if block1 \n");
				    	
				    	sb.append(" boolean writeKeyEvery = false; \n  boolean writeValEvery = false;\n");
				    	
				    	//Ljava/util/Map<Ljava/lang/String;Lorg/jmicro/api/test/Person;>;
				    	String gs = f.getGenericSignature();
				    	String[] genericType = getMapGenericType(gs);
				    	
				    	String keyType = genericType.length == 2? genericType[0]:null;
				    	String valType = genericType.length == 2? genericType[1]:null;
				    	
				    	//encode Key type
				    	sb.append("if(").append(keyType==null ? "null" : keyType + ".class").append("!= null && (java.lang.reflect.Modifier.isFinal(" + keyType + ".class.getModifiers()) ||org.jmicro.api.codec.ISerializeObject.class.isAssignableFrom("+keyType+".class))) { \n"); 
				    	sb.append(" "+flagName+" |= org.jmicro.common.Constants.GENERICTYPEFINAL; //能从泛型中能获取到足够的列表元素类型信息\n");
			    		sb.append(" } else { // block2 \n");
			    		//从值中获取元素类型信息
			    		sb.append(" boolean sameKeyElt = org.jmicro.agent.SerializeProxyFactory.sameCollectionTypeEles(__val"+i+".keySet());//是否是同种类型的对象 \n");
				    	sb.append(" boolean isKeyFinal = org.jmicro.agent.SerializeProxyFactory.seriaFinalClass(__val"+i+".keySet().iterator().next().getClass());\n");
				    	
				    	sb.append(" if(sameKeyElt && isKeyFinal) { //block3 \n");
				    	    sb.append(" Class cls = __val"+i+".keySet().iterator().next().getClass(); \n");
				    		sb.append(" "+flagName+" |= org.jmicro.common.Constants.HEADER_ELETMENT; \n");//第一个元素 是否是抽象类，sameElt=true时有效
					    	//sb.append(" __buffer.writeUTF(__val"+i+".iterator().next().getClass().getName());\n");
				    		sb.append(" writeKeyEvery = false; \n");
				    		sb.append(" Short c"+i+" = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(cls);\n");
				    		sb.append(" if(c"+i+" == null) {");
				    		sb.append(" "+flagName+" |= org.jmicro.common.Constants.ELEMENT_TYPE_CODE; \n");
				    		sb.append("  __buffer.writeUTF(cls.getName()); ");
				    		sb.append(" } // \n else { \n");
				    		sb.append(" __buffer.writeShort(c"+i+".intValue());\n");
				    		sb.append(" } ");
				    	sb.append(" } //block3 \n");
				    		sb.append(" else { //block4 \n");
				    		sb.append(" writeKeyEvery = true;\n");
				    	sb.append(" } // block4 \n");
				    	sb.append(" } // block2 \n");
				    	
				    	
				    	//encode value type
				    	sb.append("if(").append(valType==null ? "null" : valType + ".class").append("!= null && (java.lang.reflect.Modifier.isFinal(" + valType + ".class.getModifiers()) ||org.jmicro.api.codec.ISerializeObject.class.isAssignableFrom("+valType+".class))) { \n"); 
				    	sb.append(" "+flagName+" |= org.jmicro.common.Constants.EXT0; \n");//能从泛型中能获取到足够的列表元素类型信息
			    		sb.append(" } else { // block2 \n");
			    		//从值中获取元素类型信息
			    		sb.append(" boolean sameValElt = org.jmicro.agent.SerializeProxyFactory.sameCollectionTypeEles(__val"+i+".values()); \n");//是否是同种类型的对象
				    	sb.append(" boolean isValFinal = org.jmicro.agent.SerializeProxyFactory.seriaFinalClass(__val"+i+".values().iterator().next().getClass());\n");
				    	
				    	sb.append(" if(sameValElt && isValFinal) { //block3 \n");
				    		sb.append(" "+flagName+" |= org.jmicro.common.Constants.EXT1;//首值编码 \n");//第一个元素 是否是抽象类，sameElt=true时有效
				    		sb.append(" writeValEvery = false; \n");
				    		sb.append(" Class cls = __val"+i+".keySet().iterator().next().getClass(); \n");
				    		sb.append(" Short c"+i+" = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(cls);\n");
				    		sb.append(" if(c"+i+" == null) {");
				    		sb.append(" "+flagName+" |= org.jmicro.common.Constants.SIZE_NOT_ZERO; //字符串类型名编码 \n ");
				    		sb.append("  __buffer.writeUTF(cls.getName()); ");
				    		sb.append(" } // \n else { \n");
				    		sb.append(" __buffer.writeShort(c"+i+".intValue());\n");
				    		sb.append(" } ");
				    	sb.append(" } //block3 \n");
				    		sb.append(" else { //block4 \n");
				    		sb.append(" writeValEvery = true;\n");
				    	sb.append(" } // block4 \n");
				    	sb.append(" } // block2 \n");
				    	
				    		
				    	sb.append(" java.util.Iterator ite = __val"+i+".keySet().iterator();\n")
				    	.append(" while(ite.hasNext()) { //loop block5 \n");
				    	//v cannot be null
				    	sb.append(" Object key = ite.next(); \n");
				    	sb.append(" Object val = __val"+i+".get(key); \n");
				    	
					   // sb.append(" if(writeEvery) {__buffer.writeUTF(v.getClass().getName());\n}");
					    sb.append(" if(writeKeyEvery) { \n Short cc"+i+" = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(key.getClass()); \n ");
					    sb.append(" if(cc"+i+"==null) org.jmicro.agent.SerializeProxyFactory.errorToSerializeObjectCode(key.getClass().getName()); \n  else \n");
					    sb.append("__buffer.writeShort(cc"+i+".intValue());\n}\n");
					    
					    sb.append(" org.jmicro.agent.SerializeProxyFactory.encodeListElement(__buffer,key); \n");
					    
					    sb.append(" if(writeValEvery) { \n Short cc"+i+" = org.jmicro.api.codec.TypeCoderFactory.getCodeByClass(val.getClass()); \n ");
					    sb.append(" if(cc"+i+"==null) org.jmicro.agent.SerializeProxyFactory.errorToSerializeObjectCode(val.getClass().getName()); \n  else \n");
					    sb.append("__buffer.writeShort(cc"+i+".intValue());\n}\n");
					   
					    sb.append(" org.jmicro.agent.SerializeProxyFactory.encodeListElement(__buffer,val); \n");
					    sb.append(" } //end for loop block5 \n ");
					    
					   /* sb.append(" System.out.println(\"Endoce Flag: \" + ")
					    .append(flagName).append("+\"    \" + obj.getClass().getName()").append(");\n");*/
					    
					    sb.append(" out.write(flagIndex"+i+","+flagName+");\n");
				    	
					    sb.append(" } // end if block1 \n ");
				    	
				    }else {
				    	throw new CommonException("Not support");
				    }
				    
/*					sb.append(" try { \n");
					sb.append(" java.lang.reflect.Field __f = ").append("__obj.getClass().getDeclaredField(\"").append(f.getName()).append("\");\n");
					sb.append(" __coder.encode(buffer,__val").append(i).append(",").append(fieldDeclareType.getName()).append(".class,").append(" __f.getGenericType() );\n");
					sb.append(" catch(NoSuchFieldException | SecurityException e) { e.printStackTrace(); }\n");
*/				}
				sb.append(" } //end else block0 \n");
			}
			sb.append("\n\n");
		}
		
		sb.append("}");
		//System.out.println("\n\n");
		System.out.println(sb.toString());
		return sb.toString();
	}
	
	public static Object newInstance(Class cls) {
		try {
			return cls.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	
	}
	
	public static Class loadClazz(String clsName) {
		if(clsName == null) {
			throw new NullPointerException();
		}
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if(cl == null) {
			cl = SerializeProxyFactory.class.getClassLoader();
		}
		try {
			return cl.loadClass(clsName);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	

	
	private static String getGenericType(String gs) {
		//Ljava/util/Set<Lorg/jmicro/api/test/Person;>
		if(StringUtils.isEmpty(gs) || !gs.contains("<L") || !gs.endsWith(";>;")) {
			return null;
		}
		
		String clsName = gs.substring(gs.indexOf("<L")+2);
		clsName = clsName.substring(0,clsName.length()-3);
		
		if(StringUtils.isEmpty(gs)) {
			return null;
		}

		clsName = clsName.replaceAll("/", "\\.");
		
		return clsName;
	
	}
	
	private static String[] getMapGenericType(String gs) {
		//Ljava/util/Map<Ljava/lang/String;Lorg/jmicro/api/test/Person;>;
		if(StringUtils.isEmpty(gs) || !gs.contains("<L") || !gs.endsWith(";>;")) {
			return null;
		}
		
		String clsName = gs.substring(gs.indexOf("<L")+2);
		clsName = clsName.substring(0,clsName.length()-3);
	
		String[] strs = clsName.split(";");
		
		if(strs == null || strs.length != 2) {
			return null;
		}

		strs[0] = strs[0].replaceAll("/", "\\.");
		
		strs[1] = strs[1].substring(1);
		strs[1] = strs[1].replaceAll("/", "\\.");
		
		return strs;
	
	}
	
	public static void errorToSerializeObjectCode(String clsName) {
		String msg = "Have to call method org.jmicro.api.codec.TypeCoderFactory.registClass(\""+clsName+".class\") to regist serialize class["+clsName+"]";
		throw new CommonException(msg);
	}
	
	public static boolean sameArrayTypeEles(Object[] coll) {
		if(coll == null || coll.length == 0 || coll.length == 1) {
			return true;
		}
		
		Object pre = coll[0];
		Object cur = null;
		boolean same = true;
		
		for(int i = 1; i < coll.length; i++) {
			cur = coll[i];
			if(cur.getClass() != pre.getClass()) {
				same = false;
				break;
			}
			pre = cur;
		}
		return same;
	}
	
	public static boolean sameCollectionTypeEles(Collection coll) {
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
	
	public static boolean seriaFinalClass(Class cls) {
		return java.lang.reflect.Modifier.isFinal(cls.getModifiers()) ||org.jmicro.api.codec.ISerializeObject.class.isAssignableFrom(cls);
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
		
		if(val != null && val instanceof ISerializeObject) {
			//System.out.println("Use Instance "+valCls.getName());
			/*buffer.write(Decoder.PREFIX_TYPE_PROXY);
			short code = TypeCoderFactory.getCodeByClass(valCls);
			buffer.writeShort(code);*/
			((ISerializeObject)val).encode(buffer, null);
			return;
		} else {
			TypeCoderFactory.getDefaultCoder().encode(buffer, val, null, null);
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
			short code = TypeCoderFactory.getCodeByClass(valCls);
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
			return TypeCoderFactory.getDefaultCoder().decode(buffer, valCls, null);
		}
	
	}
	
}
