package cn.jmicro.mng.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.json.JsonWriterSettings;

import com.google.gson.GsonBuilder;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.pubsub.PubSubManager;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.mng.api.IPSDataService;
import cn.jmicro.mng.api.PSDataVo;

@Component(level=20001)
@Service(version="0.0.1", external=true, debugMode=0, showFront=false,logLevel=MC.LOG_NO)
public class PSDataServiceImpl implements IPSDataService {

	private JsonWriterSettings settings = JsonWriterSettings.builder()
	         .int64Converter((value, writer) -> writer.writeNumber(value.toString()))
	         .build();
	
	@Inject
	private MongoDatabase mongoDb;
	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=10,maxPacketSize=2048)
	public Resp<Long> count(Map<String, String> queryConditions) {
		
		Document match = this.getCondtions(queryConditions);
		MongoCollection<Document> rpcLogColl = mongoDb.getCollection(PubSubManager.TABLE_PUBSUB_ITEMS);
		Resp<Long> resp = new Resp<>();
		Long cnt = rpcLogColl.countDocuments(match);
		resp.setData(cnt);
		resp.setCode(Resp.CODE_SUCCESS);
		
		return resp;
	}

	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=10,maxPacketSize=2048)
	public Resp<List<PSDataVo>> query(Map<String, String> queryConditions, int pageSize, int curPage) {

		Document qryMatch = this.getCondtions(queryConditions);

		Document match = new Document("$match", qryMatch);
		// Document group = new Document("$group", sub_group);

		Document sortFields = new Document(IObjectStorage.CREATED_TIME, -1);

		Document sort = new Document("$sort", sortFields);

		Document skip = new Document("$skip", pageSize * curPage);
		Document limit = new Document("$limit", pageSize);

		List<Document> aggregateList = new ArrayList<Document>();
		aggregateList.add(match);
		aggregateList.add(sort);
		aggregateList.add(skip);
		aggregateList.add(limit);

		MongoCollection<Document> rpcLogColl = mongoDb.getCollection(PubSubManager.TABLE_PUBSUB_ITEMS);
		AggregateIterable<Document> resultset = rpcLogColl.aggregate(aggregateList);
		MongoCursor<Document> cursor = resultset.iterator();

		Resp<List<PSDataVo>> resp = new Resp<>();
		List<PSDataVo> rl = new ArrayList<>();
		resp.setData(rl);

		try {
			while (cursor.hasNext()) {
				Document log = cursor.next();
				PSDataVo vo = fromJson(log.toJson(settings));
				if (vo != null) {
					rl.add(vo);
				}
			}
			resp.setCode(Resp.CODE_SUCCESS);
		} finally {
			cursor.close();
		}

		return resp;
	}

	private Document getCondtions(Map<String, String> queryConditions) {
		Document match = new Document();
		
		 if(!PermissionManager.isCurAdmin()) {
			 match.put("srcClientId", JMicroContext.get().getAccount().getId());
		 }
		
		String key = "startTime";
		String val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			Document st = new Document();
			st.put("$gte", Long.parseLong(val));
			match.put(IObjectStorage.CREATED_TIME, st);
		}
		
		return match;
	}
	
	private PSDataVo fromJson(String json) {
		GsonBuilder builder = new GsonBuilder();
		//builder.registerTypeAdapter(IReq.class,rpcTpeAdatper);
		PSData psd = builder.create().fromJson(json, PSData.class);
		PSDataVo vo = builder.create().fromJson(json, PSDataVo.class);
		vo.setPsData(psd);
		return vo;
	}
}
