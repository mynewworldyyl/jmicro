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

	private String val1111 = "dfsadfffffffffffff";
	
	private byte bvssssssssssss = 1;
	
	private int iv222222222222222 = 9;
	
	private short sv33333333333333 = 52;
	
	private long lvdddddddddddddd = 2;
	
	private float fv222222222222222222222222 = 2222;
	
	private double dv22234324 = 22;
	
	private char cvfdksafjdlaj = '2';
	
	private Date date = new Date();
	
	public Set<Person> setv = new HashSet<>();
	
	//public List<Person> listv = new ArrayList<>();
	
	/*public List<Person> listv = new ArrayList<>();
	
	public Map<String,Person> mapv = new HashMap<>();*/
	
	//private Person p = new Person();
	
	//public Set<Integer> seti = new HashSet<>();
	
}
