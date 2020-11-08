package cn.jmicro.api.monitor;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.utils.TimeUtils;

/**
 * 
 * @author yeyulei
 *
 */
@SO
public class StatisItem {

	private short type;
	
	private int cnt;
	
	private long sum;
	
	private long time;
	
	public StatisItem() {
		this.time = TimeUtils.getCurTime();
	}
	
	public StatisItem(short type) {
		this.type = type;
	}
	
	public void add(int cnt,double sum) {
		this.cnt += cnt;
		this.sum += sum;
	}

	public short getType() {
		return type;
	}

	public void setType(short type) {
		this.type = type;
	}

	public int getCnt() {
		return cnt;
	}

	public void setCnt(int cnt) {
		this.cnt = cnt;
	}

	public long getSum() {
		return sum;
	}

	public void setSum(long sum) {
		this.sum = sum;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}
	
	
}
