package org.jmicro.api.annotation.server;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(TYPE)
@Retention(RUNTIME)
public @interface Server {

	public String value() default "defaultServer";
	
	public String port() default "17907";
	
	public String host() default "0.0.0.0";
	
	public String handler() default "handler";
}
