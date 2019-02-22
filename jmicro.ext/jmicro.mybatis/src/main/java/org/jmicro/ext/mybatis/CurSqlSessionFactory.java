package org.jmicro.ext.mybatis;

import org.apache.ibatis.session.SqlSession;

public interface CurSqlSessionFactory {

	  SqlSession curSession();
	  void closeCurSession();

}
