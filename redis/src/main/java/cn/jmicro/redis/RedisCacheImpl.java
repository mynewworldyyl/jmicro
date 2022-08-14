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
package cn.jmicro.redis;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.cache.ICache;
import cn.jmicro.api.cache.ICacheRefresher;
import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.codec.IEncoder;
import cn.jmicro.api.codec.TypeUtils;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 *  实现缓存基本功能：
 * 1. 缓存任意对象,提取时直接取得缓存对象,省去应用序列化和反序列化操作,采用JMICRO高效前缀类型序列化框架实现
 * 2. 有效避免缓存雪崩,随机超时时间
 * 3. 缓存自动刷新,设定定时刷新
 * 4. 有效避免缓存击穿
 * 
 * 缓存数据KEY在单JVM和多JVM下应该有不同的策略，如果要求在单JVM下独立存在，那么KEY应该加入JVM标识，如
 * key = Config.getInstanceName() + "/" + key; 
 * 
 * Config.getInstanceName()取得JVM实例标识，JMICRO启动JVM时生成此标识，并自动确保在整个微服务集群中不存在重复JMV实例标识
 * 
 * 如果多JVM中使用相同缓存，则KEY不需要加JVM标识，但此此种情况下，更新缓存时有可能存在多个JMV实例下同时更新缓存的可能，除非加入分步式锁。
 * 但是因为多JVM实例使用的是同一个数据，所以单个实例和多个实例同时更新缓存一般情况下不会存在问题，如果服务有此依赖并出现问题，应该考虑此问题的可能。
 * 
 * @author Yulei Ye
 * @date 2020年1月11日
 */
@Component(active=true,value="redisCache",side=Constants.SIDE_ANY)
public class RedisCacheImpl implements ICache {

	private static final Logger logger = LoggerFactory.getLogger(RedisCacheImpl.class);
	
	private final Map<Long,TimerTicker> timers = new ConcurrentHashMap<>();
	
	private static final String ATOMIC_DEC_SCRIPT;
	static {
		StringBuilder sb = new StringBuilder();
		sb.append("local k = KEYS[1];\n");
		sb.append("local cnt = tonumber(ARGV[1]);\n");
		sb.append("local curVal = tonumber(redis.call('GET', k));\n");//取当前值
		sb.append("if cnt < 0 and curVal < -cnt then \n");
		sb.append("return -1; \n");//库存不够，扣减失败，直接返回失败
		sb.append("end\n");
		sb.append("return redis.call('INCRBY', k, cnt);\n");
		ATOMIC_DEC_SCRIPT = sb.toString();
	}
	
	private String[] adminPrefixs = new String[] {
			JMicroContext.CACHE_LOGIN_KEY,
			Constants.CACHE_DIR_PREFIX
	};
	
	@Cfg("/RedisCacheImpl/openDebug")
	private boolean openDebug = false;
	
	@Inject
	private ICodecFactory codeFactory;
	
	@Inject
	private JedisPool jeditPool;
	
	@Inject
	private ProcessInfoJRso pi;
	
	private Random r = new Random(TimeUtils.getCurTime()/10000);
	
	private Map<String,ICacheRefresher> refreshers = Collections.synchronizedMap(new HashMap<>());
	
	//指定KEY上次直接从源中提取数据时间，避免同小时间片内大批量对不存在的数据请求
	private Map<String,Long> notExistData = Collections.synchronizedMap(new HashMap<>());
	
	private String adminPrefix;
	
	private String selfPrefix;
	
	public void jready() {
		adminPrefix = Config.getAdminClientId() + ":";
		selfPrefix = Config.getClientId() + ":";
	}
	
	@Override
	public int increcement(String key, int val) {
		key = this.securityKey(key);
		Jedis r = jeditPool.getResource();
		try {
			int endId = Integer.parseInt(r.eval(ATOMIC_DEC_SCRIPT, 1, key,val+"").toString());
			return endId;
		}finally {
			r.close();
		}
	}

