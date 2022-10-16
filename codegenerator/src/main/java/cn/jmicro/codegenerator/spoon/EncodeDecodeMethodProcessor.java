package cn.jmicro.codegenerator.spoon;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.codec.ISerializeObject;
import spoon.processing.AbstractAnnotationProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;

public class EncodeDecodeMethodProcessor extends AbstractAnnotationProcessor<SO,CtClass<?>> {
	
	@Override
	public void process(SO so, CtClass<?> ctClass) {
		
		//实现ISerializeObject接口
		final CtTypeReference<ISerializeObject> seriObjectType = getFactory().Code().createCtTypeReference(ISerializeObject.class);
		ctClass.addSuperInterface(seriObjectType);
		
		createEncodeMethod(ctClass);
		createDecodeMethod(ctClass);
		
		System.out.println(ctClass.toString());
	}

	private void createDecodeMethod(CtClass<?> ctClass) {
		

		final CtCodeSnippetStatement encodeBody = getFactory().Code()
				.createCodeSnippetStatement(getDecodeMethod(ctClass));
		final CtBlock<?> encodeBodyBlock = getFactory().Code().createCtBlock(encodeBody);
	
		Set<ModifierKind> methodModifiers = new HashSet<>();
		methodModifiers.add(ModifierKind.PUBLIC);
		
		final CtTypeReference<Void> returnType = getFactory().Code().createCtTypeReference(Void.TYPE);
		final CtTypeReference<DataInput> dataInputType = getFactory().Code().createCtTypeReference(DataInput.class);
		
		final CtTypeReference<IOException> exception = getFactory().Code().createCtTypeReference(IOException.class);
		Set<CtTypeReference<? extends Throwable>> thrownTypes = new HashSet<>();
		thrownTypes.add(exception);
		
		final CtParameter<DataInput> parameter = getFactory().Core().<DataInput>createParameter();
		parameter.<CtParameter<DataInput>>setType(dataInputType);
		parameter.setSimpleName("__buffer");
		
		CtMethod<Void> encodeMethod = getFactory().createMethod(ctClass, methodModifiers, returnType, "decode",
				Collections.<CtParameter<?>>singletonList(parameter), thrownTypes, encodeBodyBlock);
		
		ctClass.addMethod(encodeMethod);

	}

	private void createEncodeMethod(CtClass<?> ctClass) {
		final CtCodeSnippetStatement encodeBody = getFactory().Code()
				.createCodeSnippetStatement(getEncodeMethod(ctClass));
		final CtBlock<?> encodeBodyBlock = getFactory().Code().createCtBlock(encodeBody);
	
		Set<ModifierKind> methodModifiers = new HashSet<>();
		methodModifiers.add(ModifierKind.PUBLIC);
		
		final CtTypeReference<Void> returnType = getFactory().Code().createCtTypeReference(Void.TYPE);
		final CtTypeReference<DataOutput> dataOuputType = getFactory().Code().createCtTypeReference(DataOutput.class);
		
		final CtTypeReference<IOException> exception = getFactory().Code().createCtTypeReference(IOException.class);
		Set<CtTypeReference<? extends Throwable>> thrownTypes = new HashSet<>();
		thrownTypes.add(exception);
		
		final CtParameter<DataOutput> parameter = getFactory().Core().<DataOutput>createParameter();
		parameter.<CtParameter<DataOutput>>setType(dataOuputType);
		parameter.setSimpleName("__buffer");
		
		CtMethod<Void> encodeMethod = getFactory().createMethod(ctClass, methodModifiers, returnType, "encode",
				Collections.<CtParameter<?>>singletonList(parameter), thrownTypes, encodeBodyBlock);
		
		ctClass.addMethod(encodeMethod);
		
	}

