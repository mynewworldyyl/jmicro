package cn.jmicro.api.gateway;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lombok.Getter;

@Getter
public class MessageRouteGroup {

	private String key;
	
	private String alg = "round";
	
	private List<MessageRouteRow> list = new LinkedList<>();
	
	private ReentrantReadWriteLock locker = new ReentrantReadWriteLock(true);

	public void removeRoute(int id) {
		ReentrantReadWriteLock.WriteLock  wl = locker.writeLock();
		if(wl.tryLock()) {
			try {
				Iterator<MessageRouteRow> ite = list.iterator();
				while(ite.hasNext()) {
					MessageRouteRow r = ite.next();
					if(id == r.getId()) {
						ite.remove();
					}
				}
			}finally {
				wl.unlock();
			}
		}
	}
	
	public void addRoute(MessageRouteRow rr) {
		ReentrantReadWriteLock.WriteLock  wl = locker.writeLock();
		if(wl.tryLock()) {
			try {
				list.add(rr);
			}finally {
				wl.unlock();
			}
		}
	}
	
	public void updateRoute(MessageRouteRow rr) {
		removeRoute(rr.getId());
		addRoute(rr);
	}
	
	public boolean isEmpty() {
		return list == null || list.isEmpty();
	}
	
	
	public void setKey(String k) {this.key = k;}
}
