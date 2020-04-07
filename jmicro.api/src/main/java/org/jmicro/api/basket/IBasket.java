package org.jmicro.api.basket;

public interface IBasket<T> {
	
	public boolean add(T elt);
	
	public boolean add(T[] elts);
	
	public int remainding();
	
	public boolean exchangeStatus();
	
	public boolean isReadStatus();
	
	public boolean isWriteStatus();
	
	public boolean getAll(T[] arr);
	
	public T get();
}
