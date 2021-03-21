package cn.jmicro.common;

import java.net.URL;

import cn.jmicro.common.util.StringUtils;
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

	public URL find(String classname) {
		if (StringUtils.isEmpty(classname)) {
			return null;
		}

		int idx = classname.lastIndexOf(".");
		String cn = null;
		if (idx > 0) {
			cn = classname.substring(classname.lastIndexOf(".") + 1);
		} else {
			cn = classname;
		}

		if (cn.charAt(0) <= 'Z' && cn.charAt(0) >= 'A') {
			return super.find(classname);
		}

		return null;
	}
	 
}
