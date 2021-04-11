package cn.expjmicro.example.tx.api.entities;

import lombok.Data;

@Data
public class Req {
	private long txid;
	private int goodId;
	private int num;
	
	//private Timestamp time;
}
