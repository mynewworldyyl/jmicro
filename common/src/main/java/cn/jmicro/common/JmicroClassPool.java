package cn.jmicro.common;

import javassist.ClassPool;

public class JmicroClassPool extends ClassPool {

	public JmicroClassPool() {
		super(null);
	}

	public JmicroClassPool(boolean useDefaultPath) {
		super(useDefaultPath);
	}

	public JmicroClassPool(ClassPool parent) {
		super(parent);
	}
	
	public void release() {
		this.classes.clear();
	}

}
