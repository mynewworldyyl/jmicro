package cn.jmicro.api.monitor;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(FIELD)
@Retention(RUNTIME)
public @interface MCA {
	
	public String value();
	
	public String desc() default "";
	
	public String group() default MC.TYPE_DEF_GROUP;
	
}
