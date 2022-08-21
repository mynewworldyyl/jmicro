package cn.jmicro.objfactory.spring;

import java.util.Set;
import java.util.function.Consumer;

import org.springframework.aop.support.AopUtils;

import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.masterelection.IMasterChangeListener;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.objectfactory.IPostInitListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.AsyncConfigJRso;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.security.ILoginStatusListener;

//@Component
public class SpringJMicroObjectFactory implements IObjectFactory {

	private IObjectFactory ofProxy;
	
	private ClassLoader jmicroRpcClassLoader;
	
	@Override
	public void foreach(Consumer<Object> c) {
		ofProxy.foreach(c);
	}

	@Override
	public void regist(Object obj) {
		Object c = SpringAndJmicroComponent.createLazyProxyObjectByCglib(obj,AopUtils.getTargetClass(obj).getName(),jmicroRpcClassLoader);
		ofProxy.regist(c);
	}

	@Override
	public void regist(Class<?> clazz, Object obj) {

	}

	@Override
	public <T> void registT(Class<T> clazz, T obj) {

	}

	@Override
	public void regist(String comName, Object obj) {

	}

	@Override
	public Boolean exist(Class<?> clazz) {
		return null;
	}

	@Override
	public <T> T get(Class<T> cls) {
		return null;
	}

	@Override
	public <T> T getByName(String clsName) {
		return ofProxy.getByName(clsName);
	}

	@Override
	public <T> T getRemoteServie(String srvName, String namespace, String version, AsyncConfigJRso[] acs) {
		return null;
	}

	@Override
	public <T> T getRemoteServie(Class<T> srvCls, String ns, AsyncConfigJRso[] acs) {
		return null;
	}

	@Override
	public <T> T getRemoteServie(ServiceItemJRso item, AsyncConfigJRso[] acs) {
		return null;
	}

	@Override
	public <T> T get(Class<T> cls, boolean create) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> Set<T> getByParent(Class<T> parrentCls) {
		return null;
	}

	@Override
	public void start(IDataOperator dataOperator, String[] args) {

	}

	@Override
	public void addPostListener(IPostInitListener listener) {

	}

	@Override
	public Class<?> loadCls(String clsName) {
		return null;
	}

	@Override
	public void masterSlaveListen(IMasterChangeListener l) {

	}

	@Override
	public Boolean isSysLogin() {
		return null;
	}

	@Override
	public Boolean isRpcReady() {
		return null;
	}

	@Override
	public ProcessInfoJRso getProcessInfo() {
		return null;
	}

	@Override
	public void notifyPostListener(Object obj) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addLoginStatusListener(ILoginStatusListener listener) {
		// TODO Auto-generated method stub
		
	}

	public IObjectFactory getOf() {
		return ofProxy;
	}

	public void setOf(IObjectFactory of,ClassLoader jmicroRpcClassLoader) {
		this.ofProxy = of;
		this.jmicroRpcClassLoader = jmicroRpcClassLoader;
	}

}
