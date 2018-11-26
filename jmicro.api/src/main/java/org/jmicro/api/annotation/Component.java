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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.jmicro.common.Constants;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-上午11:56:01
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Component {
	/**
	 * 组件名称，必须确保在全局唯一 
	 */
	public String value() default "";
	/**
	 * 使用时才实例化，启动时只是生成代理
	 */
	public boolean lazy() default false;
	
	/**
	 * 实例化优先级，值越底，优先极越高。用户自定义的服务因为依赖于系统的核心组件，所以用户自定义的组件的level值不要太小，建议从10000开始
	 * 如果用户定义的组件A和组件B，B依赖于A，则A的level要大于B，否则B先于A启动，B的依赖没有找到，从而报错
	 */
	public int level() default 10000;
	/**
	 * 组件是否可用，如当前开发了实现相同功能的服务A和B，但是此时不想启用A，可以暂时设置active=false，则IOC容器不会实例化A。
	 * @return
	 */
	public boolean active() default true;
	
	//provider or client or NULL witch can be used any side
	/**
	 * 此组件的使用方，可以是服务提供方或消费方，也可以两方都可以使用，
	 * 如果指定了服务提供方或消费方，则该组件所依赖的组件也被限制为指定方
	 * @return
	 */
	public String side() default Constants.SIDE_ANY; 
}
