package cn.expjmicro.example.tx.api.entities;

import cn.jmicro.api.annotation.IDStrategy;
import cn.jmicro.api.annotation.SO;
import lombok.Data;

@SO
@Data
@IDStrategy
public class Order {

	private long id;
	
	private long goodId;
	
	private int num;
	
	private double amount;
	
	private long txid;
}
