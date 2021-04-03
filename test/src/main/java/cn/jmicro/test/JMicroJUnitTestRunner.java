package cn.jmicro.test;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;

import cn.jmicro.api.classloader.RpcClassLoader;

public class JMicroJUnitTestRunner extends BlockJUnit4ClassRunner{

	public JMicroJUnitTestRunner(Class<?> klass)  throws InitializationError{
		super(klass);
	}

	@Override
	protected TestClass createTestClass(Class<?> testClass) {
		 RpcClassLoader cl = new RpcClassLoader(testClass.getClassLoader());
		 try {
			 String pn = testClass.getPackage().getName();
			 String[] pns = pn.split("\\.");
			 if(pns.length >= 2) {
				 cl.addBasePackage(pns[0]+"." + pns[1]);
			 }else {
				 cl.addBasePackage(pn);
			 }
			 Thread.currentThread().setContextClassLoader(cl);
			 Class<?> clazz = cl.loadClass(testClass.getName());
			 return new TestClass(clazz);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	
}
