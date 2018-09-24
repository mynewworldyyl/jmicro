package org.jmicro.api.annotation.service;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(TYPE)
@Retention(RUNTIME)
public @interface Service {

	public String value() default "";
	
	public String server() default "defaultServer";
	
	public Class<?>[] interfaces() default {};
	
}
