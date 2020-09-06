package cn.jmicro.example.test.logservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import cn.jmicro.api.Resp;
import cn.jmicro.api.mng.LogEntry;
import cn.jmicro.mng.api.ILogService;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestLogService  extends JMicroBaseTestCase{

	@Test
	public void tesCount() {
		ILogService srv = of.getRemoteServie(ILogService.class.getName(), "mng", "0.0.1", null);
		Map<String,String> params = new HashMap<String,String>();
		Resp<Long> rst = srv.count(params);
		System.out.println(rst);
		Assert.assertNotNull(rst);
		Assert.assertTrue(rst.getCode() == Resp.CODE_SUCCESS);
	}
	
	@Test
	public void tesQeury() {
		ILogService srv = of.get(ILogService.class);
		Map<String,String> params = new HashMap<String,String>();
		Resp<List<LogEntry>> rst = srv.query(params,10,0);
		System.out.println(rst);
		Assert.assertNotNull(rst);
		Assert.assertTrue(rst.getCode() == Resp.CODE_SUCCESS);
	}
	
	@Test
	public void tesGetDicts() {
		ILogService srv = of.get(ILogService.class);
		Resp<Map<String,Object>> rst = srv.queryDict();
		System.out.println(rst);
		Assert.assertNotNull(rst);
		Assert.assertTrue(rst.getCode() == Resp.CODE_SUCCESS);
	}
	
	
}