package cn.jmicro.codegenerator.test;

import java.io.IOException;

import javax.lang.model.element.Modifier;

import org.junit.Test;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

public class TestJavaPoet {

	private static MethodSpec whatsMyName(String name) {
	  return MethodSpec.methodBuilder(name)
		  .addModifiers(Modifier.PUBLIC,Modifier.FINAL)
	      .returns(String.class)
	      .addStatement("return $S", name)
	      .build();
	}
	
	@Test
	public void testGeneratorMain() throws IOException {
		TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
			      .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
			      .addMethod(whatsMyName("slimShady"))
			      .addMethod(whatsMyName("eminem"))
			      .addMethod(whatsMyName("marshallMathers"))
			      .build();

			  JavaFile javaFile = JavaFile.builder("com.example.helloworld", helloWorld)
			      .build();

			  javaFile.writeTo(System.out);
	}
}
