package cn.expjmicro.example.tx.api.entities;

import lombok.Data;

@Data
public class Good {

	private long id;
	private String name;
	private double price;
	
	private int total;
	
	private int usableCnt;
	
}