	@Override
	public <T> boolean put(String key, T val) {
		checkPermission(key);
		
		if(StringUtils.isEmpty(key)) {
			logger.error("Put key cannot be NULL");
			return false;
		}
		
		if(val == null) {
			logger.error("Put value cannot be NULL");
			return false;
		}
		
		key = securityKey(key);
		byte[] k = ICache.keyData(key);
		if(k == null) {
			return false;
		}
		
		byte[] value = encodeValue(val);// bb.array();
		if(value == null) return false;
		
		Jedis jedis = null;
		try {
			 /*if(logger.isInfoEnabled()) {
				 logger.info("Put KEY: {}, LEN: {}",key,value.length);
			 }*/
			 jedis = jeditPool.getResource();
			 jedis.set(k, value);
			 return true;
		} finally {
			if(jedis != null) {
				jedis.close();
			}
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private Object decodeValue(byte[] data, Class<?> cls) {
		if(data == null || data.length == 0) return null;

		try {
			if(TypeUtils.isByteBuffer(cls)){
				return ByteBuffer.wrap(data);
			} if(cls == String.class) {
				return new String(data,Constants.CHARSET);
			}else if(TypeUtils.isVoid(cls)) {
				return null;
			}else if(TypeUtils.isInt(cls)){
				return Integer.parseInt(new String(data, Constants.CHARSET));
			}else if(TypeUtils.isByte(cls)){
				return Byte.parseByte(new String(data, Constants.CHARSET));
			}else if(TypeUtils.isShort(cls)){
				return Short.parseShort(new String(data, Constants.CHARSET));
			}else if(TypeUtils.isLong(cls)){
				return Long.parseLong(new String(data, Constants.CHARSET));
			}else if(TypeUtils.isFloat(cls)){
				return Float.parseFloat(new String(data, Constants.CHARSET));
			}else if(TypeUtils.isDouble(cls)){
				return Double.parseDouble(new String(data,Constants.CHARSET));
			}else if(TypeUtils.isBoolean(cls)){
				return data[0] == 1;
			}else if(TypeUtils.isChar(cls)){
				return new String(data,Constants.CHARSET).charAt(0);
			}else if(TypeUtils.isDate(cls)){
				long lv = Long.parseLong(new String(data, Constants.CHARSET));
				return new Date(lv);
			} else {
				return codeFactory.getDecoder(Message.PROTOCOL_BIN).decode(ByteBuffer.wrap(data), cls);
			}
		} catch (NumberFormatException | UnsupportedEncodingException e) {
			logger.error(cls.getName(),e);
			throw new CommonException(cls.getName(),e);
		}
	
	}

	@SuppressWarnings("unchecked")
	private <T> byte[] encodeValue(T obj) {
		
		if(obj == null) {
			return new byte[0];
		}
		
		Class<T> cls = (Class<T>)obj.getClass();
		
		try {
			if(TypeUtils.isByteBuffer(cls)){
				return ((ByteBuffer)obj).array();
			} if(cls == String.class) {
				return obj.toString().getBytes(Constants.CHARSET);
			}else if(TypeUtils.isVoid(cls)) {
				return new byte[0];
			}else if(TypeUtils.isInt(cls)){
				return obj.toString().getBytes(Constants.CHARSET);
			}else if(TypeUtils.isByte(cls)){
				return obj.toString().getBytes(Constants.CHARSET);
			}else if(TypeUtils.isShort(cls)){
				return obj.toString().getBytes(Constants.CHARSET);
			}else if(TypeUtils.isLong(cls)){
				return obj.toString().getBytes(Constants.CHARSET);
			}else if(TypeUtils.isFloat(cls)){
				return obj.toString().getBytes(Constants.CHARSET);
			}else if(TypeUtils.isDouble(cls)){
				return obj.toString().getBytes(Constants.CHARSET);
			}else if(TypeUtils.isBoolean(cls)){
				boolean b = (Boolean)obj;
				return b? new byte[] {1} : new byte[] {0};
			}else if(TypeUtils.isChar(cls)){
				return obj.toString().getBytes(Constants.CHARSET);
			}else if(TypeUtils.isDate(cls)){
				return (((Date)obj).getTime()+"").getBytes(Constants.CHARSET);
			} else {
				ByteBuffer bb = (ByteBuffer)codeFactory.getEncoder(Message.PROTOCOL_BIN).encode(obj);
				if(bb != null) {
					return bb.array();
				}
			}
		} catch (UnsupportedEncodingException e) {
			logger.error(obj.toString(),e);
			throw new CommonException(obj.toString(),e);
		}
		throw new CommonException(obj.toString());
	}

	private void checkPermission(String key) {
		if(isAdminPrefix(key) && !(Utils.formSystemPackagePermission(4) || Config.isAdminSystem())) {
			String msg = "No permission to do this operation";
			LG.log(MC.LOG_WARN, RedisCacheImpl.class, msg);
			throw new CommonException(msg);
		}
	}
	
	private boolean isAdminPrefix(String key) {
		for(String p : adminPrefixs) {
			if(key.startsWith(p)) {
				return true;
			}
		}
		return false;
	}
	
	private String securityKey(String key) {
		if(this.isAdminPrefix(key)) {
			key = adminPrefix + key;
		}else {
			key = selfPrefix + key;
		}
		return key;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(String key, Class<T> type) {
		checkPermission(key);
		if(StringUtils.isEmpty(key)) {
			logger.error("Get key cannot be NULL");
			return null;
		}
		
		key = securityKey(key);
		
		byte[] k = ICache.keyData(key);
		
		Jedis jedis = null;
		try {
			 jedis = jeditPool.getResource();
			 byte[] val = jedis.get(k);
			 
			/* if(logger.isInfoEnabled()) {
				 logger.info("Get KEY: {}, LEN: {}",key,(val == null?"Null":val.length));
			 }*/
			 
			if(val != null && val.length > 0) {
				//命中缓存,理想情况下,大部份缓存都走到这里返回
				//return (T)codeFactory.getDecoder(Message.PROTOCOL_BIN).decode(ByteBuffer.wrap(val), null);
				return (T)this.decodeValue(val, type);
			} else {
				 
				//上次从源读取过相同数据,但是数据不存在,则判断和上次更新时间是否超过1秒,是则更新缓存，否则直接返回空
				if(notExistData.containsKey(key)) {
					long interval = TimeUtils.getCurTime() - notExistData.get(key);
					if(interval < 1000) {
						//间隔时间太小,直接返回空,意味数据最大延迟1000毫秒
						return null;
					}
					notExistData.remove(key);
				}
				
				//更新缓存并避免缓存击穿
				synchronized(key.intern()) {
					val = jedis.get(k);
					//有可能前面已经有一个线程更新缓存
					if(val == null) {
						//同一时刻只有一个线程能进来更新缓存
						boolean f = update(key);
						if(!f) {
							//数据不存在,缓本次更新时间
							notExistData.put(key, TimeUtils.getCurTime());
							return null;
						} else {
							val = jedis.get(k);
						}
					}
				}
				
				if(val != null) {
					return (T)this.decodeValue(val, type);
					//return (T)codeFactory.getDecoder(Message.PROTOCOL_BIN).decode(ByteBuffer.wrap(val), null);
				}
				return null;
			}
		} finally {
			if(jedis != null) {
				jedis.close();
			}
		}
	}

	private boolean update(String key) {
		
		ICacheRefresher ref = this.refreshers.get(key);
		if(ref == null) {
			return false;
		}
		
		Object val = ref.get(key);
		if(val == null) {
			return false;
		}
		
		return this.put(key, val);
		
	}

	@Override
	public <T> boolean put(String key, T val, long expire) {
		return put(key,val,expire,-1);
	}

	@Override
	public <T> boolean put(String key, T val, long expire, int randomVal) {
		boolean f = this.put(key, val);
		this.expire(key, expire,randomVal);
		return f;
	}

	@Override
	public <T> boolean setNx(String key, T val) {
		
		checkPermission(key);
		
		if(StringUtils.isEmpty(key)) {
			logger.error("Set key cannot be NULL");
			return false;
		}
		
		if(val == null) {
			logger.error("Set value cannot be NULL");
			return false;
		}
		
		key = securityKey(key);
		byte[] k = ICache.keyData(key);
		if(k == null) {
			return false;
		}
		
		byte[] value = this.encodeValue(val);
		
		/*if(val == null || val.toString().equals("")) {
			value = new byte[0];
		} else {
			ByteBuffer bb = (ByteBuffer)codeFactory.getEncoder(Message.PROTOCOL_BIN).encode(val);
			if(bb == null) {
				logger.error(val.toString() + " encode error");
				return false;
			}
			value = bb.array();
		}*/
		
		Jedis jedis = null;
		try {
			 jedis = jeditPool.getResource();
			 return jedis.setnx(k, value) != 0;
		} finally {
			if(jedis != null) {
				jedis.close();
			}
		}
	}

	@Override
	public boolean exist(String key) {
		checkPermission(key);
		if(StringUtils.isEmpty(key)) {
			logger.error("Key cannot be NULL");
			return false;
		}
		
		byte[] k = ICache.keyData(this.securityKey(key));
		if( k == null) {
			return false;
		}
		
		Jedis jedis = null;
		try {
			 jedis = jeditPool.getResource();
			 return jedis.exists(k);
		} finally {
			if(jedis != null) {
				jedis.close();
			}
		}
	}

	@Override
	public boolean expire(String key, long expire) {
		checkPermission(key);
		if(StringUtils.isEmpty(key)) {
			logger.error("Expire key cannot be NULL");
			return false;
		}
		
		byte[] k = ICache.keyData(selfPrefix+key);
		if( k == null) {
			return false;
		}
		
		Jedis jedis = null;
		try {
			 jedis = jeditPool.getResource();
			 jedis.pexpire(k, expire);
		} finally {
			if(jedis != null) {
				jedis.close();
			}
		}
		return true;
	}

	@Override
	public boolean expire(String key, long expire, int randomVal) {
		checkPermission(key);
		if(expire > 0 ) {
			long rv = -1;
			if(randomVal > 0) {
				rv = expire + this.r.nextInt(randomVal);
			} else {
				rv = 0;
			}
			return expire(key,expire+rv);
		}
		return false;
	}
	
	@Override
	public void setReflesher(String key, ICacheRefresher ref) {
		checkPermission(key);
		refreshers.put(key, ref);
	}
	

	@Override
	public boolean configRefresh(String key, long timerWithMilliseconds,long expire, int randomVal) {
		checkPermission(key);
		ICacheRefresher ref = this.refreshers.get(key);
		if(ref == null) {
			logger.error(key + " refresh not exist");
			return false;
		}
		
		//通过旋转时钟刷新缓存
		TimerTicker t = TimerTicker.getTimer(this.timers, timerWithMilliseconds).setOpenDebug(openDebug);
		
		t.addListener(key, r, true, (k,rr) -> {
			Object val = ref.get(key);
			if( val != null) {
				put(key,val,expire,randomVal);
			}
		});
		
		return true;
	}

	@Override
	public boolean del(String key) {
		checkPermission(key);
		if(StringUtils.isEmpty(key)) {
			logger.error("Del key cannot be NULL");
			return false;
		}
		byte[] k = ICache.keyData(selfPrefix+key);
		if( k == null) {
			return false;
		}
		
		Jedis jedis = null;
		try {
			 jedis = jeditPool.getResource();
			 Long rst = jedis.del(k);
			this.refreshers.remove(key);
			this.notExistData.remove(key);
			return rst == 1;
		} finally {
			if(jedis != null) {
				jedis.close();
			}
		}
	}

	
	/*********************Map start**************************/
	
	@Override
	public boolean hput(String key, Map<String, Object> hdata) {
		checkPermission(key);
		
		if(StringUtils.isEmpty(key)) {
			logger.error("hPut key cannot be NULL");
			return false;
		}
		
		if(hdata == null || hdata.isEmpty()) {
			logger.error("hPut value cannot be NULL");
			return false;
		}
		
		key = securityKey(key);
		byte[] k = ICache.keyData(key);
		if(k == null) {
			return false;
		}
		
		Map<byte[],byte[]> pdata = new HashMap<>();
		
		@SuppressWarnings("unchecked")
		IEncoder<ByteBuffer> enc = codeFactory.getEncoder(Message.PROTOCOL_BIN);
		for(Map.Entry<String, Object> e : hdata.entrySet()) {
			ByteBuffer bb = enc.encode(e.getValue());
			if(bb == null) {
				throw new CommonException("Encode error: "+e.getKey()+" ,data: " + e.getValue().getClass().getName());
			}
			byte[] value = bb.array();
			byte[] kd = ICache.keyData(e.getKey());
			pdata.put(kd, value);
		}
		
		Jedis jedis = null;
		try {
			 /*if(logger.isInfoEnabled()) {
				 logger.info("Put KEY: {}, LEN: {}",key,value.length);
			 }*/
			 jedis = jeditPool.getResource();
			 return jedis.hset(k, pdata) >= 1;
		} finally {
			if(jedis != null) {
				jedis.close();
			}
		}
	}

	@Override
	public boolean hdel(String key, String fname) {
		checkPermission(key);
		if(StringUtils.isEmpty(key)) {
			logger.error("Del key cannot be NULL");
			return false;
		}
		byte[] k = ICache.keyData(this.securityKey(key));
		if( k == null) {
			return false;
		}
		Jedis jedis = null;
		try {
			jedis = jeditPool.getResource();
			Long rst = jedis.hdel(k,ICache.keyData(fname));
			this.notExistData.remove(key);
			return rst == 1;
		} finally {
			if(jedis != null) {
				jedis.close();
			}
		}
	
	}

	@Override
	public <T> T hget(String key, String fname, Class<T> type) {

		checkPermission(key);
		if(StringUtils.isEmpty(key)) {
			logger.error("Get key cannot be NULL");
			return null;
		}
		
		key = securityKey(key);
		
		byte[] k = ICache.keyData(key);
		byte[] fk = ICache.keyData(fname);
		
		Jedis jedis = null;
		try {
			 jedis = jeditPool.getResource();
			 byte[] val = jedis.hget(k,fk);
			 
			/* if(logger.isInfoEnabled()) {
				 logger.info("Get KEY: {}, LEN: {}",key,(val == null?"Null":val.length));
			 }*/
			 
			if(val != null && val.length > 0) {
				//命中缓存,理想情况下,大部份缓存都走到这里返回
				return (T)codeFactory.getDecoder(Message.PROTOCOL_BIN).decode(ByteBuffer.wrap(val), null);
			} else {
				//上次从源读取过相同数据,但是数据不存在,则判断和上次更新时间是否超过1秒,是则更新缓存，否则直接返回空
				if(notExistData.containsKey(key)) {
					long interval = TimeUtils.getCurTime() - notExistData.get(key);
					if(interval < 1000) {
						//间隔时间太小,直接返回空,意味数据最大延迟1000毫秒
						return null;
					}
					notExistData.remove(key);
				}
				return null;
			}
		} finally {
			if(jedis != null) {
				jedis.close();
			}
		}
	
	}

	@Override
	public boolean hexist(String key, String fname) {
		checkPermission(key);
		if(StringUtils.isEmpty(key)) {
			logger.error("Key cannot be NULL");
			return false;
		}
		
		byte[] k = ICache.keyData(this.securityKey(key));
		if( k == null) {
			return false;
		}
		
		byte[] fk = ICache.keyData(this.securityKey(key));
		
		Jedis jedis = null;
		try {
			 jedis = jeditPool.getResource();
			 return jedis.hexists(k,fk);
		} finally {
			if(jedis != null) {
				jedis.close();
			}
		}
	
	}

	@Override
	public <T> boolean hput(String key, String fname, T val) {

		checkPermission(key);
		
		if(StringUtils.isEmpty(key)) {
			logger.error("Put key cannot be NULL");
			return false;
		}
		
		if(val == null) {
			logger.error("Put value cannot be NULL");
			return false;
		}
		
		key = securityKey(key);
		byte[] k = ICache.keyData(key);
		if(k == null) {
			return false;
		}
		
		ByteBuffer bb = (ByteBuffer)codeFactory.getEncoder(Message.PROTOCOL_BIN).encode(val);
		if(bb == null) {
			logger.error(val.toString() + " encode error");
			return false;
		}
		byte[] value = bb.array();
		byte[] fk = ICache.keyData(fname);
		
		Jedis jedis = null;
		try {
			 /*if(logger.isInfoEnabled()) {
				 logger.info("Put KEY: {}, LEN: {}",key,value.length);
			 }*/
			 jedis = jeditPool.getResource();
			 return jedis.hset(k,fk,value) == 1l;
		} finally {
			if(jedis != null) {
				jedis.close();
			}
		}
	
	}
	
	/**********************Map end*************************/
}
