package org.jmicro.api.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.jmicro.common.Constants;

@Target(TYPE)
@Retention(RUNTIME)
public @interface Service {

	public String value() default "";
	
	public String registry() default Constants.DEFAULT_REGISTRY;
	
	public int retryCount() default 3;
	
	public String server() default Constants.DEFAULT_SERVER;
	
	public Class<?>[] interfaces() default {};
	
	public String namespace() default Constants.DEFAULT_NAMESPACE;
	
	public String version() default Constants.DEFAULT_VERSION;
	
}
