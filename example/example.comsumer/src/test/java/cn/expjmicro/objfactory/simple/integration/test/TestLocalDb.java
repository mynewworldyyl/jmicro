package cn.expjmicro.objfactory.simple.integration.test;

import org.junit.Test;

import cn.jmicro.api.Resp;
import cn.jmicro.api.data.LocalDataManager;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestLocalDb  extends JMicroBaseTestCase{

	@Test
	public void testInsertData() {
		LocalDataManager db = of.get(LocalDataManager.class);
		db.registTableClass("t_test_data", TestDbTable.class);
		
		TestDbTable d = new TestDbTable();
		d.setData0("test0");
		d.setData1(222);
		d.setId(0);
		db.insert("t_test_data", d);
		
		d = new TestDbTable();
		d.setData0("test1");
		d.setData1(333);
		d.setId(0);
		db.insert("t_test_data", d);
		
		d = new TestDbTable();
		d.setData0("test2");
		d.setData1(444);
		d.setId(0);
		db.insert("t_test_data", d);
		
		d = new TestDbTable();
		d.setData0("test3");
		d.setData1(555);
		d.setId(0);
		db.insert("t_test_data", d);
		
		this.waitForReady(300000);
	}
	
	@Test
	public void testInsertDataByOutID() {
		LocalDataManager db = new LocalDataManager();
		db.registTableClass("t_test_data", TestDbTable.class);
		
		TestDbTable d = new TestDbTable();
		d.setData0("test0");
		d.setData1(222);
		d.setId(27);
		Resp<Boolean> resp = db.insert("t_test_data", d);
		System.out.println(resp);
		
		this.waitForReady(300000);
	}
	
	
	@Test
	public void testUpdateDataById() {
		LocalDataManager db = new LocalDataManager(of);
		db.registTableClass("t_test_data", TestDbTable.class);
		TestDbTable d = new TestDbTable();
		d.setData0("test2222");
		d.setData1(6666);
		d.setId(40);
		Resp<Boolean> resp = db.updateById("t_test_data", d);
		System.out.println(resp);
		
		this.waitForReady(3000);
	}
	
	@Test
	public void testDeleteById() {
		LocalDataManager db = new LocalDataManager(of);
		db.registTableClass("t_test_data", TestDbTable.class);
		Resp<Boolean> resp = db.delete("t_test_data", 40);
		System.out.println(resp);
		
		this.waitForReady(3000);
	}
}
