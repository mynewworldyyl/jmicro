package cn.expjmicro.example.test.js;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.util.Date;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Test;

import cn.jmicro.api.test.Person;

public class TestJS {

	@Test
	public void testJSEngine() throws ScriptException {
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
		engine.eval("print('Hello World!');");
	}
	
	@Test
	public void testJSFileEngine() throws ScriptException, FileNotFoundException {
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
		engine.eval(new FileReader("./src/test/java/org/jmicro/example/test/js/script.js"));
	}
	
	
	@Test
	public void testInvokeJSFunction() throws ScriptException, FileNotFoundException, NoSuchMethodException {
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
		engine.eval(new FileReader("./src/test/java/org/jmicro/example/test/js/script.js"));
		
		Invocable invocable = (Invocable) engine;

		Object result = invocable.invokeFunction("fun1", "Peter Parker");
		System.out.println(result);
		System.out.println(result.getClass());
	}
	
	@Test
	public void testInvokeJSFunctionWithJavaObject() throws ScriptException, FileNotFoundException, NoSuchMethodException {
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
		engine.eval(new FileReader("./src/test/java/org/jmicro/example/test/js/script.js"));
		Invocable invocable = (Invocable) engine;
		invocable.invokeFunction("fun2", new Date());
		//[object java.util.Date]
		invocable.invokeFunction("fun2", LocalDateTime.now());
		//[object java.time.LocalDateTime]
		invocable.invokeFunction("fun2", new Person());
	}
	
	@Test
	public void testInvokeJavaObjectMethod() throws ScriptException, FileNotFoundException, NoSuchMethodException {
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
		engine.eval(new FileReader("./src/test/java/org/jmicro/example/test/js/script.js"));
		//Invocable invocable = (Invocable) engine;
	}
	
}
