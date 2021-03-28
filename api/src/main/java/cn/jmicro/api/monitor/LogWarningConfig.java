package cn.jmicro.api.monitor;

import java.io.BufferedWriter;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.exp.Exp;
import cn.jmicro.api.monitor.genclient.ILogWarning$JMAsyncClient;

@SO
public class LogWarningConfig {
	
	//将匹配的日志转发给cfgParams（服务方法KEY）指定的RPC方法
	public static final int TYPE_FORWARD_SRV = 1;
	
	//将匹配的日志存数据库，cfgParams为数据库表名称
	public static final int TYPE_SAVE_DB = 2;
	
	//将匹配的日志输出到控制台，cfgParams参数无意义
	public static final int TYPE_CONSOLE = 3;
	
	//将匹配的日志保存到文件，cfgParams为文件路径
	public static final int TYPE_SAVE_FILE = 4;

	private String id;
	
	private boolean enable;
	
	private int clientId;
	
	private int type;
	
	//根据type值解析其含义
	private String cfgParams;
	
	private String tag;
	
	private String expStr;
	
	private int createdBy;
	
	private int minNotifyInterval;
	
	private transient long lastNotifyTime;
	
	private transient Exp exp;
	
	private transient ILogWarning$JMAsyncClient srv;
	
	private transient BufferedWriter bw;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getMinNotifyInterval() {
		return minNotifyInterval;
	}

	public void setMinNotifyInterval(int minNotifyInterval) {
		this.minNotifyInterval = minNotifyInterval;
	}

	public long getLastNotifyTime() {
		return lastNotifyTime;
	}

	public void setLastNotifyTime(long lastNotifyTime) {
		this.lastNotifyTime = lastNotifyTime;
	}

	public String getExpStr() {
		return expStr;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public void setExpStr(String expStr) {
		this.expStr = expStr;
	}

	public Exp getExp() {
		return exp;
	}

	public void setExp(Exp exp) {
		this.exp = exp;
	}

	public ILogWarning$JMAsyncClient getSrv() {
		return srv;
	}

	public void setSrv(ILogWarning$JMAsyncClient srv) {
		this.srv = srv;
	}

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getCfgParams() {
		return cfgParams;
	}

	public void setCfgParams(String cfgParams) {
		this.cfgParams = cfgParams;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public BufferedWriter getBw() {
		return bw;
	}

	public void setBw(BufferedWriter bw) {
		this.bw = bw;
	}

	public int getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(int createdBy) {
		this.createdBy = createdBy;
	}
	
}
