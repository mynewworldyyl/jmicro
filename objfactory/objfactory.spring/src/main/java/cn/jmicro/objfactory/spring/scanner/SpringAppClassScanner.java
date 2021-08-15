package cn.jmicro.objfactory.spring.scanner;

import java.io.IOException;

import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ClassUtils;

public class SpringAppClassScanner {

	static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";
	
	private Environment environment;
	
	private ResourcePatternResolver resourcePatternResolver;
	
	public Resource[] doScann(String basePackage) throws IOException {
		String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
				resolveBasePackage(basePackage) + '/' + DEFAULT_RESOURCE_PATTERN;
		return getResourcePatternResolver().getResources(packageSearchPath);
	}
	
	protected String resolveBasePackage(String basePackage) {
		return ClassUtils.convertClassNameToResourcePath(getEnvironment().resolveRequiredPlaceholders(basePackage));
	}

	public final Environment getEnvironment() {
		if (this.environment == null) {
			this.environment = new StandardEnvironment();
		}
		return this.environment;
	}
	
	private ResourcePatternResolver getResourcePatternResolver() {
		if (this.resourcePatternResolver == null) {
			this.resourcePatternResolver = new PathMatchingResourcePatternResolver();
		}
		return this.resourcePatternResolver;
	}

}
