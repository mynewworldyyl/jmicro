package org.jmicro.api.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.jmicro.common.Constants;

@Target(TYPE)
@Retention(RUNTIME)
public @interface ObjFactory {

	public String value() default Constants.DEFAULT_OBJ_FACTORY;

}
