package cn.jmicro.ext.mybatis;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.objectfactory.IPostFactoryListener;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.util.StringUtils;

@Component(lazy=false, level=99)
public class Init implements IPostFactoryListener{
	
	private final static Logger logger = LoggerFactory.getLogger(Init.class);
	
	//private MapperProxy mapperProxy = new MapperProxy();
	
	private IObjectFactory of;
	
	@Override
	public void preInit(IObjectFactory of) {
		this.of = of;
		Config cfg = of.get(Config.class);
		String  configLocation = cfg.getString("/mybatis/configLocation", null);
		if(StringUtils.isEmpty(configLocation)) {
			throw new CommonException("/mybatis/configLocation not found");
		}
		
		String  env = cfg.getString("/mybatis/env", null);
		if(StringUtils.isEmpty(configLocation)) {
			env = "dev";
		}
		
		Map<String,String> params = cfg.getParamByPattern("/mybatis/");
		Properties props = new Properties();
		props.putAll(params);
		
		InputStream inputStream = IPostFactoryListener.class.getResourceAsStream(configLocation);
		SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream,env,props);
		SqlSessionManager ssm = new SqlSessionManager(sqlSessionFactory);
		of.regist(SqlSessionManager.class, ssm);
		//of.regist(SqlSessionFactory.class, ssm);
		//of.regist(CurSqlSessionFactory.class, ssm);
		
		createMapperProxy(of,sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers(),ssm);
	}

	private void createMapperProxy(IObjectFactory of, Collection<Class<?>> mappers,SqlSessionManager ssm) {
		if(mappers == null || mappers.isEmpty()) {
			return;
		}
		for(Class<?> cls : mappers) {
			if(!cls.isInterface()) {
				logger.error("cls ["+cls.getName()+"] must be interface");
				continue;
			}
			Object m = createMapperProxy(cls,ssm);
			if(m != null) {
				of.regist(cls,m);
			}
		}
	}

	private Object createMapperProxy(Class<?> cls, SqlSessionManager ssm) {
		 return Proxy.newProxyInstance(Init.class.getClassLoader(),new Class[] { cls }, new MapperProxy(cls));
	}

	@Override
	public void afterInit(IObjectFactory of) {
		
	}

	@Override
	public int runLevel() {
		return 0;
	}
	
	private class MapperProxy implements InvocationHandler {
		
		private Class<?> targetCls;
		
		private Object obj = new Object();
		
		public MapperProxy(Class<?> targetCls) {
			this.targetCls = targetCls;
		}
		
	    @Override
	    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
	    	  if(method.getDeclaringClass() == Object.class) {
	    		  return method.invoke(obj, args);
	    	  }
	    	  SqlSessionManager ssm = Init.this.of.get(SqlSessionManager.class);
	    	  SqlSession sqlSession = ssm.curSession();
	    	  Object mapper = sqlSession.getMapper(method.getDeclaringClass());
	    	  Method m = mapper.getClass().getMethod(method.getName(), method.getParameterTypes());
	          Object result = m.invoke(mapper, args);
	          return result;
	   }
	    
	}
	
}
