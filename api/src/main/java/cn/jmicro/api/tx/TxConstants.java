package cn.jmicro.api.tx;

public interface TxConstants {

	public static final String TYPE_TX_KEY = "txid"; //分布式全局事务
	
	public static final byte TYPE_TX_NO = 0; //无事务，不支持事务，默认值
	
	public static final byte TYPE_TX_LOCAL = 1; //本地事务，不支持分布式
	
	public static final byte TYPE_TX_DISTRIBUTED = 3; //分布式全局事务
	
	public static final byte TX_2PC = 1; //2PC事务
	
	public static final byte TX_3PC = 2; //3PC事务
}
