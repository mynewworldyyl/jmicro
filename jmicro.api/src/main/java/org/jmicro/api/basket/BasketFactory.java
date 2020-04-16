package org.jmicro.api.basket;

/**
 * 专门对外提供篮子服务，并保证篮子不会重复借出，直到篮子归还为止。
 * 如果借出的是读篮子，归还前必须把篮子的全部数据读完，否则不能归还。
 * 如果是写篮子，则可以归还，即使没有写数据
 * 
 * @author Yulei Ye
 * @date 2020年4月3日
 */
public class BasketFactory<T> {

	private int size = 10;
	private IBasket<T>[] baskets = null;
	private boolean[] using = null;
	
	private Object readLocker = new Object();
	private Object writeLocker = new Object();
	
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
	
	public IBasket<T> borrowWriteBasket() {
		synchronized(writeLocker) {
			for(int i =0; i < size; i++) {
				if(!using[i] && baskets[i].isWriteStatus()) {
					using[i] = true;
					return baskets[i];
				}
			}
		}
		return null;
	}
	
	public IBasket<T> borrowReadSlot() {
		synchronized(readLocker) {
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
		
		public boolean add(E elt) {
			if(readStatus) {
				return false;
			}
			if(writeIndex < this.capacity) {
				s[writeIndex++] = elt;
				if(firstWriteTime == 0) {
					firstWriteTime = System.currentTimeMillis();
				}
				return true;
			}
			return false;
		}
		
		public boolean add(E[] elts) {
			if(readStatus) {
				return false;
			}
			if(writeIndex + elts.length <= this.capacity) {
				System.arraycopy(elts, 0, s, writeIndex, elts.length);
				writeIndex += elts.length;
				if(firstWriteTime == 0) {
					firstWriteTime = System.currentTimeMillis();
				}
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
		
		public boolean getAll(E[] arr) {
			if(!readStatus) {
				return false;
			}
			
			int size = remainding();
			if(size == 0 || size != arr.length) {
				return false;
			}
			
			for(int i = 0; i < size ; i++) {
				arr[i] = get();
			}
			
			return true;
		}
		
		@SuppressWarnings("unchecked")
		public E get() {
			if(!readStatus || remainding() == 0) {
				return null;
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
