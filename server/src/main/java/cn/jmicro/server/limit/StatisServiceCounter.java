package cn.jmicro.server.limit;

import java.util.concurrent.TimeUnit;

import cn.jmicro.api.monitor.IServiceCounter;

public class StatisServiceCounter implements IServiceCounter<Short>{

	private String key;
	private Short[] types;
	
	private double qps = 0D;
	
	public StatisServiceCounter() {}
	
	public StatisServiceCounter(String key,Short[] types) {
		this.key = key;
		this.types = types;
	}
	
	@Override
	public double getQps(TimeUnit tounit, Short... types) {
		return qps;
	}
	
	public void setQps(Double qps) {
		this.qps = qps;
	}

	@Override
	public long get(Short type) {
		return 0;
	}

	@Override
	public boolean add(Short type, long val) {
		return false;
	}

	@Override
	public boolean add(Short type, long val, long actTime) {
		return false;
	}

	@Override
	public Long getTotal(Short... type) {
		return null;
	}

	@Override
	public boolean increment(Short type) {
		return false;
	}

	@Override
	public boolean increment(Short type, long actTime) {
		return false;
	}

	@Override
	public boolean addCounter(Short type) {
		return false;
	}

	@Override
	public long getByTypes(Short... types) {
		return 0;
	}

	@Override
	public boolean existType(Short type) {
		return false;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Short[] getTypes() {
		return types;
	}

	public void setTypes(Short[] types) {
		this.types = types;
	}
	
}
