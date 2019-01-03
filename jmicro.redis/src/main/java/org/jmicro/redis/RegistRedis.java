package org.jmicro.redis;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.common.CommonException;
import org.jmicro.common.util.StringUtils;

import redis.clients.jedis.Jedis;

@Component(lazy=false, level=2)
public class RegistRedis {

	@Cfg(value = "/RegistRedis/redisHost", defGlobal=true)
	private String redisHost;
	
	@Inject
	private IObjectFactory of;
	
	public void init() {
		if(StringUtils.isEmpty(redisHost)) {
			throw new CommonException("Redis config [/RegistRedis/redisHost] not found");
		}
		Jedis jedis = new Jedis(redisHost);
		of.regist(Jedis.class, jedis);
	}
	
}
