package org.jmicro.api.basket;

public interface IBasket<T> {
	
	public long firstWriteTime();
	
	/**
	 * 是否存在有效元素，即可读取的元素
	 */
	public boolean isEmpty();
	
	public boolean add(T elt);
	
	public boolean add(T[] elts);
	
	public int remainding();
	
	public boolean exchangeStatus();
	
	public boolean isReadStatus();
	
	public boolean isWriteStatus();
	
	public boolean getAll(T[] arr);
	
	public T get();
}