	private static  String getDecodeMethod(CtClass<?> cls){

		//ClassPool cp = ClassPool.getDefault();
		
		StringBuffer sb = new StringBuffer();
		
		CtTypeReference<?> superType = cls.getSuperclass();
		if(superType != null && superType.getTypeDeclaration().hasAnnotation(SO.class)) {
			sb.append("super.decode(__buffer); ");
		}
		
		sb.append(cls.getQualifiedName()).append(" __obj =  this;\n ");
		sb.append(" cn.jmicro.api.codec.JDataInput in = (cn.jmicro.api.codec.JDataInput)__buffer;\n");
		sb.append(" cn.jmicro.api.codec.typecoder.TypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactory.getIns().getDefaultCoder(); \n");
		
		Collection<CtFieldReference<?>> fields = cls.getDeclaredFields();
		if(fields == null || fields.isEmpty()) {
			return sb.toString();
		}
		
		List<CtFieldReference<?>> sortFieldsList = new ArrayList<>();
		sortFieldsList.addAll(fields);
		sortFieldsList.sort((s1,s2)->{
			return s1.getSimpleName().compareTo(s2.getSimpleName());
		});
		
		for(int i = 0; i < sortFieldsList.size(); i++) {
			CtFieldReference<?> f = sortFieldsList.get(i);
			if(f.getModifiers().contains(ModifierKind.TRANSIENT) || f.isStatic() || f.isFinal()) {
				//transient字段不序列化,final不可赋值，static一般为常量，所以也不做序列化
				continue;
			}
			
			Class<?> fd = f.getType().getActualClass();

			sb.append(fd.getName()).append(" __val"+i+"; \n");
			
			String varName = " __val"+i;
			
			if(fd == Integer.TYPE) {
				sb.append(varName).append(" = in.readInt();\n");
			}else if(fd.getName().equals(Integer.class.getName()) ) {
				sb.append(varName).append(" = new Integer(in.readInt());\n");
			}else if(fd.getName().equals(String.class.getName())) {
				sb.append(varName).append(" = __buffer.readUTF();\n");
			}else if(fd == Long.TYPE ) {
				sb.append(varName).append(" = in.readLong();\n");
			}else if(fd.getName().equals(Long.class.getName()) ) {
				sb.append(varName).append(" = new java.lang.Long(in.readLong());\n");
			}else if(fd == Byte.TYPE) {
				sb.append(varName).append(" = in.readByte();\n");
			}else if(fd.getName().equals(Byte.class.getName()) ) {
				sb.append(varName).append(" = new Byte(in.readByte());\n");
			}else if(fd == Short.TYPE ) {
				sb.append(varName).append(" = in.readShort();\n");
			}else if(fd.getName().equals(Short.class.getName())) {
				sb.append(varName).append(" = new Short(in.readShort());\n");
			}else if(fd == Float.TYPE ) {
				sb.append(varName).append(" = in.readFloat();\n");
			}else  if(fd.getName().equals(Float.class.getName()) ) {
				sb.append(varName).append(" = new Float(in.readFloat());\n");
			}else if(fd == Double.TYPE) {
				sb.append(varName).append(" = in.readDouble();\n");
			}else if(fd.getName().equals(Double.class.getName()) ) {
				sb.append(varName).append(" = new Double(in.readDouble());\n");
			}else if(fd == Boolean.TYPE) {
				sb.append(varName).append(" = in.readBoolean();\n");
			}else if(fd.getName().equals(Boolean.class.getName())) {
				sb.append(varName).append(" = new java.lang.Boolean(in.readBoolean());\n");
			}else if(fd.getName().equals(Character.class.getName()) ) {
				sb.append(varName).append(" = new Character(in.readChar());\n");
			}else if(fd == Character.TYPE) {
				sb.append(varName).append(" = in.readChar();\n");
			}else  if(fd.getName().equals(Date.class.getName())) {
				sb.append(" long tv"+i+" = in.readLong(); \n ");
				sb.append(varName).append(" = tv"+i+" == 0L ? null : new java.util.Date(tv"+i+");\n");
			} else {
				//sb.append(" byte preCode"+i+" = in.readByte();\n");
				sb.append(" if(in.readByte() == cn.jmicro.api.codec.DecoderConstant.PREFIX_TYPE_NULL) { "+varName+"=null; } else { \n");
				
				if(fd.isAnnotationPresent(SO.class)) {
					sb.append(varName).append(" = new ").append(fd.getName()).append("();\n");
					sb.append(" ((cn.jmicro.api.codec.ISerializeObject)"+varName+").decode(__buffer);\n }");
				} else {
					sb.append(varName).append(" = (")
					.append(fd.getName()).append(") __coder.decode(__buffer,")
					.append(fd.getName()).append(".class,").append(" null );\n }");
				}
			}
			
			sb.append("\n __obj.").append(f.getSimpleName()).append(" = "+varName+";\n \n");
		}
		return sb.toString();
	
	}
	
