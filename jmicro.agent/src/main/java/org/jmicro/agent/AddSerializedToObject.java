package org.jmicro.agent;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import javassist.CannotCompileException;
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
			return SerializeProxyFactory.getSerializeData(classfileBuffer, classBeingRedefined,className);
		} catch (IOException | RuntimeException | NotFoundException | CannotCompileException e) {
			e.printStackTrace();
			return null;
		}
	}

}
