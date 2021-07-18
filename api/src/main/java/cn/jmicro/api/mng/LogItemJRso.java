package cn.jmicro.api.mng;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.JMLogItemJRso;

@SO
public class LogItemJRso {

	private JMLogItemJRso item;
	
	private short type = 0;
	
	private int num = 1;
	private long val = 0;
	
	private String desc=null;
	private long time = 0;
	
	private String tag = null;
	private byte level = MC.LOG_NO;
	
	private String instanceName;

	public JMLogItemJRso getItem() {
		return item;
	}

	public void setItem(JMLogItemJRso item) {
		this.item = item;
	}

	public short getType() {
		return type;
	}

	public void setType(short type) {
		this.type = type;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public long getVal() {
		return val;
	}

	public void setVal(long val) {
		this.val = val;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public byte getLevel() {
		return level;
	}

	public void setLevel(byte level) {
		this.level = level;
	}

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}
	
}
