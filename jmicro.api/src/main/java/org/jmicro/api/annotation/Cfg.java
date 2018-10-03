package org.jmicro.api.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.jmicro.common.Constants;

@Target(FIELD)
@Retention(RUNTIME)
public @interface Cfg {

	public String value();
	
	public boolean required() default true;
	
	public String root() default "";
	
	public boolean updatable() default true;
}
