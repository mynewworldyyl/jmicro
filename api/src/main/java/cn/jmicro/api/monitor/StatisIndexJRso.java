package cn.jmicro.api.monitor;

import lombok.Serial;

@Serial
public class StatisIndexJRso {

	public static final int PREFIX_TOTAL = StatisConfigJRso.PREFIX_TOTAL;//"total";
	public static final int PREFIX_TOTAL_PERCENT = StatisConfigJRso.PREFIX_TOTAL_PERCENT; //"totalPercent";
	public static final int PREFIX_QPS = StatisConfigJRso.PREFIX_QPS;//"qps";
	public static final int PREFIX_CUR = StatisConfigJRso.PREFIX_CUR;//"cur";
	public static final int PREFIX_CUR_PERCENT = StatisConfigJRso.PREFIX_CUR_PERCENT;// "curPercent";
	
	private byte type;
	
	//指标名称
	private String vk;
	
	//结果键值
	private String desc;
	
	//分子指标码值
	private Short[] nums;
	
	//分母指标码值
	private Short[] dens;
	
	//当前分子值
	private transient long curNums;
		
	//当前分母值
	private transient long curDens;
	
	private String expStr;

	public byte getType() {
		return type;
	}

	public void setType(byte type) {
		this.type = type;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public Short[] getNums() {
		return nums;
	}

	public void setNums(Short[] nums) {
		this.nums = nums;
	}

	public Short[] getDens() {
		return dens;
	}

	public void setDens(Short[] dens) {
		this.dens = dens;
	}

	public String getName() {
		return vk;
	}

	public void setName(String keyName) {
		this.vk = keyName;
	}

	public String getExpStr() {
		return expStr;
	}

	public void setExpStr(String expStr) {
		this.expStr = expStr;
	}

	public long getCurNums() {
		return curNums;
	}

	public void setCurNums(long curNums) {
		this.curNums = curNums;
	}

	public long getCurDens() {
		return curDens;
	}

	public void setCurDens(long curDens) {
		this.curDens = curDens;
	}
	
}
