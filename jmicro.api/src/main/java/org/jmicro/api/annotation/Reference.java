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
 * 在服务接口相同的情况下：
 * 命名空间(namespace)：
 *    表示相同接口实现不同的功能，比如IMonitorSubmitWorker的实现平均响应时间统计，异常次数统计，流量统计等，应该使
 *    用不同命名空间去区别；
 * 版本(version):
 *    实现完全相同功能的一种版本升级，响应时间统计，先后修复不同的Bug，或性能提升等，其实现类名称完全相同，只是配置的版本
 *    号不同，使用调用版本1，和调用 版本2得到的结果远全相同（只要不碰到Bug），
 * 
 * 基于以上分析，对集合作如下解析：
 * 对于集合来说，要使用集合，说明有可能有多于1个的元素引用，此时使用集合才有意义，如果只有一个元素，为什么要使用集合呢？
 * 另一方面，服务名+名称空间+版本，唯一确定一种实现完全相同功能的服务，所以对于集合的引用，不应该使用命名空间做限制，因为
 * 如果使用了命名空间，说明集合只可能存在同一命名空间的不同版本元素（版本号没有限制），同一命名空间不同版本服务视为实现同样
 * 功能的不同实现方式，或只是Bug上的解决，对使用者来说，他们实现的功能是完全相同 ，也就是说集合里存在不同版本的服务没有意义
 * 甚至在某种情况下是重复使用了同一服务，调用了多次。
 * 所以结论是：集合服务引用，不应该加命名空间，并且要增加版本号做限制。但是站框架的角度，又不能对此做硬性限制，所以应该由使用
 * 者根据实际情况去做选择。
 * 如果不遵守这些约定，即使不出问题，相信也是一种设计或实现上的缺陷。
			 
 * @author Yulei Ye
 * @date 2018年10月4日-上午11:57:50
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface Reference {
	public String value() default "";
	
	public String namespace() default "";
	public String version() default "";
	
	public boolean required() default false;
	public String registry() default "";
	public String changeListener() default "";
}
