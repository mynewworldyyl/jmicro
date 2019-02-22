package org.jmicro.ext.mybatis;

import java.sql.Connection;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;

public class SqlSessionManager implements SqlSessionFactory,CurSqlSessionFactory{

	private SqlSessionFactory ssf;
	
	private ThreadLocal<SqlSession> curSession = new ThreadLocal<>();

	public SqlSessionManager(SqlSessionFactory sqlSessionFactory) {
		this.ssf = sqlSessionFactory;
	}
	
	@Override
	public SqlSession curSession() {
		if(curSession.get() != null) {
			return curSession.get();
		}
		curSession.set(ssf.openSession(true));
		return curSession.get();
	}
	
	public void closeCurSession() {
		if(curSession.get() == null) {
			return ;
		}
		curSession.get().close();
		curSession.set(null);
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
