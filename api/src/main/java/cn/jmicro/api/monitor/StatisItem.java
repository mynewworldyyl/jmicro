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
	
	//private int cnt;
	
	private long val;
	
	private long time;
	
	public StatisItem() {
		this.time = TimeUtils.getCurTime();
	}
	
	public StatisItem(short type) {
		this.type = type;
	}
	
	public void add(long val) {
		this.val += val;
		//this.sum += sum;
	}

	public short getType() {
		return type;
	}

	public void setType(short type) {
		this.type = type;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public long getVal() {
		return val;
	}

	public void setVal(long val) {
		this.val = val;
	}
}
