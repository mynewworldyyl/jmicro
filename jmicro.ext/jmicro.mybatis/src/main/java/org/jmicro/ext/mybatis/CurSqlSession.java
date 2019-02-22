package org.jmicro.ext.mybatis;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

public class CurSqlSession implements SqlSession{

	private SqlSession s;
	
	public CurSqlSession(SqlSession s) {
		this.s = s;
	}

	@Override
	public <T> T selectOne(String statement) {
		return s.selectOne(statement);
	}

	@Override
	public <T> T selectOne(String statement, Object parameter) {
		return s.selectOne(statement, parameter);
	}

	@Override
	public <E> List<E> selectList(String statement) {
		return s.selectList(statement);
	}

	@Override
	public <E> List<E> selectList(String statement, Object parameter) {
		return s.selectList(statement, parameter);
	}

	@Override
	public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
		return s.selectList(statement, parameter, rowBounds);
	}

	@Override
	public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
		return s.selectMap(statement, mapKey);
	}

	@Override
	public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
		return s.selectMap(statement, parameter, mapKey);
	}

	@Override
	public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
		return s.selectMap(statement, parameter, mapKey, rowBounds);
	}

	@Override
	public <T> Cursor<T> selectCursor(String statement) {
		return s.selectCursor(statement);
	}

	@Override
	public <T> Cursor<T> selectCursor(String statement, Object parameter) {
		return s.selectCursor(statement, parameter);
	}

	@Override
	public <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds) {
		return s.selectCursor(statement, parameter, rowBounds);
	}

	@Override
	public void select(String statement, Object parameter, ResultHandler handler) {
		s.select(statement, parameter, handler);
		
	}

	@Override
	public void select(String statement, ResultHandler handler) {
		s.select(statement, handler);
	}

	@Override
	public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
		s.select(statement, parameter, rowBounds, handler);
	}

	@Override
	public int insert(String statement) {
		return s.insert(statement);
	}

	@Override
	public int insert(String statement, Object parameter) {
		return s.insert(statement, parameter);
	}

	@Override
	public int update(String statement) {
		return s.update(statement);
	}

	@Override
	public int update(String statement, Object parameter) {
		return s.update(statement, parameter);
	}

	@Override
	public int delete(String statement) {
		return s.delete(statement);
	}

	@Override
	public int delete(String statement, Object parameter) {
		return s.delete(statement, parameter);
	}

	@Override
	public void commit() {
		s.commit();
	}

	@Override
	public void commit(boolean force) {
		s.commit(force);
	}

	@Override
	public void rollback() {
		s.rollback();
	}

	@Override
	public void rollback(boolean force) {
		s.rollback(force);
	}

	@Override
	public List<BatchResult> flushStatements() {
		return s.flushStatements();
	}

	@Override
	public void close() {
		s.close();
	}

	@Override
	public void clearCache() {
		s.clearCache();
	}

	@Override
	public Configuration getConfiguration() {
		return s.getConfiguration();
	}

	@Override
	public <T> T getMapper(Class<T> type) {
		return s.getMapper(type);
	}

	@Override
	public Connection getConnection() {
		return s.getConnection();
	}
	
	
}
