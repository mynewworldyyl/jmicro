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
	private Basket<T>[] baskets = new Basket[10];
	private boolean[] using = new boolean[10];
	
	private Object locker = new Object();
	
	public BasketFactory(int size,int maxCapacityOfBasket) {
		this.size = size;
		baskets = new Basket[size];
		using = new boolean[size];
		for(int i =0; i < size; i++) {
			baskets[i] = new Basket(maxCapacityOfBasket);
			using[i] = false;
		}
	}
	
	public Basket<T> borrowWriteSlot() {
		synchronized(locker) {
			for(int i =0; i < size; i++) {
				if(!using[i] && baskets[i].isWriteStatus()) {
					using[i] = true;
					return baskets[i];
				}
			}
		}
		return null;
	}
	
	
	public Basket<T> borrowReadSlot() {
		synchronized(locker) {
			for(int i =0; i < size; i++) {
				if(!using[i] && baskets[i].isReadStatus()) {
					using[i] = true;
					return baskets[i];
				}
			}
		}
		return null;
	}
	
	public boolean returnWriteSlot(Basket<T> b,boolean returnStatus) {
		if(!b.isWriteStatus()) {
			return false;
		}
		return doReturn(b,returnStatus);
	}
	
	
	public boolean returnReadSlot(Basket<T> b,boolean returnStatus) {
		if(!b.isReadStatus()) {
			return false;
		}
		return doReturn(b,returnStatus);
	}
	
	private int getBasketIndex(Basket<T> b) {
		for(int i =0; i < size; i++) {
			if(baskets[i] == b) {
				return i;
			}
		}
		return -1;
	}
	
	private boolean doReturn(Basket<T> b,boolean returnStatus) {
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
}
