package cn.jmicro.ext.mybatis;

import org.apache.ibatis.session.SqlSession;

public interface CurSqlSessionFactory {

	  SqlSession curSession();
	  void commitAndCloseCurSession();
	  void rollbackAndCloseCurSession();
	  void remove();
}
