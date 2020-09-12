package cn.jmicro.pubsub;

import java.util.Set;

import org.bson.Document;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.profile.ProfileManager;
import cn.jmicro.api.pubsub.PubSubManager;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.common.Constants;

@Component
public class DataClearWorker {

	@Inject
	private IObjectStorage os ;
	
	@Inject
	private ProfileManager pm;
	
	public void ready() {
		TimerTicker.doInBaseTicker(30, "pubsub-DataClearWorker", null, (key,att)->{
			clearLogData();
			clearStatisData();
		});
	}

	private void clearStatisData() {
		Set<Integer> clientIds = this.os.distinct(PubsubMessageStatis.PUBSUB_BASE_DATA, "clientId",Integer.class);
		if(clientIds == null || clientIds.isEmpty()) {
			return;
		}
		
		long curTime = System.currentTimeMillis();
		
		// curTime-3day >= create_itme
		Document delFilter = new Document();
		for(Integer cid : clientIds) {
			delFilter.put(Constants.CLIENT_ID, cid);
			int days = pm.getVal(cid, PubSubManager.PROFILE_PUBSUB, "keepTimeLong", 1, Integer.class);
			long time = curTime - days*24*60*60*1000;
			delFilter.put(IObjectStorage.CREATED_TIME, new Document("$lte",time));
			os.deleteByQuery(PubsubMessageStatis.PUBSUB_BASE_DATA, delFilter);
			os.deleteByQuery(PubsubMessageStatis.PUBSUB_QPS_DATA, delFilter);
			//os.deleteByQuery(PubsubMessageStatis.PUBSUB_TOTAL_DATA, delFilter);
		}
		
	}

	private void clearLogData() {
		Set<Integer> clientIds = this.os.distinct(PubSubManager.TABLE_PUBSUB_ITEMS, "srcClientId",Integer.class);
		if(clientIds == null || clientIds.isEmpty()) {
			return;
		}
		
		long curTime = System.currentTimeMillis();
		
		// curTime-3day >= create_itme
		Document delFilter = new Document();
		for(Integer cid : clientIds) {
			delFilter.put("srcClientId", cid);
			int days = pm.getVal(cid, PubSubManager.PROFILE_PUBSUB, "keepTimeLong", 1, Integer.class);
			long time = curTime - days*24*60*60*1000;
			delFilter.put("createdTime", new Document("$lte",time));
			os.deleteByQuery(PubSubManager.TABLE_PUBSUB_ITEMS, delFilter);
		}
		
	}
	
}
