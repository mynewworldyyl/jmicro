package cn.jmicro.ext.mybatis;

import java.sql.Connection;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.tx.ICurTxSessionFactory;
import cn.jmicro.api.tx.ITxSession;
import cn.jmicro.api.tx.TxConstants;
import cn.jmicro.common.CommonException;

public class SqlSessionManager implements SqlSessionFactory,ICurTxSessionFactory{

	private SqlSessionFactory ssf;
	
	//private ThreadLocal<CurSqlSession> curSession = new ThreadLocal<>();

	public SqlSessionManager(SqlSessionFactory sqlSessionFactory) {
		this.ssf = sqlSessionFactory;
	}
	
	@Override
	public ITxSession curSession() {
		CurSqlSession s = JMicroContext.get().getParam(TxConstants.TX_SESSION_KEY, null);
		if(s == null) {
			 s = new CurSqlSession(ssf.openSession(false));
			 JMicroContext.get().setParam(TxConstants.TX_SESSION_KEY,s);
		}
		return s;
	}
	
	public void commitAndCloseCurSession() {
		CurSqlSession s = JMicroContext.get().getParam(TxConstants.TX_SESSION_KEY, null);
		if(s == null) {
			return ;
		}
		s.commit(false);
		s.close();
		remove();
	}
	
	public void rollbackAndCloseCurSession() {
		CurSqlSession s = JMicroContext.get().getParam(TxConstants.TX_SESSION_KEY, null);
		if(s == null) {
			return ;
		}
		s.rollback(true);
		s.close();
		remove();
	}

	public void remove() {
		JMicroContext.get().removeParam(TxConstants.TX_SESSION_KEY);
	}
	
	@Override
	public SqlSession openSession() {
		return ssf.openSession();
	}

	@Override
	public SqlSession openSession(boolean autoCommit) {
		return ssf.openSession(autoCommit);
	}

	@Override
	public SqlSession openSession(Connection connection) {
		return ssf.openSession(connection);
	}

	@Override
	public SqlSession openSession(TransactionIsolationLevel level) {
		return ssf.openSession(level);
	}

	@Override
	public SqlSession openSession(ExecutorType execType) {
		return ssf.openSession(execType);
	}

	@Override
	public SqlSession openSession(ExecutorType execType, boolean autoCommit) {
		return ssf.openSession(execType, autoCommit);
	}

	@Override
	public SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level) {
		return ssf.openSession(execType,level);
	}

	@Override
	public SqlSession openSession(ExecutorType execType, Connection connection) {
		return ssf.openSession(execType, connection);
	}

	@Override
	public Configuration getConfiguration() {
		return this.ssf.getConfiguration();
	}
	
}
