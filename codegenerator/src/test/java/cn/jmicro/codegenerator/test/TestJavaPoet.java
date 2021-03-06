package cn.jmicro.codegenerator.test;

import java.io.IOException;

import javax.lang.model.element.Modifier;

import org.junit.Test;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import cn.jmicro.codegenerator.AsyncClientUtils;

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
	
	@Test
	public void testGetGatewayFullName() throws IOException {
		//String name = AsyncClientUtils.genGatewayServiceName("cn.jmicro.mng.api.genclient.AgentLogService$JMAsyncClientImpl");
		//String name = AsyncClientUtils.genGatewayServiceName("cn.jmicro.mng.api.genclient.IAgentLogService$JMAsyncClient");
		String name = AsyncClientUtils.genGatewayServiceName("cn.jmicro.mng.api.IAgentLogService");
		System.out.println(name);
	}
	
	@Test
	public void testGenAsyncServiceImplName() throws IOException {
		//String name = AsyncClientUtils.genAsyncServiceImplName("cn.jmicro.mng.api.genclient.IAgentLogService$Gateway$JMAsyncClient");
		//String name = AsyncClientUtils.genAsyncServiceImplName("cn.jmicro.mng.api.genclient.IAgentLogService$JMAsyncClient");
		String name = AsyncClientUtils.genAsyncServiceImplName("cn.jmicro.mng.api.IAgentLogService");
		System.out.println(name);
	}
	
	@Test
	public void testGenAsyncServiceName() throws IOException {
		//String name = AsyncClientUtils.genAsyncServiceName("cn.jmicro.mng.api.genclient.IAgentLogService$Gateway$JMAsyncClient");
		//String name = AsyncClientUtils.genAsyncServiceName("cn.jmicro.mng.api.genclient.AgentLogService$JMAsyncClientImpl");
		String name = AsyncClientUtils.genAsyncServiceName("cn.jmicro.mng.api.IAgentLogService");
		System.out.println(name);
	}
	
	@Test
	public void testGenSyncServiceName() throws IOException {
		//String name = AsyncClientUtils.genSyncServiceName("cn.jmicro.mng.api.genclient.IAgentLogService$Gateway$JMAsyncClient");
		String name = AsyncClientUtils.genSyncServiceName("cn.jmicro.mng.api.genclient.AgentLogService$JMAsyncClientImpl");
		//String name = AsyncClientUtils.genSyncServiceName("cn.jmicro.mng.api.genclient.IAgentLogService$Gateway$JMAsyncClient");
		System.out.println(name);
	}
	
}
