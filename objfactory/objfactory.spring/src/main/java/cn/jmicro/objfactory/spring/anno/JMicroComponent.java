package cn.jmicro.objfactory.spring.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.jmicro.common.Constants;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JMicroComponent {

	//如果非空，则byname,否则bytype
	public String value() default "";
	
	public boolean required() default false;
	
	public boolean remoteService() default false;
	
	public String namespace() default "";
	
	public String version() default "";
}
