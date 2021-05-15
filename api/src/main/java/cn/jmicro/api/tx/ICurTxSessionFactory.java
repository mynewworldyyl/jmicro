package cn.jmicro.api.tx;

public interface ICurTxSessionFactory {

	  ITxSession curSession();
	  void commitAndCloseCurSession();
	  void rollbackAndCloseCurSession();
	  void remove();
}
