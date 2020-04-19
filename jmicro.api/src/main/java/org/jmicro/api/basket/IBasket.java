package org.jmicro.api.basket;

public interface IBasket<T> {
	
	public long firstWriteTime();
	
	/**
	 * 是否存在有效元素，即可读取的元素
	 */
	public boolean isEmpty();
	
	public boolean write(T elt);
	
	public boolean write(T[] elts,int srcPosition,int len);
	
	public int remainding();
	
	public boolean exchangeStatus();
	
	public boolean isReadStatus();
	
	public boolean isWriteStatus();
	
	public boolean readAll(T[] arr);
	
	public T read();
}
