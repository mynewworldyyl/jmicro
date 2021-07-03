package cn.jmicro.api.tx;

public interface TxConstants {

	public static final String TX_SESSION_KEY = "txSessionKey";
	public static final String TX_ID = "txid"; //分布式全局事务
	public static final String TX_SERVER_ID = "txInsId"; //事务协调器ID
	
	public static final byte TYPE_TX_NO = 0; //无事务，不支持事务，默认值
	
	public static final byte TYPE_TX_LOCAL = 1; //本地事务，不支持分布式
	
	public static final byte TYPE_TX_DISTRIBUTED = 3; //分布式全局事务
	
	public static final byte TYPE_S_WITHOUT_TX = 4;//打开数据库连接，但不开启事务
	
	public static final byte TX_2PC = 1; //2PC事务
	
	public static final byte TX_3PC = 2; //3PC事务
}
