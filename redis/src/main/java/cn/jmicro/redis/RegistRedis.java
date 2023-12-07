package cn.jmicro.redis;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.objectfactory.IPostFactoryListener;
import cn.jmicro.common.util.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

@Component(lazy=false, level=0)
public class RegistRedis implements IPostFactoryListener{

	//@Cfg(value = "/RegistRedis/redisHost", defGlobal=true)
	private String redisHost = "127.0.0.1";
	
	//@Cfg(value = "/RegistRedis/port", defGlobal=true)
	private int port = 6379;
	
	@Inject
	private IObjectFactory of;
	
	public void init() {
	}

	@Override
	public void preInit(IObjectFactory of) {
		Config cfg = of.get(Config.class);
		String rh = cfg.getString("/RegistRedis/redisHost", "127.0.0.1");
		if(StringUtils.isNotEmpty(rh)) {
			redisHost = rh;
			//throw new CommonException("Redis config [/RegistRedis/redisHost] not found");
		}
		int p = cfg.getInt("/RegistRedis/port", 6379);
		if(p > 0) {
			port = p;
			//throw new CommonException("Redis config [/RegistRedis/redisHost] not found");
		}
		
		String pwd = cfg.getString("/RegistRedis/pwd", null);
		
	/*	Jedis jedis = new Jedis(redisHost,port);
		of.regist(Jedis.class, jedis);*/
		
		JedisPool pool = new JedisPool(new JedisPoolConfig(), redisHost, port, Protocol.DEFAULT_TIMEOUT, pwd);
		of.regist(JedisPool.class, pool);
		
	}

	@Override
	public void afterInit(IObjectFactory of) {
		
	}

	@Override
	public int runLevel() {
		return 0;
	}
	
}
