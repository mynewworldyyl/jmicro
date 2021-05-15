package cn.jmicro.api.tx;

public interface ITxSession {

	public void commit();

	public void commit(boolean force);

	public void rollback();

	public void rollback(boolean force);
}
