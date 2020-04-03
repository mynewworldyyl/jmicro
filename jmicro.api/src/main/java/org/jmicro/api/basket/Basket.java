package org.jmicro.api.basket;

public class Basket<T> {
	
	private Object[] s = null;
	
	private int capacity;
	
	private int readIndex;
	
	private int writeIndex;

	private boolean readStatus = false;
	
	public Basket(int capacity) {
		s = new Object[capacity];
		readIndex = -1;
		writeIndex = 0;
	}
	
	public boolean add(T elt) {
		if(readStatus) {
			return false;
		}
		if(writeIndex < this.capacity) {
			s[writeIndex++] = elt;
			return true;
		}
		return false;
	}
	
	public boolean add(T[] elts) {
		if(readStatus) {
			return false;
		}
		if(writeIndex + elts.length <= this.capacity) {
			System.arraycopy(elts, 0, s, writeIndex, elts.length);
			writeIndex += elts.length;
			return true;
		}
		return false;
	}
	
	public int remainding() {
		if(readStatus) {
			//剩余可读元素个数
			return writeIndex - readIndex-1;
		} else {
			//剩余可写元素个数
			return this.capacity - writeIndex;
		}
		
	}
	
	public boolean exchangeStatus() {
		if(this.readStatus) {
			//由读状态变为写状态，当前元素个数必须小于容量
			if(remainding() > 0) {
				//还剩余未读元素，必须读完才能交换
				return false;
			} else {
				this.readStatus = !this.readStatus;
				return true;
			}
		} else {
			//由写状态切换到读状态，必须有元素可读，否则没意义
			if((writeIndex - readIndex-1) > 0) {
				this.readStatus = !this.readStatus;
				return true;
			}else {
				return false;
			}			
		}		
	}
	
	public boolean isReadStatus() {
		return this.readStatus;
	}
	
	public boolean isWriteStatus() {
		return !this.readStatus;
	}
	
	public T[] getAll() {
		int size = remainding();
		if(!readStatus || size == 0) {
			return null;
		}
		
		Object[] arr = new Object[size];
		System.arraycopy(this.s, 0, arr, 0, size);
		
		readIndex = this.writeIndex-1;
	
		return (T[])arr;
	}
	
	public T get() {
		if(!readStatus || remainding() == 0) {
			return null;
		}
		return (T) this.s[++readIndex];
	}
	
	
}
