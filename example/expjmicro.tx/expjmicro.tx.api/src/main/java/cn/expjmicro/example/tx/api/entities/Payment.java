package cn.expjmicro.example.tx.api.entities;

import cn.jmicro.api.annotation.IDStrategy;
import cn.jmicro.api.annotation.SO;
import lombok.Data;

@SO
@Data
@IDStrategy
public class Payment {

	private long id;
	
	private long orderId;
	
	private double amount;
	
	private long txid;
	
}
