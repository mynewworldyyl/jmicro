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

/**
 * 1M: 1分钟内，时间单位参考：@link org.jmicro.api.registry.ServiceItem
 * 50%: 发生的异常数超过总请求数的50%
 * 500MS: 熔断后，每间隔500毫秒对接口做一次测试(使用testingArg参数)，测试成功率超过50%，则关闭熔断器
   *     值为空时，不启用
 * 1M 50% 500MS
 * @author Yulei Ye
 * @date 2018年12月10日 上午9:48:12
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface SBreakingRule {

	/**
	 * 是否启用规则
	 * @return
	 */
	public boolean enable() default false;
	
	/**
	 * 统计时间窗口长度，默认10秒
	 * @return
	 */
	public long breakTimeInterval() default 2000;
	
	/**
	 * 时间窗口内异常数超50%则熔断服务
	 * 时间窗口内成功率超50%则恢复服务
	 * @return
	 */
	public int percent() default 50;
	
	/**
	 * 熔断后每隔多长时间对服务进行自动化的恢复测试
	 * 默认值是timeInMilliseconds的10份之一取整
	 * @return
	 */
	public long checkInterval() default 0;
	
}
