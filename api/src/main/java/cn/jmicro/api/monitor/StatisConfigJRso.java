package cn.jmicro.api.monitor;

import java.io.BufferedWriter;
import java.util.Set;
import java.util.regex.Pattern;

import cn.jmicro.api.config.Config;
import cn.jmicro.api.exp.Exp;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import lombok.Serial;

@Serial
public class StatisConfigJRso {

	public static final String STATIS_CONFIG_ROOT = Config.getRaftBasePath("") + "/statisConfigs";
	
	public static final String UNIT_SE = Constants.TIME_SECONDS;
	public static final String UNIT_MU = Constants.TIME_MINUTES;
	public static final String UNIT_HO = Constants.TIME_HOUR;
	public static final String UNIT_DA = Constants.TIME_DAY;
	
	//public static final String BY_TYPE_SERVICE = "Service";
	//public static final String BY_TYPE_SERVICE_INSTANCE = "ServiceInstance";
	//public static final String BY_TYPE_SERVICE_ACCOUNT = "ServiceAccount";
	
	/*public static final String BY_TYPE_SERVICE_METHOD = "ServiceMethod";
	public static final String BY_TYPE_SERVICE_INSTANCE_METHOD = "ServiceInstanceMethod";
	public static final String BY_TYPE_SERVICE_ACCOUNT_METHOD = "ServiceAccountMethod";
	public static final String BY_TYPE_INSTANCE = "ClientInstance";
	public static final String BY_TYPE_ACCOUNT= "Account";*/
	
	public static final int BY_TYPE_SERVICE_METHOD = 1;
	
	//调用此实例下的服务方法时，需要监控
	public static final int BY_TYPE_SERVICE_INSTANCE_METHOD = 2;
	
	//指定账号调用此服务方法时需要监控
	public static final int BY_TYPE_SERVICE_ACCOUNT_METHOD = 3;
	
	//此运行实例的操作需要监控，包括RPC和非RPC
	public static final int BY_TYPE_INSTANCE = 4;
	
	//使用此账号运行的实例及此账号的RPC调用需要监控
	public static final int BY_TYPE_ACCOUNT = 5;
	
	//public static final String BY_TYPE_EXP = "Expression";
	
	/*
	public static final String TO_TYPE_DB = "DB";
	public static final String TO_TYPE_SERVICE_METHOD = "ServiceMethod";
	public static final String TO_TYPE_CONSOLE = "Console";
	public static final String TO_TYPE_FILE = "File";
	*/
	
	public static final int TO_TYPE_DB = 1;
	public static final int TO_TYPE_SERVICE_METHOD = 2;
	public static final int TO_TYPE_CONSOLE = 3;
	public static final int TO_TYPE_FILE = 4;
	public static final int TO_TYPE_MONITOR_LOG = 5;
	public static final int TO_TYPE_MESSAGE = 6;
	public static final int TO_TYPE_EMAIL = 7;
	public static final int TO_TYPE_DIRECT = 8;
	
	public static final byte PREFIX_TOTAL = 1; 		   //"total";
	public static final byte PREFIX_TOTAL_PERCENT = 2; //"totalPercent";
	public static final byte PREFIX_QPS = 3; 		   //"qps";
	public static final byte PREFIX_CUR = 4; 		   //"cur";
	public static final byte PREFIX_CUR_PERCENT =5;    // "curPercent";
	
	public static final String  DEFAULT_DB = "t_statis_data";
	
	public StatisConfigJRso() {}
	/*
	public static final int EXP_TYPE_SERVICE = 1;
	public static final int EXP_TYPE_ACCOUNT = 2;
	public static final int EXP_TYPE_INSTANCE = 3;
	*/
	private transient BufferedWriter bw;
	
	/*
	 * 此配置匹配数据范围
	 * -1：不限账号，全部匹配，只有高级账号可以使用
	 * 0：Admin账号，只有高级账号可以使用
	 * >0: 其他普通账号
	 */
	private int clientId = -1;
	
	//toType == TO_TYPE_SERVICE_METHOD时有效
	
	//toType == TO_TYPE_SERVICE_METHOD时有效
	private transient Set<Short> types;
	
	private transient String bysn;
	private transient String byns;
	private transient String byver;
	private transient String byme;
	private transient String byins;
	
	private transient String toSn;
	private transient String toNs;
	private transient String toVer;
	private transient String toMt;
	
	private transient Exp[] exp = new Exp[2];
	private transient int expIndex;
	
	//private transient ServiceCounter sc;
	
	private transient Pattern pattern;
	
	private transient long lastActiveTime = TimeUtils.getCurTime();
	
	private transient long lastNotifyTime = TimeUtils.getCurTime();
	
	private int id;
	
	//服务方法，实例方法，服务，账户
	private int byType;
	
	/**
	 * byType 下对应的key，如账户：test01, jmicro, *表示全部
	 * byType == BY_TYPE_SERVICE  bykey = service key
	 * byType == BY_TYPE_SERVICE_METHOD  bykey = service method key
	 * byType == BY_TYPE_SERVICE_INSTANCE  bykey = service instance key with instance name or host and port
	 * byType == BY_TYPE_SERVICE_INSTANCE_METHOD  bykey = service method key with instance name or host and port
	 * byType == BY_TYPE_SERVICE_ACCOUNT  bykey = account name
	 * byType == BY_TYPE_CLIENT_INSTANCE  bykey = jvm instance name
	 * BY_TYPE_SERVICE_METHOD_ACCOUNT
	 */
	private String byKey;
	
