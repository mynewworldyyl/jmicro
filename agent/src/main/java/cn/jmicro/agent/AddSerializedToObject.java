package cn.jmicro.agent;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.classloader.RpcClassLoader;
import cn.jmicro.api.codec.ISerializeObject;
import cn.jmicro.api.codec.TypeCoderFactory;
import cn.jmicro.api.codec.typecoder.TypeCoder;
import cn.jmicro.common.JmicroClassPool;
import cn.jmicro.common.util.StringUtils;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

public class AddSerializedToObject implements ClassFileTransformer {

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		/*if(className != null)
			System.out.println(className);
		if(classBeingRedefined == null || !classBeingRedefined.isAnnotationPresent(SO.class)) {
			return null;
		}*/
		try {
			return getSerializeData(classfileBuffer, classBeingRedefined,className);
		} catch (IOException | RuntimeException | NotFoundException | CannotCompileException e) {
			e.printStackTrace();
			return null;
		}
	}



	//public final Logger logger = LoggerFactory.getLogger(SerializeProxyFactory.class);
	
	//private static JmicroClassPool cp = new JmicroClassPool(true);
	
	public static byte[] getSerializeData(byte[] classData, Class<?> cls,String className) throws IOException, RuntimeException, NotFoundException, CannotCompileException {

		 ClassLoader cl = Thread.currentThread().getContextClassLoader();
		 
		 JmicroClassPool cp = new JmicroClassPool(true);
		 if(cl != null && (cl instanceof RpcClassLoader)) {
			 cp.appendClassPath(new LoaderClassPath(cl));
		 }
		
		 CtClass ct = cp.makeClass(new ByteArrayInputStream(classData));
		 if(!ct.hasAnnotation(SO.class)) {
			 return null;
		 }
		 
		 //System.out.println(className);
		 
		 //ct.addMethod(CtMethod.make(sameCollectionElts(), ct));
		 
		 ct.addInterface(cp.get(ISerializeObject.class.getName()));
		 
		 ct.addMethod(CtMethod.make(getEncodeMethod(ct), ct));
		 
		 ct.addMethod(CtMethod.make(getDecodeMethod(ct), ct));
		 
		 byte[] data = ct.toBytecode();
		 ct.detach();
		 
		 //cp.release();
		 
		 Thread.currentThread().setContextClassLoader(cl);
		 
		 return data;
		 
	}

	private static  String getDecodeMethod(CtClass cls) throws NotFoundException, CannotCompileException {

		//ClassPool cp = ClassPool.getDefault();
		
		StringBuffer sb = new StringBuffer(" public void decode(java.io.DataInput __buffer)  throws java.io.IOException {\n");
		
		sb.append(cls.getName()).append(" __obj =  this;\n ");
		
		sb.append(" cn.jmicro.api.codec.typecoder.TypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactory.getIns().getDefaultCoder();\n\n");
		
		sb.append(" cn.jmicro.api.codec.JDataInput in = (cn.jmicro.api.codec.JDataInput)__buffer;\n");
		
		CtField[] fields = cls.getDeclaredFields();
		if(fields.length == 0 ) {
			return "";
		}
		
		boolean d = cls.getName().contains("ActInfo");
		
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
			}else if(fieldDeclareType == CtClass.booleanType) {
				sb.append(varName).append(" = in.readBoolean();\n");
			}else if(fieldDeclareType.getName().equals(Boolean.class.getName())) {
				sb.append(varName).append(" = new java.lang.Boolean(in.readBoolean());\n");
			}else if(fieldDeclareType == CtClass.charType || fieldDeclareType.getName().equals(Character.class.getName()) ) {
				sb.append(varName).append(" = in.readChar();\n");
			}else  if(fieldDeclareType.getName().equals(Date.class.getName())) {
				sb.append(" long tv"+i+" = in.readLong(); \n ");
				sb.append(varName).append(" = tv"+i+" == 0L ? null : new java.util.Date(tv"+i+");\n");
			} else {
				//sb.append(" byte preCode"+i+" = in.readByte();\n");
				sb.append(" if(in.readByte() == cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL) { "+varName+"=null; } else { \n");
				
				if(fieldDeclareType.hasAnnotation(SO.class)) {
					sb.append(varName).append(" = new ").append(f.getType().getName()).append("();\n");
					sb.append(" ((cn.jmicro.api.codec.ISerializeObject)"+varName+").decode(__buffer);\n }");
				} else {
					sb.append(varName).append(" = (")
					.append(fieldDeclareType.getName()).append(") __coder.decode(__buffer,")
					.append(fieldDeclareType.getName()).append(".class,").append(" null );\n }");
				}
			}
			
			sb.append("\n __obj.").append(f.getName()).append(" = "+varName+";\n \n");
		}
		
		sb.append(" return;\n }\n");
		
		//System.out.println("\n\n");
		//System.out.println(sb.toString());
		
		/*if(cls.getName().equals("cn.jmicro.api.mng.ConfigNode")) {
			System.out.println("SerializeProxyFactory: "+sb.toString());
		}*/
		
		return sb.toString();
	
	}

	private static String getEncodeMethod(CtClass cls) throws NotFoundException, CannotCompileException {
		StringBuffer sb = new StringBuffer("public void encode(java.io.DataOutput __buffer) throws java.io.IOException { \n");
		sb.append(cls.getName()).append(" __obj =  this;\n ");
		sb.append(" cn.jmicro.api.codec.JDataOutput out = (cn.jmicro.api.codec.JDataOutput)__buffer;\n");
		sb.append(" cn.jmicro.api.codec.typecoder.TypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactory.getIns().getDefaultCoder(); \n");
		
		//ClassPool cp = ClassPool.getDefault();
		
		/*List<Field> fields = new ArrayList<>();
		Utils.getIns().getFields(fields, cls);
		if(fields.isEmpty()) {
			return "";
		}*/
		
		boolean d = cls.getName().contains("ActInfo");
		
		CtField[] fields = cls.getDeclaredFields();
		
		for(int i = 0; i < fields.length; i++) {
			CtField f = fields[i];
			if(Modifier.isTransient(f.getModifiers()) || Modifier.isStatic(f.getModifiers())
				|| Modifier.isFinal(f.getModifiers()) ) {
				//transient字段不序列化,final不可赋值，static一般为常量，所以也不做序列化
				continue;
			}
			
			CtClass fieldDeclareType = f.getType();
			
			/*if(d) {
				System.out.println("SerializeProxyFactory: "+f.getName());
			}*/
			
			//cls.addField(f);
			
			sb.append(" ").append(fieldDeclareType.getName()).append(" __val"+i).append("= __obj.").append(f.getName()).append(";\n");
			 /*
			  * 字数型空值序列化后变为0值，字符串和字符型NULL值变为""空串,布尔值NULL值变为false
			  */
			if(fieldDeclareType == CtClass.intType ) {
				sb.append(" out.writeInt(").append(" __val"+i).append("); \n");
			}else if(fieldDeclareType.getName().equals(Integer.class.getName()) ) {
				sb.append(" out.writeInt(").append(" __val"+i).append(" == null ? new java.lang.Integer(0) : __val"+i+"); \n");
			}else if(fieldDeclareType.getName().equals(String.class.getName())) {
				sb.append(" out.writeUTF(").append(" __val"+i).append(" == null ? \"\" : __val"+i+"); \n");
			}else if(fieldDeclareType == CtClass.longType ) {
				sb.append(" out.writeLong(").append(" __val"+i).append("); \n");
			}else if(fieldDeclareType.getName().equals(Long.class.getName()) ) {
				sb.append(" out.writeLong(").append(" __val"+i).append(" == null ? new java.lang.Long((long)0) : __val"+i+"); \n");
			}else if(fieldDeclareType == CtClass.byteType) {
				sb.append(" out.writeByte(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType.getName().equals(Byte.class.getName()) ) {
				sb.append(" out.writeByte(").append(" __val"+i).append(" == null ? new java.lang.Byte((byte)0) : __val"+i+"); \n");
			}else if(fieldDeclareType.getName().equals(Short.class.getName()) ) {
				sb.append(" out.writeShort(").append(" __val"+i).append(" == null ? new java.lang.Short((short)0) : __val"+i+"); \n");
			}else if(fieldDeclareType == CtClass.shortType) {
				sb.append(" out.writeShort(").append(" __val").append(i).append("); \n");
			}else  if(fieldDeclareType == CtClass.floatType) {
				sb.append(" out.writeFloat(").append(" __val").append(i).append("); \n");
			}else  if(fieldDeclareType.getName().equals(Float.class.getName()) ) {
				sb.append(" out.writeFloat(").append(" __val"+i).append(" == null ? new java.lang.Float((double)0) : __val"+i+"); \n");
			}else if(fieldDeclareType == CtClass.doubleType) {
				sb.append(" out.writeDouble(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType.getName().equals(Double.class.getName()) ) {
				sb.append(" out.writeDouble(").append(" __val"+i).append(" == null ?  (double)0: __val"+i+".doubleValue()); \n");
			}else if(fieldDeclareType == CtClass.booleanType ) {
				sb.append(" out.writeBoolean(").append(" __val").append(i).append("); \n");
			}else if(fieldDeclareType.getName().equals(Boolean.class.getName()) ) {
				sb.append(" out.writeBoolean(").append(" __val"+i).append(" == null ? false : __val"+i+".booleanValue()); \n");
			}else if(fieldDeclareType.getName().equals(Character.class.getName()) ) {
				sb.append(" out.writeChar(").append(" __val").append(" == null ? '' : __val"+i+"); \n");
			}else if(fieldDeclareType == CtClass.charType) {
				sb.append(" out.writeChar(").append(" __val").append(i).append("); \n");
			}else  if(fieldDeclareType.getName().equals(Date.class.getName())) {
				sb.append("if(__val"+i+" == null)  __buffer.writeLong(0L) ;") ;
				sb.append(" else out.writeLong(").append(" __val").append(i).append(".getTime()); \n");
			}else {
				sb.append("if(__val"+i+" == null){  out.write(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_NULL); \n} \n") ;
				sb.append(" else { out.write(cn.jmicro.api.codec.Decoder.PREFIX_TYPE_PROXY); \n");
				if(fieldDeclareType.hasAnnotation(SO.class)) {
					sb.append("java.lang.Object __o"+i).append("=__val"+i).append("; \n");
					sb.append(" ((cn.jmicro.api.codec.ISerializeObject)__o"+i+").encode(__buffer);\n }");
				} else {
					sb.append(" __coder.encode(__buffer,__val").append(i).append(",").append(fieldDeclareType.getName()).append(".class,").append(" null ); \n }");
				}
			}
			//fieldDeclareType.detach();
			
			sb.append("\n\n");
		}
		
		sb.append("}");
		
		//System.out.println("\n\n");
		/*if(d) {
			System.out.println(sb.toString());
		}*/
		return sb.toString();
	}
	
	public static Object newInstance(Class<?> cls) {
		try {
			return cls.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	
	}
	
	public static Class<?> loadClazz(String clsName) {
		if(clsName == null) {
			throw new NullPointerException();
		}
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if(cl == null) {
			cl = AddSerializedToObject.class.getClassLoader();
		}
		Class<?> cls = null;
		if(clsName.startsWith("[L")) {
			clsName = clsName.substring(2,clsName.length()-1);
			//cls = TypeCoder.loadClassFromCache(clsName);
			Class<?> c = TypeCoder.loadClassFromCache(clsName);
			cls = Array.newInstance(c, 0).getClass();
		} else {
			cls =  TypeCoder.loadClassFromCache(clsName);
		}
		return cls;
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
		
		/*try {
			Class<?> cls = ReflectUtils.desc2class(gs);
			System.out.println(cls);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
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
		String msg = "You can call method cn.jmicro.api.codec.TypeCoderFactory.getIns().registClass(\""+clsName+".class\") to regist serialize class["+clsName+"] upgrade performance";
		//throw new CommonException(msg);
	}
	
	public static boolean sameArrayTypeEles(Object arrObj) {
		
		if(!arrObj.getClass().isArray()) {
			return false;
		}
		
		Object[] coll = (Object[])arrObj;
		
		if(coll == null || coll.length == 0 || coll.length == 1) {
			return true;
		}
		
		Object pre = coll[0];
		if(pre == null) {
			return true;
		}
		
		Object cur = null;
		boolean same = true;
		
		for(int i = 1; i < coll.length; i++) {
			cur = coll[i];
			if(cur == null || cur.getClass() != pre.getClass() ) {
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
		
		if(pre == null) {
			return false;
		}
		
		while(ite.hasNext()) {
			cur = ite.next();
			if(cur == null || cur.getClass() != pre.getClass()) {
				same = false;
				break;
			}
			pre = cur;
		}
		
		return same;
	}
	
	public static boolean collHasNullElement(Collection coll) {
		Iterator ite = coll.iterator();
		while(ite.hasNext()) {
			if(ite.next() == null) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean seriaFinalClass(Object arrays) {
		return TypeCoder.seriaFinalClass(arrays);
	}
	
	public static boolean hasNullElement(Object arrays) {
		return TypeCoder.hasNullElement(arrays);
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
			TypeCoderFactory.getIns().getDefaultCoder().encode(buffer, val, null, null);
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
			return TypeCoderFactory.getIns().getDefaultCoder().decode(buffer, valCls, null);
		}
	
	}

}
