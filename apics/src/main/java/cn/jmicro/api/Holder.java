package cn.jmicro.api;

public class Holder<T> {

	private T val;
	
	public Holder() {
	}
	
	public Holder(T v) {
		this.set(v);
	}
	
	public void set(T v) {
		this.val = v;
	}
	
	public T get() {
		return val;
	}
	
}
