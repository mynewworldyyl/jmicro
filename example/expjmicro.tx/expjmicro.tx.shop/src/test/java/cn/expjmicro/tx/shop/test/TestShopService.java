package cn.expjmicro.tx.shop.test;

import org.junit.Test;

import cn.expjmicro.example.tx.api.ITxShopService;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestShopService extends JMicroBaseTestCase{

	protected String[] getArgs() {
		return new String[] {"-DinstanceName=JMicroBaseTestCase","-DclientId=0","-DadminClientId=0","-DpriKeyPwd=comsumer"
		,"-DsysLogLevel=1","-Dlog4j.configuration=../../../log4j.xml","-Dpwd=0","-DsysLogLevel=1"};
	}
	
	@Test
	public void testBuy() {
		ITxShopService shopSrv = of.get(ITxShopService.class);// of.getRemoteServie(ITxShopService.class.getName(), "shop","0.0.1", null);
		shopSrv.buy(1, 1);
		this.waitForReady(1000000);
	}
	
}
