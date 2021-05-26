package cn.jmicro.ext.mybatis;

import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.Statement;
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
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.objectfactory.IPostFactoryListener;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.StringUtils;

@Component(lazy=false, level=99)
public class Init implements IPostFactoryListener{
	
	private final static Logger logger = LoggerFactory.getLogger(Init.class);
	
	//private MapperProxy mapperProxy = new MapperProxy();
	
	private IObjectFactory of;
	
	private SqlSessionManager sqlSessionMng;
	
	@Override
	public void preInit(IObjectFactory of) {
		
		DriverManager.setLogWriter(new PrintWriter(System.err));
		
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
		
		String driverKey = "/mybatis/jdbc.driver";
		if(params.containsKey(driverKey)) {
			String driverUrl = params.get(driverKey);
			if(!Utils.isEmpty(driverUrl)) {
				try {
					logger.info("Load driver: " + driverUrl);
					LG.log(MC.LOG_INFO, Init.class, "Load driver: " + driverUrl);
					ClassLoader cl = ClassLoader.getSystemClassLoader();
					cl.loadClass(driverUrl);
				} catch (ClassNotFoundException e) {
					logger.error("jdbc driver class not found: " + driverUrl,e);
					LG.log(MC.LOG_ERROR, Init.class, "jdbc driver class not found: " + driverUrl,e);
				}
			}
		}
		
		if(LG.isLoggable(MC.LOG_INFO)) {
			LG.log(MC.LOG_INFO, Init.class,"MyBatis Config: "+ params.toString());
		}
		
		//logger.info("MyBatis Config: "+ params.toString());
		
		InputStream inputStream = IPostFactoryListener.class.getResourceAsStream(configLocation);
		SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream,env,props);
		sqlSessionMng = new SqlSessionManager(sqlSessionFactory);
		of.regist(SqlSessionManager.class, sqlSessionMng);
		//of.regist(SqlSessionFactory.class, ssm);
		//of.regist(CurSqlSessionFactory.class, ssm);
		testConnection(sqlSessionMng);
		//printDrivers();
		createMapperProxy(of,sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers());
	}

	private void printDrivers() {
		
		java.util.Enumeration<Driver> drs = DriverManager.getDrivers();
		if(drs != null) {
			Class<?> caller = Init.class;
			ClassLoader callerCL = caller != null ? caller.getClassLoader() : null;
			 // synchronize loading of the correct classloader.
            if (callerCL == null) {
                callerCL = Thread.currentThread().getContextClassLoader();
            }
	        
			for(;drs.hasMoreElements();) {
				Driver d = drs.nextElement();
				boolean result = false;
				Class<?> aClass = null;
				
	            try {
	                aClass =  Class.forName(d.getClass().getName(), true, callerCL);
	            } catch (Exception ex) {
	                result = false;
	            }

	             result = ( aClass == d.getClass() ) ? true : false;
				
				logger.info("Driver: " + d.toString()+", result:" + result +",cl: " + d.getClass().getClassLoader().getClass().getName());
			}
		}
	}

	private void testConnection(SqlSessionManager sqlSessionMng2) {
		SqlSession s = null;
		try {
			s = sqlSessionMng.openSession(false);
			Statement st = s.getConnection().createStatement();
			boolean succ = st.execute("select version()");
			if(!succ) {
				logger.error("test connection fail");
				LG.log(MC.LOG_ERROR, Init.class,"test connection fail");
			}
			
		}catch(Throwable e){
			logger.error("",e);
			LG.log(MC.LOG_ERROR, Init.class, "Test conn error",e);
		}finally {
			if(s != null) {
				s.close();
			}
		}
		
	}

	private void createMapperProxy(IObjectFactory of, Collection<Class<?>> mappers) {
		if(mappers == null || mappers.isEmpty()) {
			return;
		}
		for(Class<?> cls : mappers) {
			if(!cls.isInterface()) {
				logger.error("cls ["+cls.getName()+"] must be interface");
				continue;
			}
			Object m = createMapperProxy(cls);
			if(m != null) {
				of.regist(cls,m);
			}
		}
	}

	private Object createMapperProxy(Class<?> cls) {
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
	    	  SqlSession sqlSession = (SqlSession)sqlSessionMng.curSession();
	    	  Object mapper = sqlSession.getMapper(method.getDeclaringClass());
	    	  Method m = mapper.getClass().getMethod(method.getName(), method.getParameterTypes());
	          Object result = m.invoke(mapper, args);
	          return result;
	   }
	    
	}
	
}
