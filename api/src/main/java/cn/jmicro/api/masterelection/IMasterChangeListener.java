package cn.jmicro.api.masterelection;

public interface IMasterChangeListener {

	public static final int MASTER_OFFLINE = 1;
	
	public static final int MASTER_ONLINE = 2;
	
	public static final int MASTER_NOTSUPPORT = 3;
	
	void masterChange(int type,boolean asMaster);
	
}
