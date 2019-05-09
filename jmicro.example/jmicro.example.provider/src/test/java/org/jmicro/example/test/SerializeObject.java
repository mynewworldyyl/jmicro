package org.jmicro.example.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jmicro.api.annotation.SO;
import org.jmicro.api.test.Person;

@SO
public class SerializeObject implements Serializable{

	private Date date = null;
	
	private String val1111 = "dfsadfffffffffffff";
	
	private byte bvssssssssssss = 1;
	
	private int iv222222222222222 = 9;
	
	private short sv33333333333333 = 52;
	
	private long lvdddddddddddddd = 2;
	
	private float fv222222222222222222222222 = 2222;
	
	private double dv22234324 = 22;
	
	private char cvfdksafjdlaj = '2';

	public int getIv222222222222222() {
		return iv222222222222222;
	}

	public void setIv222222222222222(int iv222222222222222) {
		this.iv222222222222222 = iv222222222222222;
	}
	
	public byte getBvssssssssssss() {
		return bvssssssssssss;
	}

	public void setBvssssssssssss(byte bvssssssssssss) {
		this.bvssssssssssss = bvssssssssssss;
	}

	public short getSv33333333333333() {
		return sv33333333333333;
	}

	public void setSv33333333333333(short sv33333333333333) {
		this.sv33333333333333 = sv33333333333333;
	}

	

	public long getLvdddddddddddddd() {
		return lvdddddddddddddd;
	}

	public void setLvdddddddddddddd(long lvdddddddddddddd) {
		this.lvdddddddddddddd = lvdddddddddddddd;
	}

	public float getFv222222222222222222222222() {
		return fv222222222222222222222222;
	}

	public void setFv222222222222222222222222(float fv222222222222222222222222) {
		this.fv222222222222222222222222 = fv222222222222222222222222;
	}

	public double getDv22234324() {
		return dv22234324;
	}

	public void setDv22234324(double dv22234324) {
		this.dv22234324 = dv22234324;
	}

	public char getCvfdksafjdlaj() {
		return cvfdksafjdlaj;
	}

	public void setCvfdksafjdlaj(char cvfdksafjdlaj) {
		this.cvfdksafjdlaj = cvfdksafjdlaj;
	}

	public String getVal1111() {
		return val1111;
	}

	public void setVal1111(String val1111) {
		this.val1111 = val1111;
	}

	
	
	public Set<Person> setv = new HashSet<>();
	
	public List<Person> listv = new ArrayList<>();
	
	/*public List<Person> listv = new ArrayList<>();
	
	public Map<String,Person> mapv = new HashMap<>();*/
	
	private Person p = new Person();
	
	
}
