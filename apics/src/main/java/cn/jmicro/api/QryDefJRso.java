package cn.jmicro.api;

import lombok.Data;

@Data
public class QryDefJRso {
	
	public static final byte OP_EQ = 1;
	
	public static final byte OP_REGEX = 2;
	
	public static final byte OP_IN = 3;
	
	public static final byte OP_GT = 4;
	
	public static final byte OP_GTE = 5;
	
	public static final byte OP_LT = 6;
	
	public static final byte OP_LTE = 7;
	
	//public static final byte OP_IN = 3;

	private String fn;
	
	private Object v;
	
	private byte opType;
	
}
