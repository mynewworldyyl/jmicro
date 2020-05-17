package cn.jmicro.main.monitor.v1;

import java.util.Date;
import java.util.Set;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.common.util.DateUtils;
import cn.jmicro.common.util.JsonUtils;

//@Component
public class ServicesMonitor {

	private static final String SERVICE_EVENT = "jmicro_serviceEvent";
	
	@Cfg("/Monitor/ServicesMonitor/enable")
	private boolean enable = true;
	
	@Inject
	private IRegistry reg;
	
	@Inject
	private ServiceManager srvManager;
	
	@Inject
	private MongoDatabase mongoDb;
	
	private long time = System.currentTimeMillis();
	
	public void init() {
		
		srvManager.addListener((type,item)->{
			doServiceInstanceEvent(type,item);
		});
		
		//reg.addServiceListener(key, lis);
		
	}

	private void doServiceInstanceEvent(int type, ServiceItem item) {
		if(System.currentTimeMillis() - time < 3000) {
			return;
		}
		
		Document doc = docIns(type,item,"");
		
		if(IServiceListener.DATA_CHANGE != type ) {
			doSave(doc);
			doc.remove("_id");
		}
		
		Set<ServiceItem> items = srvManager.getServiceItems(item.getKey().getServiceName(),
				item.getKey().getNamespace()
				, item.getKey().getVersion());
		switch(type) {
		case IServiceListener.REMOVE:
			if(items == null) {
				doc.put("desc", "down");
			}
			break;
		case IServiceListener.DATA_CHANGE:
			doc.put("desc", "datachange");
			break;
		case IServiceListener.ADD:
			if(items.size() == 1) {
				doc.put("desc", "up");
			}
			break;
		}
		
		doSave(doc);
		
	}

	private Document docIns(int type, ServiceItem item,String desc) {
		String jsonItem = JsonUtils.getIns().toJson(item);
		Document doc = new Document();
		doc.put("type", type);
		
		switch(type) {
		case IServiceListener.REMOVE:
			desc = "remove";
			break;
		case IServiceListener.DATA_CHANGE:
			desc = "datachange";
			break;
		case IServiceListener.ADD:
			desc = "add";
			break;
		}
		
		doc.put("item", jsonItem);
		doc.append("createTime", DateUtils.formatDate(new Date(), DateUtils.PATTERN_YYYY_MM_DD_HHMMSSZZZ));
		doc.put("desc", desc);
		doc.put("sn", item.getKey().getServiceName());
		doc.put("ns", item.getKey().getNamespace());
		doc.put("version", item.getKey().getVersion());
		doc.put("instanceName", item.getKey().getInstanceName());
		doc.put("host", item.getKey().getHost());
		doc.put("port", item.getKey().getPort());
		//doc.put("sn", item.getKey().getServiceName());
		
		return doc;
		
	}

	private void doSave(Document doc) {
		MongoCollection<Document> coll = mongoDb.getCollection(SERVICE_EVENT);
		coll.insertOne(doc);
		
	}
	
}
