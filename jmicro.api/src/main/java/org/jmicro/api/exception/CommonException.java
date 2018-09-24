package org.jmicro.api.exception;

public class CommonException extends RuntimeException {
	 
	private static final long serialVersionUID = 13434325523L;
	
	private String key = "";

	public CommonException(String cause){
		super(cause);
	}
	
	public CommonException(String cause,Throwable exp){
		super(cause,exp);
	}
	
	public CommonException(String key,String cause){
		this(key,cause,null);
	}
	
	public CommonException(String key,String cause,Throwable exp){
		super(cause,exp);
		this.key= key;
	}
}
