package cn.jmicro.codegenerator;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

@SupportedAnnotationTypes("cn.jmicro.codegenerator.AsyncClientProxy")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ClientServiceProxyGenerator extends AbstractProcessor {

/*	private Types mTypeUtils;
    private Messager mMessager;
    private Filer mFiler;
    private Elements mElementUtils;*/

	@Override
	public void init(ProcessingEnvironment processingEnvironment) {
	    super.init(processingEnvironment);
		/*mTypeUtils = processingEnvironment.getTypeUtils();
	    mMessager = processingEnvironment.getMessager();
	    mFiler = processingEnvironment.getFiler();
	    mElementUtils = processingEnvironment.getElementUtils();*/
	}
	
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		
		for (Element element : roundEnv.getElementsAnnotatedWith(AsyncClientProxy.class)) {
			 String tn = element.getSimpleName().toString();
			 if(element.getKind() != ElementKind.INTERFACE) {
				 throw new RuntimeException(AsyncClientProxy.class + " only support interface, not suport " + tn);
		     }
			 
			 if(tn.contains("ISimpleRpc")) {
				 System.out.println("ClientServiceProxyGenerator: "+tn);
			 }
			 
			 TypeElement typeElement = (TypeElement)element;
			 generateGatewayInterfaceClass(typeElement);
		     generateInterfaceClass(typeElement);
		     generateImplClass(typeElement);
		     
		}
		return true;
	}


	private void generateGatewayInterfaceClass(TypeElement typeElement) {

		 String tn = typeElement.getSimpleName().toString();
		 String srcTn = typeElement.getQualifiedName().toString();
		 String pkgName = "";
		 
		 int idx = srcTn.lastIndexOf(".");
		 if(idx > 0) {
			 pkgName = srcTn.substring(0,idx) + "." + AsyncClientProxy.PKG_SUBFIX;
		 } else {
			 pkgName = AsyncClientProxy.PKG_SUBFIX;
		 }
		 
		 ClassName supperInterface = ClassName.get(srcTn.substring(0,idx), tn);
		 TypeSpec.Builder clientProxyHolderBuilder = TypeSpec.interfaceBuilder(tn + AsyncClientProxy.INT_GATEWAY_CLASS)
			      .addModifiers(Modifier.PUBLIC)
			      .addSuperinterface(supperInterface);

		 List<? extends Element> childElements = typeElement.getEnclosedElements();
		 childElements.forEach((e)->{
			 if(e.getKind() != ElementKind.METHOD) {
				 return;
			 }
			 ExecutableElement m = (ExecutableElement)e;
			 if(isOriginAsyncMethod(m)) {
				 return ;
			 }
			 
			 clientProxyHolderBuilder.addMethod(addInterfaceMethod(m,true));
			 clientProxyHolderBuilder.addMethod(addInterfaceMethod(m,false));
		 });
		 
	    TypeSpec typeSpec = clientProxyHolderBuilder.build();
	 
	    JavaFile javaFile = JavaFile.builder(pkgName, typeSpec).build();

	    try {
			javaFile.writeTo(processingEnv.getFiler());
		} catch (IOException e1) {
			e1.printStackTrace();
			processingEnv.getMessager().printMessage(Kind.ERROR,e1.toString(),typeElement);
		}
		
	}
	
	private boolean isOriginAsyncMethod(ExecutableElement m) {
		 TypeMirror tm = m.getReturnType();
		 return tm.toString().startsWith("cn.jmicro.api.async.IPromise");
	}

	private void generateImplClass(TypeElement typeElement) {

		 String tn = typeElement.getSimpleName().toString();
		 String srcTn = typeElement.getQualifiedName().toString();
		 String pkgName = "";
		 
		 int idx = srcTn.lastIndexOf(".");
		 if(idx > 0) {
			 pkgName = srcTn.substring(0,idx) + "." + AsyncClientProxy.PKG_SUBFIX;
		 } else {
			 pkgName = AsyncClientProxy.PKG_SUBFIX;
		 }
		
		 String cln = null;
		 if(tn.startsWith("I")) {
			  cln = tn.substring(1) + AsyncClientProxy.IMPL_SUBFIX;
		 } else {
			 cln = tn + AsyncClientProxy.IMPL_SUBFIX;
		 }
		 
		 ClassName supperInterface = ClassName.get(pkgName, tn + AsyncClientProxy.INT_SUBFIX);
		 ClassName supperClass = ClassName.get("cn.jmicro.api.objectfactory", "AbstractClientServiceProxyHolder");
		 
		 TypeSpec.Builder clientProxyHolderBuilder = TypeSpec.classBuilder(cln)
			      .addModifiers(Modifier.PUBLIC)
			      .superclass(supperClass)
			      .addSuperinterface(supperInterface);

		 List<? extends Element> childElements = typeElement.getEnclosedElements();
		 childElements.forEach((e)->{
			 if(e.getKind() != ElementKind.METHOD) {
				 return;
			 }
			 
			 ExecutableElement m = (ExecutableElement)e;
			 if(!isOriginAsyncMethod(m)) {
				 clientProxyHolderBuilder.addMethod(addAsyncClassMethod(m,false));
			 }
			 clientProxyHolderBuilder.addMethod(addSyncClassMethod(m));
			 clientProxyHolderBuilder.addMethod(addAsyncClassMethod(m,true));
			 
		 });
		 
		/* MethodSpec.Builder isValidMethod = MethodSpec.methodBuilder("isReady")
				  .addModifiers(Modifier.PUBLIC,Modifier.ABSTRACT);
		 isValidMethod.returns(TypeName.BOOLEAN);
		 isValidMethod.addCode(codeBlock);*/
		 
	    TypeSpec typeSpec = clientProxyHolderBuilder.build();
	 
	    JavaFile javaFile = JavaFile.builder(pkgName, typeSpec).build();

	    try {
			javaFile.writeTo(processingEnv.getFiler());
		} catch (IOException e1) {
			e1.printStackTrace();
			processingEnv.getMessager().printMessage(Kind.ERROR,e1.toString(),typeElement);
		}
	}

	private MethodSpec addSyncClassMethod(ExecutableElement m) {

	    MethodSpec.Builder builder = MethodSpec.methodBuilder(m.getSimpleName().toString())
		  .addModifiers(Modifier.PUBLIC);
	  
	   ClassName returnTypeName = parseReturnType(m,builder,null);
	  	
	  	String psString = "";
	  	for(VariableElement pe : m.getParameters()) {
	  		builder.addParameter(ParameterSpec.get(pe));
	  		psString = psString + "," + pe.getSimpleName();
	  	}
	  	if(psString.startsWith(",")) {
	  		psString = psString.substring(1);
	  	}
	  	
	  	if(m.getReturnType().getKind() == TypeKind.VOID) {
	  		if(!psString.equals("")) {
	  			if(m.getParameters().size() == 1) {
	  				builder.addCode("this.proxyHolder.invoke($S, null,(java.lang.Object)($L));",m.getSimpleName(),psString);
	  			}else {
	  				builder.addCode("this.proxyHolder.invoke($S,null, $L);",m.getSimpleName(),psString);
	  			}
	  		}else {
	  			builder.addCode("this.proxyHolder.invoke($S,null);",m.getSimpleName());
	  		}
	  	} else {
	  		if(!psString.equals("")) {
	  			if(m.getParameters().size() == 1) {
	  				builder.addCode("return ($L) this.proxyHolder.invoke($S,null, (java.lang.Object)($L));", m.getReturnType().toString(), 
				  			m.getSimpleName(),psString);
	  			} else {
	  				builder.addCode("return ($L) this.proxyHolder.invoke($S,null, $L);",m.getReturnType().toString(), 
				  			m.getSimpleName(),psString);
	  			}
	  		}else {
	  			builder.addCode("return ($L) this.proxyHolder.invoke($S,null);",m.getReturnType().toString(), 
			  			m.getSimpleName());
	  		}
	  	}
	  	
	    return builder.build();
	}

	private MethodSpec addAsyncClassMethod(ExecutableElement m,boolean withContext) {

			String asyncMethodName = m.getSimpleName().toString() + AsyncClientProxy.ASYNC_METHOD_SUBFIX;
		    MethodSpec.Builder builder = MethodSpec.methodBuilder(asyncMethodName)
			  .addModifiers(Modifier.PUBLIC);

		    ClassName promise = ClassName.get("cn.jmicro.api.async", "IPromise");
		   
		    parseReturnType(m,builder,promise);
		    
		  	String psString = "";
		  	for(VariableElement pe : m.getParameters()) {
		  		builder.addParameter(ParameterSpec.get(pe));
		  		psString = psString + "," + pe.getSimpleName();
		  	}
		  	
		  	if(withContext) {
		    	 addContextParameter(builder);
		    	 AnnotationSpec.Builder ab = AnnotationSpec.builder(ClassName.get("cn.jmicro.api.annotation", "WithContext"));
			  	 builder.addAnnotation(ab.build());
		    }
		  	
		  	if(psString.startsWith(",")) {
		  		psString = psString.substring(1);
		  	}
		  	
		  
		  	if(withContext) {
		  		if(!psString.equals("")) {
		  			if(m.getParameters().size() == 1) {
		  				builder.addCode("return this.proxyHolder.invoke($S,context,(java.lang.Object)($L));",asyncMethodName,psString);
		  			}else {
		  				builder.addCode("return this.proxyHolder.invoke($S,context, $L);",asyncMethodName,psString);
		  			}
		  		}else {
		  			builder.addCode("return this.proxyHolder.invoke($S,context);",asyncMethodName);
		  		}
		  	} else {
		  		if(!psString.equals("")) {
		  			if(m.getParameters().size() == 1) {
		  				builder.addCode("return  this.proxyHolder.invoke($S, null, (java.lang.Object)($L));", asyncMethodName,psString);
		  			} else {
		  				builder.addCode("return  this.proxyHolder.invoke($S, null, $L);",asyncMethodName,psString);
		  			}
		  		}else {
		  			builder.addCode("return this.proxyHolder.invoke($S,null);",asyncMethodName);
		  		}
		  	}
		  	 
		  	
		  	/*if(withContext) {
		  		if("".equals(psString)) {
			  		builder.addCode("return cn.jmicro.api.async.PromiseUtils.callService(this, $S,context);",
			  				asyncMethodName);
			  	} else {
			  		if(m.getParameters().size() == 1) {
			  			builder.addCode("return cn.jmicro.api.async.PromiseUtils.callService(this, $S,context, (java.lang.Object)($L));",
					  			m.getSimpleName(),psString);
		  			}else {
		  				builder.addCode("return cn.jmicro.api.async.PromiseUtils.callService(this, $S,context, $L);",
					  			m.getSimpleName(),psString);
		  			}
			  	}
		  	}else {
		  		if("".equals(psString)) {
			  		builder.addCode("return cn.jmicro.api.async.PromiseUtils.callService(this, $S,null);",
				  			m.getSimpleName());
			  	} else {
			  		if(m.getParameters().size() == 1) {
			  			builder.addCode("return cn.jmicro.api.async.PromiseUtils.callService(this, $S,null, (java.lang.Object)($L));",
					  			m.getSimpleName(),psString);
		  			}else {
		  				builder.addCode("return cn.jmicro.api.async.PromiseUtils.callService(this, $S, null, $L);",
					  			m.getSimpleName(),psString);
		  			}
			  	}
		  	}*/
		  
		  	
		    return builder.build();
		
	}

	private ClassName parseReturnType(ExecutableElement m, MethodSpec.Builder builder,ClassName promise) {
		ClassName returnTypeName = null;
		TypeMirror tm = m.getReturnType();
		if(this.isOriginAsyncMethod(m)) {
			DeclaredType returnTm = (DeclaredType)m.getReturnType();
		    TypeElement classTypeElement = (TypeElement) returnTm.asElement();
		    returnTypeName = ClassName.get(classTypeElement);
		    builder.returns(returnTypeName);
		}else if(tm.getKind() == TypeKind.DECLARED) {
    	 	DeclaredType returnTm = (DeclaredType)m.getReturnType();
		    TypeElement classTypeElement = (TypeElement) returnTm.asElement();
		    returnTypeName = ClassName.get(classTypeElement);
		    if(promise != null) {
		    	 ParameterizedTypeName ppromise = ParameterizedTypeName.get(promise, returnTypeName);
		    	 builder.returns(ppromise);
		    } else {
		    	 builder.returns(returnTypeName);
		    }
	    }else if(tm.getKind().isPrimitive()){
	    	TypeKind tk = m.getReturnType().getKind();
	    	if( tk == TypeKind.DOUBLE ) {
	    		if(promise != null) {
	    			 returnTypeName = ClassName.get(Double.class);
			    	 ParameterizedTypeName ppromise = ParameterizedTypeName.get(promise, returnTypeName);
			    	 builder.returns(ppromise);
			    } else {
			    	 builder.returns(Double.TYPE);
			    }
	    	}else if( tk == TypeKind.FLOAT ) {
	    		returnTypeName = ClassName.get(Float.class);
	    		if(promise != null) {
	    			 returnTypeName = ClassName.get(Double.class);
			    	 ParameterizedTypeName ppromise = ParameterizedTypeName.get(promise, returnTypeName);
			    	 builder.returns(ppromise);
			    } else {
			    	 builder.returns(Float.TYPE);
			    }
	    	}else if( tk == TypeKind.INT ) {
	    		returnTypeName = ClassName.get(Integer.class);
	    		if(promise != null) {
	    			 returnTypeName = ClassName.get(Integer.class);
			    	 ParameterizedTypeName ppromise = ParameterizedTypeName.get(promise, returnTypeName);
			    	 builder.returns(ppromise);
			    } else {
			    	 builder.returns(Integer.TYPE);
			    }
	    	}else if( tk == TypeKind.SHORT ) {
	    		returnTypeName = ClassName.get(Short.class);
	    		if(promise != null) {
	    			 returnTypeName = ClassName.get(Short.class);
			    	 ParameterizedTypeName ppromise = ParameterizedTypeName.get(promise, returnTypeName);
			    	 builder.returns(ppromise);
			    } else {
			    	 builder.returns(Short.TYPE);
			    }
	    	}else if( tk == TypeKind.BYTE ) {
	    		returnTypeName = ClassName.get(Byte.class);
	    		if(promise != null) {
	    			 returnTypeName = ClassName.get(Byte.class);
			    	 ParameterizedTypeName ppromise = ParameterizedTypeName.get(promise, returnTypeName);
			    	 builder.returns(ppromise);
			    } else {
			    	 builder.returns(Byte.TYPE);
			    }
	    	}else if( tk == TypeKind.BOOLEAN ) {
	    		returnTypeName = ClassName.get(Boolean.class);
	    		if(promise != null) {
	    			 returnTypeName = ClassName.get(Boolean.class);
			    	 ParameterizedTypeName ppromise = ParameterizedTypeName.get(promise, returnTypeName);
			    	 builder.returns(ppromise);
			    } else {
			    	 builder.returns(Boolean.TYPE);
			    }
	    	}else if( tk == TypeKind.LONG ) {
	    		returnTypeName = ClassName.get(Long.class);
	    		if(promise != null) {
	    			 returnTypeName = ClassName.get(Long.class);
			    	 ParameterizedTypeName ppromise = ParameterizedTypeName.get(promise, returnTypeName);
			    	 builder.returns(ppromise);
			    } else {
			    	 builder.returns(Long.TYPE);
			    }
	    	}else if( tk == TypeKind.CHAR ) {
	    		returnTypeName = ClassName.get(Character.class);
	    		if(promise != null) {
	    			 returnTypeName = ClassName.get(Character.class);
			    	 ParameterizedTypeName ppromise = ParameterizedTypeName.get(promise, returnTypeName);
			    	 builder.returns(ppromise);
			    } else {
			    	 builder.returns(Character.TYPE);
			    }
	    	}
	    } else if(tm.getKind() == TypeKind.ARRAY) {
	    	ArrayType at = (ArrayType) tm;
	    	TypeName tn = ArrayTypeName.get(at);
	    	
	    	if(promise != null) {
		    	 ParameterizedTypeName ppromise = ParameterizedTypeName.get(promise, tn);
		    	 builder.returns(ppromise);
		    } else {
		    	 builder.returns(tn);
		    }
	    	
	    	TypeMirror tma = at.getComponentType();
	    	if(tma.getKind().isPrimitive()) {
	    		if(tma.getKind() == TypeKind.BYTE) {
	    			if(promise != null) {
	    				 returnTypeName = ClassName.get(Byte.class);
				    }
	    		}else if(tma.getKind() == TypeKind.INT) {
	    			if(promise != null) {
	    				 returnTypeName = ClassName.get(Integer.class);
				    }
	    		}
	    	} else {
	    		returnTypeName = ClassName.bestGuess(tm.toString());
	    	}
	    }  else if(tm.getKind() == TypeKind.VOID) {
    		if(promise != null) {
    			 returnTypeName = ClassName.get(Void.class);
		    	 ParameterizedTypeName ppromise = ParameterizedTypeName.get(promise, returnTypeName);
		    	 builder.returns(ppromise);
		    } else {
		    	 builder.returns(Void.TYPE);
		    }
	    } else {
	    	if(promise != null) {
   			 	 returnTypeName = ClassName.get(java.lang.Void.class);
		    	 ParameterizedTypeName ppromise = ParameterizedTypeName.get(promise, returnTypeName);
		    	 builder.returns(ppromise);
		    }
	    }

		return returnTypeName;
	}

	private void generateInterfaceClass(TypeElement typeElement) {
		 String tn = typeElement.getSimpleName().toString();
		 String srcTn = typeElement.getQualifiedName().toString();
		 String pkgName = "";
		 
		 int idx = srcTn.lastIndexOf(".");
		 if(idx > 0) {
			 pkgName = srcTn.substring(0,idx) + "." + AsyncClientProxy.PKG_SUBFIX;
		 } else {
			 pkgName = AsyncClientProxy.PKG_SUBFIX;
		 }
		 
		 ClassName supperInterface = ClassName.get(pkgName, tn + AsyncClientProxy.INT_GATEWAY + AsyncClientProxy.INT_SUBFIX);
		 TypeSpec.Builder clientProxyHolderBuilder = TypeSpec.interfaceBuilder(tn + AsyncClientProxy.INT_SUBFIX)
			      .addModifiers(Modifier.PUBLIC)
			      .addSuperinterface(supperInterface);
			
		 MethodSpec.Builder isValidMethod = MethodSpec.methodBuilder("isReady")
				  .addModifiers(Modifier.PUBLIC,Modifier.ABSTRACT);
		 isValidMethod.returns(TypeName.BOOLEAN);
		 clientProxyHolderBuilder.addMethod(isValidMethod.build());
		 
		 MethodSpec.Builder clientId = MethodSpec.methodBuilder("clientId")
				  .addModifiers(Modifier.PUBLIC,Modifier.ABSTRACT);
		 clientId.returns(TypeName.INT);
		 clientProxyHolderBuilder.addMethod(clientId.build());
		 
		 MethodSpec.Builder getItem = MethodSpec.methodBuilder("getItem")
				  .addModifiers(Modifier.PUBLIC,Modifier.ABSTRACT);
		 getItem.returns(ClassName.get("cn.jmicro.api.registry", "ServiceItemJRso"));
		 clientProxyHolderBuilder.addMethod(getItem.build());
		 
	    TypeSpec typeSpec = clientProxyHolderBuilder.build();
	 
	    JavaFile javaFile = JavaFile.builder(pkgName, typeSpec).build();

	    try {
			javaFile.writeTo(processingEnv.getFiler());
		} catch (IOException e1) {
			e1.printStackTrace();
			processingEnv.getMessager().printMessage(Kind.ERROR,e1.toString(),typeElement);
		}
		
	}

	private MethodSpec addInterfaceMethod(ExecutableElement m,boolean widthContext) {
	  MethodSpec.Builder builder = MethodSpec.methodBuilder(m.getSimpleName().toString() + AsyncClientProxy.ASYNC_METHOD_SUBFIX)
		  .addModifiers(Modifier.PUBLIC,Modifier.ABSTRACT);
	  
	    ClassName promise = ClassName.get("cn.jmicro.api.async", "IPromise");
	    parseReturnType(m, builder, promise);
	    
	    /*
	    ParameterizedTypeName ppromise = null;
	    if( returnTypeName != null ) {
	    	ppromise = ParameterizedTypeName.get(promise, returnTypeName);
	    } else {
	    	ppromise = ParameterizedTypeName.get(promise, ClassName.get(java.lang.Void.class) );
	    }
	  	builder.returns(ppromise);
	  	*/
	  	
	  	List<? extends VariableElement> ps = m.getParameters();
	  	//List<? extends TypeParameterElement> tpes = m.getTypeParameters();
	  	for(VariableElement pe : ps) {
	  		builder.addParameter(ParameterSpec.get(pe));
	  	}
	  	
	  	if(widthContext) {
	  		addContextParameter(builder);
	  		AnnotationSpec.Builder ab = AnnotationSpec.builder(ClassName.get("cn.jmicro.api.annotation", "WithContext"));
	  		builder.addAnnotation(ab.build());
	  	}
	  	
	    return builder.build();
	}

	private void addContextParameter(Builder builder) {
		/*List<TypeName> bounds = new ArrayList<>();
	  	bounds.add(TypeName.get(String.class));
	  	bounds.add(TypeName.get(Object.class));*/
	  	//TypeName cxtType = ParameterizedTypeName.get(Map.class, String.class,Object.class);
	  	builder.addParameter(TypeName.get(Object.class),"context");
	}
	
	
}
