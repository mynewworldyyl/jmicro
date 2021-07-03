package cn.jmicro.ext.pagehelper;

import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.pagehelper.PageInterceptor;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.objectfactory.IPostFactoryListener;
import cn.jmicro.common.CommonException;
import cn.jmicro.ext.mybatis.SqlSessionManager;

@Component(lazy=false, level=2000)
public class PageInterceptorConfig implements IPostFactoryListener {

	private final static Logger logger = LoggerFactory.getLogger(PageInterceptorConfig.class);

	@Override
	public void afterInit(IObjectFactory of) {
		
		Config cfg = of.get(Config.class);
		//Map<String,String> ps = cfg.get
		Map<String,String> result = cfg.getParamByPattern(PageHelperProperties.PAGEHELPER_PREFIX);
		
        PageInterceptor interceptor = new PageInterceptor();
        Properties properties = new Properties();
        //先把一般方式配置的属性放进去
        properties.putAll(pageHelperProperties());
        //在把特殊配置放进去，由于close-conn 利用上面方式时，属性名就是 close-conn 而不是 closeConn，所以需要额外的一步
        properties.putAll(result);
        interceptor.setProperties(properties);
      
        SqlSessionManager ssm = of.get(SqlSessionManager.class);
        
        if(ssm != null) {
        	 ssm.getConfiguration().addInterceptor(interceptor);
        } else {
        	throw new CommonException(SqlSessionManager.class.getName() + " not found!");
        }
		
	}

	@Override
	public int runLevel() {
		return 399;
	}

	public Properties pageHelperProperties() {
        return new Properties();
    }
	
	@Override
	public void preInit(IObjectFactory of) {
		
	}
}
