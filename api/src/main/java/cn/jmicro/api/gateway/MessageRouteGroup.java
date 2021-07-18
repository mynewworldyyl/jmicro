package cn.jmicro.api.gateway;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.registry.ServiceItemJRso;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageRouteGroup {

	//private String key;
	
	//private String alg = "round";
	
	private ProcessInfoJRso pi;
	
	private Set<ServiceItemJRso> srvItems = new HashSet<>();
	
	private List<MessageRouteRow> list = new LinkedList<>();
	
	private ReentrantReadWriteLock locker = new ReentrantReadWriteLock(true);

	public void removeRoute(int id) {
		ReentrantReadWriteLock.WriteLock  wl = locker.writeLock();
		if(wl.tryLock()) {
			try {
				Iterator<MessageRouteRow> ite = list.iterator();
				while(ite.hasNext()) {
					MessageRouteRow r = ite.next();
					if(id == r.getInsId()) {
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
		removeRoute(rr.getInsId());
		addRoute(rr);
	}
	
	public boolean isEmpty() {
		return list == null || list.isEmpty();
	}

	public void addServiceItem(ServiceItemJRso si) {
		srvItems.add(si);
	}

	public void updateServiceItem(ServiceItemJRso si) {
		srvItems.remove(si);
		srvItems.add(si);
	}

	public void removeServiceItem(ServiceItemJRso si) {
		srvItems.remove(si);
	}
}
