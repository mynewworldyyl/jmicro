package cn.expjmicro.example.test.js;

public class JavaObject {

	public static String fun1(String name) {
	    System.out.format("Hi there from Java, %s \n", name);
	    return "greetings from java";
	}
	
	public String fun2(String name) {
	    System.out.format("Hi there from Java menber method, %s \n", name);
	    return "Java menber method!";
	}
	
	public static void fun3(Object object) {
	    System.out.println(object.getClass());
	}
}
