package cn.jmicro.ext.druid;

import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.pool.DruidDataSource;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.objectfactory.IPostFactoryListener;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Utils;

@Component(lazy=false, level=20)
public class PageInterceptorConfig implements IPostFactoryListener {

	private final static Logger logger = LoggerFactory.getLogger(PageInterceptorConfig.class);

	@Override
	public void afterInit(IObjectFactory of) {
		
	}

	@Override
	public int runLevel() {
		return 399;
	}

	public Properties pageHelperProperties() {
        return new Properties();
    }
	
	/**
	 * -D/mybatis/jdbc.driver=XXXX
	 * -D/mybatis/jdbc.url=XXXX
	 * -D/mybatis/jdbc.username=XXXX
	   -D/mybatis/jdbc.password=XXXX
	 */
	@Override
	public void preInit(IObjectFactory of) {
		Config cfg = of.get(Config.class);
		Map<String,String> params = cfg.getParamByPattern("/mybatis/");
		String driverKey = params.get("/mybatis/jdbc.driver");
		String url = params.get("/mybatis/jdbc.url");
		String username = params.get("/mybatis/jdbc.username");
		String password = params.get("/mybatis/jdbc.password");
		if(Utils.isEmpty(url) || Utils.isEmpty(username)) {
			return;
		}
		
		if(Utils.isEmpty(driverKey)) {
			throw new CommonException("Driver is NULL");
		}
		
		DruidDataSource ds = new DruidDataSource();
		ds.setUsername(username);
		ds.setPassword(password);
		ds.setUrl(url);
		ds.setDriverClassName(driverKey);
		
		try {
			ds.init();
		} catch (SQLException e) {
			try {
				if(ds != null) ds.close();
			} catch (Exception e1) {
				logger.error("",e1);
			}
			throw new CommonException("",e);
		}
		
		of.regist(DataSource.class, ds);
	}
}
