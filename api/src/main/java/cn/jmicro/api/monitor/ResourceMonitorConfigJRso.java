package cn.jmicro.api.monitor;

import java.io.BufferedWriter;

import cn.jmicro.api.annotation.IDStrategy;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.exp.Exp;
import lombok.Serial;

@Serial
@IDStrategy(1)
public class ResourceMonitorConfigJRso {

	public static final String RES_MONITOR_CONFIG_ROOT = Config.getRaftBasePath("") + "/resMonitorConfigs";
	
	public static final String DEFAULT_RESOURCE_TABLE_PREFIX = "t_res_table_";
	
	public static final String DEFAULT_RESOURCE_TABLE_NAME = DEFAULT_RESOURCE_TABLE_PREFIX + "default";
	
	//toType == TO_TYPE_SERVICE_METHOD时有效
	private transient String toSn;
	private transient String toNs;
	private transient String toVer;
	private transient String toMt;
	
	private transient Exp exp;
	
	private transient long lastNotifyTime;
	
	private transient BufferedWriter bw;
	
	//要监听的资源指标的KEYS
	//private transient Set<String> medataKey;
	
	//监听资源名称
	//private transient Set<String> resourceNames;
	
	private int id;
	
	private boolean enable;
	
	//要监听的实例名称
	private String monitorInsName;
	
	public String resName;
	
	public String expStr;
	
	//监听周期，单位毫秒
	private int t;
	
	//StatisConfig.TO_*打头类型
	private int toType;
	
	/*
	 * 根据toType类型决定其值格式，如存库，表示表名，转发RPC则是RPC方法的KEY，publish则是消息主题，控制台输出格式
	 */
	private String toParams;
	
	/*
	 * 原封不动地透传给接收者
	 */
	private String extParams;

	private int createdBy;
	
	private int clientId;
	
	private String createdByAct;
	
	public Exp getExp() {
		return exp;
	}

	public void setExp(Exp exp) {
		this.exp = exp;
	}

	public String getResName() {
		return resName;
	}

	public void setResName(String resName) {
		this.resName = resName;
	}

	public String getExpStr() {
		return expStr;
	}

	public void setExpStr(String expStr) {
		this.expStr = expStr;
	}

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
	}

	public String getCreatedByAct() {
		return createdByAct;
	}

	public void setCreatedByAct(String createdByAct) {
		this.createdByAct = createdByAct;
	}

	public String getMonitorInsName() {
		return monitorInsName;
	}

	public long getLastNotifyTime() {
		return lastNotifyTime;
	}

	public void setLastNotifyTime(long lastNotifyTime) {
		this.lastNotifyTime = lastNotifyTime;
	}

	public void setMonitorInsName(String monitorInsName) {
		this.monitorInsName = monitorInsName;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public String getToSn() {
		return toSn;
	}

	public void setToSn(String toSn) {
		this.toSn = toSn;
	}

	public String getToNs() {
		return toNs;
	}

	public void setToNs(String toNs) {
		this.toNs = toNs;
	}

	public String getToVer() {
		return toVer;
	}

	public void setToVer(String toVer) {
		this.toVer = toVer;
	}

	public String getToMt() {
		return toMt;
	}

	public void setToMt(String toMt) {
		this.toMt = toMt;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(int createdBy) {
		this.createdBy = createdBy;
	}

	public BufferedWriter getBw() {
		return bw;
	}

	public void setBw(BufferedWriter bw) {
		this.bw = bw;
	}

	public int getToType() {
		return toType;
	}

	public void setToType(int toType) {
		this.toType = toType;
	}

	public String getToParams() {
		return toParams;
	}

	public void setToParams(String toParams) {
		this.toParams = toParams;
	}

	public String getExtParams() {
		return extParams;
	}

	public void setExtParams(String extParams) {
		this.extParams = extParams;
	}

	public int getT() {
		return t;
	}

	public void setT(int t) {
		this.t = t;
	}

	@Override
	public int hashCode() {
		return this.id;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof ResourceMonitorConfigJRso)) {
			return false;
		}
		return ((ResourceMonitorConfigJRso)obj).getId() == this.id;
	}
	
	
}
