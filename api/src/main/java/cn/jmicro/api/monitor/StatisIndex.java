package cn.jmicro.api.monitor;

import cn.jmicro.api.annotation.SO;

@SO
public class StatisIndex {

	public static final int PREFIX_TOTAL = StatisConfig.PREFIX_TOTAL;//"total";
	public static final int PREFIX_TOTAL_PERCENT = StatisConfig.PREFIX_TOTAL_PERCENT; //"totalPercent";
	public static final int PREFIX_QPS = StatisConfig.PREFIX_QPS;//"qps";
	public static final int PREFIX_CUR = StatisConfig.PREFIX_CUR;//"cur";
	public static final int PREFIX_CUR_PERCENT = StatisConfig.PREFIX_CUR_PERCENT;// "curPercent";
	
	private byte type;
	
	//指标名称
	private String vk;
	
	//结果键值
	private String desc;
	
	//分子指标码值
	private Short[] nums;
	
	//分母指标码值
	private Short[] dens;
	
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
	
}
