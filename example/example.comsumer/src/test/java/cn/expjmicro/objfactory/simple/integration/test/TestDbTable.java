package cn.expjmicro.objfactory.simple.integration.test;

import cn.jmicro.api.annotation.IDStrategy;
import cn.jmicro.api.annotation.Tid;

@IDStrategy
public class TestDbTable {

	@Tid
	private int id;
	
	private String data0;
	
	private Integer data1;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getData0() {
		return data0;
	}

	public void setData0(String data0) {
		this.data0 = data0;
	}

	public Integer getData1() {
		return data1;
	}

	public void setData1(Integer data1) {
		this.data1 = data1;
	}
	
	
}
