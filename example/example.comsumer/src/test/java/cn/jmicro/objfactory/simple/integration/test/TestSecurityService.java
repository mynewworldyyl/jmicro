package cn.jmicro.objfactory.simple.integration.test;

import org.junit.Test;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.api.security.IAccountService;
import cn.jmicro.api.security.ISecretService;
import cn.jmicro.api.security.JmicroPublicKey;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestSecurityService extends JMicroBaseTestCase{
	
	@Test
	public void testGetPublicKey() {
		ISecretService srv = this.getSrv(ISecretService.class, "sec","0.0.1");
		Resp<String> r = srv.getPublicKeyByInstance("security");
		System.out.println(r.getData());
	}
	
	@Test
	public void testCreatePublicKey() {
		IAccountService srv = getSrv(IAccountService.class, "sec","0.0.1");
		Resp<ActInfo> r = srv.login("jmicro", "0");
		System.out.println(JsonUtils.getIns().toJson(r));
		
		org.junit.Assert.assertTrue(r.getCode() == 0);
		org.junit.Assert.assertNotNull(r.getData());
		
		JMicroContext.get().setString(JMicroContext.LOGIN_KEY, r.getData().getLoginKey());
		JMicroContext.get().setAccount(r.getData());
		
		ISecretService secSrv = this.getSrv(ISecretService.class, "sec","0.0.1");
		Resp<JmicroPublicKey> rj = secSrv.createSecret("mng", "mng123");
		
		org.junit.Assert.assertNotNull(rj);
		org.junit.Assert.assertTrue(rj.getCode() == 0);
		org.junit.Assert.assertNotNull(rj.getData());
		
		System.out.println(JsonUtils.getIns().toJson(rj.getData()));
		
		this.waitForReady(1000*10*60);
		
	}
}
