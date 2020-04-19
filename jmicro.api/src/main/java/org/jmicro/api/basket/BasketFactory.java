package org.jmicro.api.basket;

import java.util.Iterator;

import org.jmicro.common.CommonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 专门对外提供篮子服务，并保证篮子不会重复借出，直到篮子归还为止。
 * 如果借出的是读篮子，归还前必须把篮子的全部数据读完，否则不能归还。
 * 如果是写篮子，则可以归还，即使没有写数据
 * 
 * @author Yulei Ye
 * @date 2020年4月3日
 */
public class BasketFactory<T>{

	private final static Logger logger = LoggerFactory.getLogger(BasketFactory.class);
	
	private int size = 10;
	private IBasket<T>[] baskets = null;
	private boolean[] using = null;
	
	private Object readLocker = new Object();
	private Object writeLocker = new Object();
	
	public Iterator<IBasket<T>> iterator(boolean read) {
		return new Iterator<IBasket<T>>() {
			int index = 0;
			@Override
			public boolean hasNext() {
				throw new CommonException("Not support");
			}

			@Override
			public IBasket<T> next() {
				
					if(index >= size) {
						return null;
					}
				
					if(read) {
						synchronized(writeLocker) {
							for(int i = index; i < size; i++) {
								if(!using[i] && baskets[i].isReadStatus()) {
									using[i] = true;
									index = i+1;
									return baskets[i];
								}
							}
						}
					} else {
						synchronized(writeLocker) {
							for(int i = index; i < size; i++) {
								if(!using[i] && baskets[i].isWriteStatus()) {
									using[i] = true;
									index = i+1;
									return baskets[i];
								}
							}
						}
					}
					return null;
				}
		};
	}
	
	@SuppressWarnings("unchecked")
	public BasketFactory(int size,int maxCapacityOfBasket) {
		this.size = size;
		baskets = new Basket[size];
		using = new boolean[size];
		for(int i =0; i < size; i++) {
			baskets[i] = new Basket<T>(maxCapacityOfBasket);
			using[i] = false;
		}
	}
	
	public IBasket<T> borrowWriteBasket(boolean ...doLog) {
		synchronized(writeLocker) {
			for(int i =0; i < size; i++) {
				if(!using[i] && baskets[i].isWriteStatus()) {
					using[i] = true;
					return baskets[i];
				}
			}
		}
		
		if(doLog.length == 1 && doLog[0]) {
			StringBuffer us = new StringBuffer("USING: ");
			StringBuffer sbstatus = new StringBuffer("WRITE: ");
			StringBuffer slot = new StringBuffer("BASKET: ");
			for(int i =0; i < size; i++) {
				us.append(using[i]).append(",");
				sbstatus.append(baskets[i].isWriteStatus()).append(",");
				slot.append(baskets[i].remainding()).append(",");
			}
			us.append("\n").append(sbstatus.toString()).append("\n").append(slot.toString()).append("\n")
			.append("=================================");
			System.out.println(us.toString());
		}
		
		
		return null;
	}
	
	public IBasket<T> borrowReadSlot() {
		synchronized(writeLocker) {
			for(int i =0; i < size; i++) {
				if(!using[i] && baskets[i].isReadStatus()) {
					using[i] = true;
					return baskets[i];
				}
			}
		}
		return null;
	}
	
	public boolean returnWriteBasket(IBasket<T> b,boolean returnStatus) {
		if(!b.isWriteStatus()) {
			return false;
		}
		return doReturn(b,returnStatus);
	}
	
	
	public boolean returnReadSlot(IBasket<T> b,boolean returnStatus) {
		if(!b.isReadStatus()) {
			return false;
		}
		return doReturn(b,returnStatus);
	}
	
	private int getBasketIndex(IBasket<T> b) {
		for(int i =0; i < size; i++) {
			if(baskets[i] == b) {
				return i;
			}
		}
		return -1;
	}
	
	private boolean doReturn(IBasket<T> b,boolean returnStatus) {
		int idx = getBasketIndex(b);
		if(idx < 0) {
			logger.error("return basket is not belong this basket factory!");
			return false;
		}
		
		if(returnStatus && !b.exchangeStatus()) {
			//状态切换失败
			return false;
		}
		//直到using[idx]=true前，篮子还是独占的，所以是线程安全的，也就不需要同步
		using[idx] = false;
		
		return true;
	}
	
	class Basket<E> implements IBasket<E>{
		
		private Object[] s = null;
		
		private int capacity;
		
		private int readIndex;
		
		private int writeIndex;

		private boolean readStatus = false;
		
		private long firstWriteTime;
		
		public Basket(int capacity) {
			this.capacity = capacity;
			s = new Object[capacity];
			readIndex = -1;
			writeIndex = 0;
			firstWriteTime = 0;
		}
		
		public boolean write(E elt) {
			if(readStatus) {
				logger.error("Basket status is read status, not write status!");
				return false;
			}
			if(writeIndex < this.capacity) {
				s[writeIndex++] = elt;
				if(firstWriteTime == 0) {
					firstWriteTime = System.currentTimeMillis();
				}
				return true;
			}else {
				logger.error("Basket is full, cannot write now!");
				return false;
			}
			
		}
		
		public boolean write(E[] elts,int srcPosition,int len) {
			if(readStatus) {
				logger.error("Basket status is read status, not write status!");
				return false;
			}
			if(writeIndex + len <= this.capacity) {
				System.arraycopy(elts, srcPosition, s, writeIndex, len);
				writeIndex += len;
				if(firstWriteTime == 0) {
					firstWriteTime = System.currentTimeMillis();
				}
				return true;
			} else {
				logger.error("Basket is full, cannot write now! NEED SIZE:"+elts.length+", CAN USE: "+this.remainding());
				return false;
			}
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
					logger.error("Have to read all the element before return this basket size: "+remainding());
					return false;
				} else {
					this.readStatus = false;
					readIndex = -1;
					writeIndex = 0;
					firstWriteTime = 0;
					return true;
				}
			} else {
				//由写状态切换到读状态，必须有元素可读，否则没意义
				if((writeIndex - readIndex-1) > 0) {
					this.readStatus = true;
					return true;
				} else {
					logger.error("No element to read for this basket");
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
		
		public boolean readAll(E[] arr) {
			if(!readStatus) {
				return false;
			}
			
			int size = remainding();
			if(size == 0 || size != arr.length) {
				return false;
			}
			
			for(int i = 0; i < size ; i++) {
				arr[i] = read();
			}
			
			return true;
		}
		
		@SuppressWarnings("unchecked")
		public E read() {
			if(!readStatus || remainding() == 0) {
				return null;
			}
			if(readIndex >=9) {
				System.out.print("");
			}
			E e = (E)this.s[++readIndex];
			this.s[readIndex] = null;
			return e;
		}

		@Override
		public long firstWriteTime() {
			return firstWriteTime;
		}

		/**
		 * 是否存在有效元素，即可读取的元素
		 */
		@Override
		public boolean isEmpty() {
			if(readStatus) {
				//剩余可读元素个数
				return writeIndex - readIndex - 1 == 0;
			} else {
				//剩余可写元素个数
				return writeIndex == 0;
			}
		}
		
	}
}
