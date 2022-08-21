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
package cn.jmicro.api.objectfactory;

import java.util.Set;
import java.util.function.Consumer;

import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.masterelection.IMasterChangeListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.AsyncConfigJRso;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.security.ILoginStatusListener;
/**
 * 为JMicro微服务框架量身定制的IOC容器，具有基本的依赖注入，属性注入，属性动态更新，生成动态代理对像，动态代理远程对像，动态代理服务对像等功能。
 * 此IOC只能创建无参数构造函数的类，如果类不能满足此条件，则不能通过IOC创建，但可以在外部创建好后注册到IOC容器。
 * 技术上可支持多个IOC容器同时存在，但意义不大，因此目前只针对单IOC做过测试验证，没对多IOC同时存在做测试，但代码已经实出。
 * 整个JMicro框架从创建并启动IOC开始，其基本流程如下：
 * 
 * 首无通过 public static void parseArgs(String[] args)方法解析命令行参数；
 * 
 * JVM启动时，会在classpath下搜索全部IObjectFactory的实现类，实现类需要注解为@ObjFactory，通过默认构造函数做实例化，所以实现类必须带无参构造函数。
 * 
 * 增加  @see IPostInitListener， @see IPostFactoryReady 两种类型监听器。
 * 
 * 调用start方法启动容器
 * 
 * 微服务框开始运行并接受外部请求
 * 
 * 实现细节参考 @see SimpleObjectFactory
 *  
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:03:35
 */
public interface IObjectFactory {

	void foreach(Consumer<Object> c);
	
	/**
	 * 将外部的对像注册到IOC中，使外部创建的对像可以被IOC的其他对象所依赖，并且其自身也可依赖IOC容器的其他对象。
	 * 如单例模式创建的对像，依赖带参构造函数创建的对像。
	 * 注册KEY为实现类全名，及Compoent注解名
	 * @param obj
	 */
	void regist(Object obj);
	
	/**
	 * 指定类名注册对像，obj必须是clazz的实例
	 * @param clazz
	 * @param obj
	 */
	void regist(Class<?> clazz,Object obj);
	
	<T> void registT(Class<T> clazz,T obj);
	
	void regist(String comName, Object obj);
	
	/**
	 * 判断clazz类所对应的实例是否存在，如果存在则返回true，否则返回false
	 * @param clazz
	 * @return
	 */
	Boolean exist(Class<?> clazz);
	
	/**
	 * 取得类所对应的实例，如果cls是具体类，并具当前容器还不存在对应的实例，则创建之，然后返回。
	 * @param cls
	 * @return
	 */
	<T> T get(Class<T> cls);
	
	<T> T get(Class<T> cls,boolean create);
	
	/**
	 * 根据组件名（Component注解指定的value值）称取得实例
	 * @param clsName
	 * @return
	 */
	<T> T getByName(String clsName);
	
	/**
	 * 只能取得无包限制访问的服务，如具有包访问制，则返回NULL
	 * @param srvName
	 * @param namespace
	 * @param version
	 * @param acs
	 * @return
	 */
	<T> T getRemoteServie(String srvName,String namespace,String version,AsyncConfigJRso[] acs);
	
	/**
	 * 只能取得无包限制访问的服务，如具有包访问制，则返回NULL
	 * @param srvCls
	 * @param ns
	 * @param acs
	 * @return
	 */
	<T> T getRemoteServie(Class<T> srvCls,String ns,AsyncConfigJRso[] acs);
	
	<T> T getRemoteServie(ServiceItemJRso item,AsyncConfigJRso[] acs);
	
	/**
	 * 取得所有子类的实例
	 * @param parrentCls
	 * @return
	 */
	<T> Set<T> getByParent(Class<T> parrentCls);
	
	/**
	 * 启动IOC容器，实例化并初始化当前classpath下的所有组件，默认调用组件的init方法，或 @JMethod（“init”）指定的初始化方法。
	 * 组件创建过程：
	 * 1. 搜索classpath下指定包（通过basePackages命令行参数指定，cn.jmicro默认加入，并且不可修改）的全部注解为@Component的类；
	 * 2. 通过默认构造函数实例化组件；
	 * 3. 对level由小到大对组件进行做排序；
	 * 4. 组件初始化过程：
	 *    a. 注入依赖对像，包括远程服务对像；
	 *    b. IPostInitListener.preInit(Object obj,Config cfg)
	 *    c. 调用组件的init方法；
	 *    d. IPostInitListener.afterInit(Object obj,Config cfg)
	 * 5. 调用容器中全部的IPostFactoryReady.ready(IObjectFactory of)方法
	 * 6. 服务启动完成，
	 * 
	 */
	void start(IDataOperator dataOperator,String[] args);
	
	/**
	 * 如果IPostInitListener没有加PostListener注解，可以在调用start前，调用此方法加入，然后再start容器
	 * @param listener
	 */
	void addPostListener(IPostInitListener listener);
	
	void notifyPostListener(Object obj);
	
	/**如果IPostFactoryReady没有被IOC容器管理，可以在调用start前，调用此方法加入，然后再start容器
	 * @param listener
	 */
	//void addPostReadyListener(IFactoryListener listener);
	
	void addLoginStatusListener(ILoginStatusListener listener);
	
	Class<?> loadCls(String clsName);
	
	void masterSlaveListen(IMasterChangeListener l);
	
	Boolean isSysLogin();
	
	Boolean isRpcReady();
	
	ProcessInfoJRso getProcessInfo();
}
