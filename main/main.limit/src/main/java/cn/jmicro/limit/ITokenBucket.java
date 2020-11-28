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
package cn.jmicro.limit;
/**
 * 令牌桶
 * @author Yulei Ye
 * @date 2018年10月17日-上午11:48:53
 */
public interface ITokenBucket {
	
	/**
	 * 申请指定数量的令牌，如果不需要等待则返回0，如果需要等待，则返回实际等待的时间
	 * @param permits
	 * @return 返回申请令牌等待的时间，单位是毫秒（MS）
	 */
	int applyToken(int permits);

	/**
	 * 申请指定数量的令牌需要多少时间，如果不需要等待则返回0。
	 * 等待时间包括等待队列申请许可请求所需要时间。
	 * @param permits
	 * @return 返回申请令牌等待的时间，单位是毫秒（MS）
	 */
	int howLong(int permits);
	
	/**
	 * 更新令牌生成速度，只对更新后的请求生效
	 * @param speed qps
	 */
	void updateSpeed(int speed);
	
}
