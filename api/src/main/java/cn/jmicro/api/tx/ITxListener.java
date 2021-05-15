package cn.jmicro.api.tx;

public interface ITxListener {

	void onTxResult(boolean commit,long txid);
}