	//表达式类型：服务，实例，账号
	//private int expForType;
	
	private String expStr;
	
	private String expStr1;
	
	/*
	 * qps, total,current total, total percent, current percent
	 * @See MC.PREFIX_QPS
	 * @See MC.PREFIX_TOTAL_PERCENT
	 * @See MC.PREFIX_CUR_PERCENT
	 * @See MC.PREFIX_TOTAL
	 * @See MC.PREFIX_CUR
	 */
	private StatisIndexJRso[] statisIndexs;
	
	/*
	 * 采集时间单位 SE：秒  MU:分 HO：时  DA：天，MO：月
	 * @See StatisConfig.UNIT_SE
	 * @See StatisConfig.UNIT_MU
	 * @See StatisConfig.UNIT_HO
	 * @See StatisConfig.UNIT_DA
	 * @See StatisConfig.UNIT_MO
	 */
	private String timeUnit = UNIT_SE;
	
	//多少个时间单位
	private int timeCnt=1;
	
	private int counterTimeout = 5*60;
	
	private int minNotifyTime = 10000;
	
	/* 
	 *    统计结果发送目标，如存库，转发RPC方法，publish消息，输出控制台等
	 * 
	 * @See StatisConfig.TO_TYPE_DB
	 * @See StatisConfig.TO_TYPE_SERVICE_METHOD
	 * @See StatisConfig.TO_TYPE_CONSOLE
	 * @See StatisConfig.TO_TYPE_FILE
	 */
	private int toType;
	
	/*
	 * 根据toType类型决定其值格式，如存库，表示表名，转发RPC则是RPC方法的KEY，publish则是消息主题，控制台输出格式
	 */
	private String toParams;
	
	private String actName = "";
	
	//此配置由谁创建，决定匹配数据结果由谁可见
	private int createdBy;
	
	//是否启用
	private boolean enable;
	
	private String tag;
	
	private String namedType;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}


	public int getTimeCnt() {
		return timeCnt;
	}

	public void setTimeCnt(int timeCnt) {
		this.timeCnt = timeCnt;
	}

	public String getByKey() {
		return byKey;
	}

	public void setByKey(String byKey) {
		this.byKey = byKey;
	}


	public long getLastActiveTime() {
		return lastActiveTime;
	}

	public void setLastActiveTime(long lastActiveTime) {
		this.lastActiveTime = lastActiveTime;
	}

	public StatisIndexJRso[] getStatisIndexs() {
		return statisIndexs;
	}

	public void setStatisIndexs(StatisIndexJRso[] statisIndexs) {
		this.statisIndexs = statisIndexs;
	}

	public String getTimeUnit() {
		return timeUnit;
	}

	public void setTimeUnit(String timeUnit) {
		this.timeUnit = timeUnit;
	}

	public String getToParams() {
		return toParams;
	}

	public void setToParams(String toParams) {
		this.toParams = toParams;
	}

	public int getByType() {
		return byType;
	}

	public void setByType(int byType) {
		this.byType = byType;
	}

	public int getToType() {
		return toType;
	}

	public void setToType(int toType) {
		this.toType = toType;
	}

	public int getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(int createdBy) {
		this.createdBy = createdBy;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getNamedType() {
		return namedType;
	}

	public void setNamedType(String namedType) {
		this.namedType = namedType;
	}

	public String getActName() {
		return actName;
	}

	public void setActName(String actName) {
		this.actName = actName;
	}

	public BufferedWriter getBw() {
		return bw;
	}

	public void setBw(BufferedWriter bw) {
		this.bw = bw;
	}

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int forClientId) {
		this.clientId = forClientId;
	}

	public Set<Short> getTypes() {
		return types;
	}

	public void setTypes(Set<Short> types) {
		this.types = types;
	}

	public String getBysn() {
		return bysn;
	}

	public void setBysn(String bysn) {
		this.bysn = bysn;
	}

	public String getByns() {
		return byns;
	}

	public void setByns(String byns) {
		this.byns = byns;
	}

	public String getByver() {
		return byver;
	}

	public void setByver(String byver) {
		this.byver = byver;
	}

	public String getByme() {
		return byme;
	}

	public void setByme(String byme) {
		this.byme = byme;
	}

	public String getByins() {
		return byins;
	}

	public void setByins(String byins) {
		this.byins = byins;
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

	public String getExpStr() {
		return expStr;
	}

	public void setExpStr(String expStr) {
		this.expStr = expStr;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

	public int getCounterTimeout() {
		return counterTimeout;
	}

	public void setCounterTimeout(int counterTimeout) {
		this.counterTimeout = counterTimeout;
	}

	public long getLastNotifyTime() {
		return lastNotifyTime;
	}

	public void setLastNotifyTime(long lastNotifyTime) {
		this.lastNotifyTime = lastNotifyTime;
	}

	public int getMinNotifyTime() {
		return minNotifyTime;
	}

	public void setMinNotifyTime(int minNotifyTime) {
		this.minNotifyTime = minNotifyTime;
	}
	
	public void changeExpIndex() {
		if(this.exp[1] != null) {
			this.expIndex =  0 == this.expIndex ? 1 : 0;
		}
	}

	public Exp getExp() {
		return exp[this.expIndex];
	}

	public void setExp0(Exp exp0) {
		exp[0] = exp0;
	}
	
	public void setExp1(Exp exp1) {
		exp[1] = exp1;
	}

	public String getExpStr1() {
		return expStr1;
	}

	public void setExpStr1(String expStr1) {
		this.expStr1 = expStr1;
	}

}
