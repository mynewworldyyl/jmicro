/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmicro.api.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-上午11:55:36
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface Cfg {

	/*
	 * value值以“/”开始，以非“/”结束
	 * 对于Map，可以以*号结尾，表示匹配的条目全部放Map中，Key即配置的Key值，字符串类型，通过Map的泛型解析Value的类型
	 */
	public String value();
	
	public boolean required() default false;
	
	public String changeListener() default "";
	
	public boolean defGlobal() default true;
}
