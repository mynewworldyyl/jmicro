package cn.jmicro.api.monitor;

import java.io.BufferedWriter;
import java.lang.reflect.Method;
import java.util.Set;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.exp.Exp;

@SO
public class StatisConfig {

	public static final String UNIT_SE="Second";
	public static final String UNIT_MU="Munites";
	public static final String UNIT_HO="Hour";
	public static final String UNIT_DA="Date";
	public static final String UNIT_MO="Month";
	
	public static final String BY_TYPE_SERVICE = "Service";
	public static final String BY_TYPE_SERVICE_INSTANCE = "ServiceInstance";
	public static final String BY_TYPE_SERVICE_ACCOUNT = "ServiceAccount";
	
	public static final String BY_TYPE_SERVICE_METHOD = "ServiceMethod";
	public static final String BY_TYPE_SERVICE_INSTANCE_METHOD = "ServiceInstanceMethod";
	public static final String BY_TYPE_SERVICE_ACCOUNT_METHOD = "ServiceAccountMethod";
	
	public static final String BY_TYPE_INSTANCE = "ClientInstance";
	public static final String BY_TYPE_ACCOUNT= "Account";
	
	//public static final String BY_TYPE_EXP = "Expression";
	
	public static final String TO_TYPE_DB = "DB";
	public static final String TO_TYPE_SERVICE_METHOD = "ServiceMethod";
	public static final String TO_TYPE_CONSOLE = "Console";
	public static final String TO_TYPE_FILE = "File";
	
	public static final String  DEFAULT_DB = "t_statis_data";
	
	/*public static final int EXP_TYPE_SERVICE = 1;
	public static final int EXP_TYPE_ACCOUNT = 2;
	public static final int EXP_TYPE_INSTANCE = 3;*/
	
	private transient BufferedWriter bw;
	
	/*
	 * -1：不限账号
	 * 0：Admin账号
	 * >0: 其他普通账号
	 */
	private transient int forClientId = -1;
	
	//toType == TO_TYPE_SERVICE_METHOD时有效
	private transient Object srv;
	
	//toType == TO_TYPE_SERVICE_METHOD时有效
	private transient Method srvMethod;
	
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
	
	private transient Exp exp;
	
	private int id;
	
	//服务方法，实例方法，服务，账户
	private String byType;
	
	/*
	 * byType 下对应的key，如账户：test01, jmicro, *表示全部
	 * byType == BY_TYPE_SERVICE  bykey = service key
	 * byType == BY_TYPE_SERVICE_METHOD  bykey = service method key
	 * byType == BY_TYPE_SERVICE_INSTANCE  bykey = service instance key with instance name or host and port
	 * byType == BY_TYPE_SERVICE_INSTANCE_METHOD  bykey = service method key with instance name or host and port
	 * byType == BY_TYPE_SERVICE_ACCOUNT  bykey = account name
	 * byType == BY_TYPE_CLIENT_INSTANCE  bykey = jvm instance name
	 * BY_TYPE_SERVICE_METHOD_ACCOUNT
	 * 
	 */
	private String byKey;
	
	//表达式类型：服务，实例，账号
	//private int expForType;
	
	private String expStr;
	
	/*
	 * qps, total,current total, total percent, current percent
	 * 
	 * @See MC.PREFIX_QPS
	 * @See MC.PREFIX_TOTAL_PERCENT
	 * @See MC.PREFIX_CUR_PERCENT
	 * @See MC.PREFIX_TOTAL
	 * @See MC.PREFIX_CUR
	 */
	private String[] statisIndexs;
	
	/*
	 * 采集时间单位 SE：秒  MU:分 HO：时  DA：天，MO：月
	 * @See StatisConfig.UNIT_SE
	 * @See StatisConfig.UNIT_MU
	 * @See StatisConfig.UNIT_HO
	 * @See StatisConfig.UNIT_DA
	 * @See StatisConfig.UNIT_MO
	 */
	private String timeUnit = UNIT_MU;
	
	//多少个时间单位
	private int timeCnt=1;
	
	/* 
	 * 统计结果发送目标，如存库，转发RPC方法，publish消息，输出控制台等
	 * @See StatisConfig.TO_TYPE_DB
	 * @See StatisConfig.TO_TYPE_SERVICE_METHOD
	 * @See StatisConfig.TO_TYPE_CONSOLE
	 * @See StatisConfig.TO_TYPE_FILE
	 * 
	 */
	private String toType;
	
	/*
	 * 根据toType类型决定其值格式，如存库，表示表名，转发RPC则是RPC方法的KEY，publish则是消息主题，控制台输出格式
	 * 
	 */
	private String toParams;
	
	private String actName = "";
	
	//此配置由谁创建
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

	public String getByType() {
		return byType;
	}

	public int getTimeCnt() {
		return timeCnt;
	}

	public void setTimeCnt(int timeCnt) {
		this.timeCnt = timeCnt;
	}

	public void setByType(String byType) {
		this.byType = byType;
	}

	public String getByKey() {
		return byKey;
	}

	public void setByKey(String byKey) {
		this.byKey = byKey;
	}


	public String[] getStatisIndexs() {
		return statisIndexs;
	}

	public void setStatisIndexs(String[] statisIndexs) {
		this.statisIndexs = statisIndexs;
	}

	public String getTimeUnit() {
		return timeUnit;
	}

	public void setTimeUnit(String timeUnit) {
		this.timeUnit = timeUnit;
	}

	public String getToType() {
		return toType;
	}

	public void setToType(String toType) {
		this.toType = toType;
	}

	public String getToParams() {
		return toParams;
	}

	public void setToParams(String toParams) {
		this.toParams = toParams;
	}

	/*public int getForClientId() {
		return forClientId;
	}

	public void setForClientId(int forClientId) {
		this.forClientId = forClientId;
	}*/

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

	public int getForClientId() {
		return forClientId;
	}

	public void setForClientId(int forClientId) {
		this.forClientId = forClientId;
	}

	public Object getSrv() {
		return srv;
	}

	public void setSrv(Object srv) {
		this.srv = srv;
	}

	public Method getSrvMethod() {
		return srvMethod;
	}

	public void setSrvMethod(Method srvMethod) {
		this.srvMethod = srvMethod;
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

	public Exp getExp() {
		return exp;
	}

	public void setExp(Exp exp) {
		this.exp = exp;
	}

	public String getExpStr() {
		return expStr;
	}

	public void setExpStr(String expStr) {
		this.expStr = expStr;
	}


}
