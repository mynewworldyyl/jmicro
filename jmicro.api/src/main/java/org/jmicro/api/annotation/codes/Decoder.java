package org.jmicro.api.annotation.codes;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(TYPE)
@Retention(RUNTIME)
public @interface Decoder {

	public String value() default "decoder";
	
	public String method() default "decoder";
	
}
