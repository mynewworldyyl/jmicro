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
package org.jmicro.api.choreography.controller;

import org.jmicro.api.choreography.base.SchedulerResult;

/**
   * 基于Java可运行Jar包的服务管器，只需要是一个可运行Jar即可。本接口结合IServiceAgent实现JVM调度器。
   * 运行实体为核心命令为 java -DjvmOptions -cp classpath mainClass arg1,arg2,arg3,arg4,... argN
   *  除java外的其他选项由配置获取并通过RPC转给IServiceAgent
   *  
 *   运行JVM实例数count,调度器负责确保运行指定数量的JVM
 *   
 *   实例全称前缀instancePrefix,对应JMicro的instanceName前缀，如count=3，instancePrefix=JMicro
 *   3个JMV实例 名分别为 JMicro0,JMicro1,JMicro2
 *   
 * mainClass,如果Jar不是可运行的Jar（MANIFEST.MF文件无Main-class属性），则需要指定mainClass全类名
 * 
 * jvmOptions：定制JVM参数，参考JAVA命令说明
 * 
 * classpath：指定运行所需要的类路径，如果目标机Jar包不存在，则从指定的jar仓库下载。
 * 
 * argN:将直接传给运行应用实例。
 * 
 * ZK配置：
 * 1. IServiceAgent 实例目录，/jmicro/JMICRO_choreography/agents下面挂接全部当前可用的agent，调度器负责将运行指令写到此目录下
 * agent监听到运行指令增加后，负责执行指令。当指令删除后，agent停止特定JVM实例。
 * 2. 当agent启动时，agent会注册自己到，/jmicro/JMICRO_choreography目录，Controller发现有新的Agent加入，则将其例入分配管理
 *     列表，有新的运行命令需求时，优先分配给无运行实例的Agent。
 * 
 * @author Yulei Ye
 * @date 2019年1月22日 下午10:16:38
 */
public interface IServiceController {

	SchedulerResult startByCmd(String cmd, int count, String... args);
	
	SchedulerResult stopById(String id);
	
	SchedulerResult lsServices(String matcher);
	
}
