package cn.expjmicro.example.tx.api.entities;

import cn.jmicro.api.annotation.SO;
import lombok.Data;

@SO
@Data
public class Good {

	private long id;
	private String name;
	private double price;
	
	private int total;
	
	private int usableCnt;
	
}