	private static String getEncodeMethod(CtClass<?> cls) {
		StringBuffer sb = new StringBuffer();
		
		CtTypeReference<?> superType = cls.getSuperclass();
		if(superType != null && superType.getTypeDeclaration().hasAnnotation(SO.class)) {
			sb.append("super.encode(__buffer);");
		}
		
		sb.append(cls.getQualifiedName()).append(" __obj =  this;\n ");
		sb.append(" cn.jmicro.api.codec.JDataOutput out = (cn.jmicro.api.codec.JDataOutput)__buffer;\n");
		sb.append(" cn.jmicro.api.codec.typecoder.TypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactory.getIns().getDefaultCoder(); \n");
		
		Collection<CtFieldReference<?>> fields = cls.getDeclaredFields();
		if(fields == null || fields.isEmpty()) {
			return sb.toString();
		}
		
		List<CtFieldReference<?>> sortFieldsList = new ArrayList<>();
		sortFieldsList.addAll(fields);
		sortFieldsList.sort((s1,s2)->{
			return s1.getSimpleName().compareTo(s2.getSimpleName());
		});
		
		//CtField[] fields = cls.;
		
		for(int i = 0; i < sortFieldsList.size(); i++) {
			CtFieldReference<?> f = sortFieldsList.get(i);
			if(f.getModifiers().contains(ModifierKind.TRANSIENT) || f.isStatic() || f.isFinal()) {
				//transient字段不序列化,final不可赋值，static一般为常量，所以也不做序列化
				continue;
			}
			
			Class<?> fd = f.getType().getActualClass();
			
			sb.append(" ").append(fd.getName()).append(" __val"+i).append("= __obj.").append(f.getSimpleName()).append(";\n");
			 /*
			  * 字数型空值序列化后变为0值，字符串和字符型NULL值变为""空串,布尔值NULL值变为false
			  */
			if(fd == Integer.TYPE) {
				sb.append(" out.writeInt(").append(" __val"+i).append("); \n");
			}else if(fd == Integer.class) {
				sb.append(" out.writeInt(").append(" __val"+i).append(" == null ? new java.lang.Integer(0) : __val"+i+"); \n");
			}else if(fd.getName().equals(String.class.getName())) {
				sb.append(" out.writeUTF(").append(" __val"+i).append(" == null ? \"\" : __val"+i+"); \n");
			}else if(fd == Long.TYPE ) {
				sb.append(" out.writeLong(").append(" __val"+i).append("); \n");
			}else if(fd == Long.class) {
				sb.append(" out.writeLong(").append(" __val"+i).append(" == null ? new java.lang.Long((long)0) : __val"+i+"); \n");
			}else if(fd == Byte.TYPE) {
				sb.append(" out.writeByte(").append(" __val").append(i).append("); \n");
			}else if(fd == Byte.class) {
				sb.append(" out.writeByte(").append(" __val"+i).append(" == null ? new java.lang.Byte((byte)0) : __val"+i+"); \n");
			}else if(Short.class == fd) {
				sb.append(" out.writeShort(").append(" __val"+i).append(" == null ? new java.lang.Short((short)0) : __val"+i+"); \n");
			}else if(fd == Short.TYPE) {
				sb.append(" out.writeShort(").append(" __val").append(i).append("); \n");
			}else  if(fd == Float.TYPE) {
				sb.append(" out.writeFloat(").append(" __val").append(i).append("); \n");
			}else  if(fd == Float.class) {
				sb.append(" out.writeFloat(").append(" __val"+i).append(" == null ? new java.lang.Float((double)0) : __val"+i+"); \n");
			}else if(Double.TYPE == fd) {
				sb.append(" out.writeDouble(").append(" __val").append(i).append("); \n");
			}else if(Double.class == fd) {
				sb.append(" out.writeDouble(").append(" __val"+i).append(" == null ?  (double)0: __val"+i+".doubleValue()); \n");
			}else if(fd == Boolean.TYPE ) {
				sb.append(" out.writeBoolean(").append(" __val").append(i).append("); \n");
			}else if(fd == Boolean.class) {
				sb.append(" out.writeBoolean(").append(" __val"+i).append(" == null ? false : __val"+i+".booleanValue()); \n");
			}else if(fd.getName().equals(Character.class.getName()) ) {
				sb.append(" out.writeChar(").append(" __val").append(" == null ? '' : __val"+i+"); \n");
			}else if(fd == Character.TYPE) {
				sb.append(" out.writeChar(").append(" __val").append(i).append("); \n");
			}else  if(fd == Date.class) {
				sb.append("if(__val"+i+" == null)  __buffer.writeLong(0L) ;") ;
				sb.append(" else out.writeLong(").append(" __val").append(i).append(".getTime()); \n");
			}else {
				sb.append("if(__val"+i+" == null){  out.write(cn.jmicro.api.codec.DecoderConstant.PREFIX_TYPE_NULL); \n} \n") ;
				sb.append(" else { out.write(cn.jmicro.api.codec.DecoderConstant.PREFIX_TYPE_PROXY); \n");
				if(fd.isAnnotationPresent(SO.class)) {
					sb.append("java.lang.Object __o"+i).append("=__val"+i).append("; \n");
					sb.append(" ((cn.jmicro.api.codec.ISerializeObject)__o"+i+").encode(__buffer);\n }");
				} else {
					sb.append(" __coder.encode(__buffer,__val").append(i).append(",").append(fd.getName()).append(".class,").append(" null ); \n }");
				}
			}
			//fieldDeclareType.detach();
			
			sb.append("\n\n");
		}

		return sb.toString();
	}

}