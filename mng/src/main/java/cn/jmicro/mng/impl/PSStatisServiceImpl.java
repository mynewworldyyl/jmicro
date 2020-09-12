package cn.jmicro.mng.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.json.JsonWriterSettings;

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
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.common.Constants;
import cn.jmicro.mng.api.IPSStatisService;

@Component(level=20001)
@Service(namespace="mng", version="0.0.1", external=true, debugMode=0, showFront=false)
public class PSStatisServiceImpl implements IPSStatisService {

	public static final String PUTSUB_TOTAL = "t_pubsub_total";
	
	private JsonWriterSettings settings = JsonWriterSettings.builder()
	         .int64Converter((value, writer) -> writer.writeNumber(value.toString()))
	         .build();
	
	@Inject
	private MongoDatabase mongoDb;
	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=10,maxPacketSize=2048)
	public Resp<Long> count(Map<String, String> queryConditions) {
		Document match = this.getCondtions(queryConditions);
		MongoCollection<Document> rpcLogColl = mongoDb.getCollection(PUTSUB_TOTAL);
		Resp<Long> resp = new Resp<>();
		Long cnt = rpcLogColl.countDocuments(match);
		resp.setData(cnt);
		resp.setCode(Resp.CODE_SUCCESS);
		
		return resp;
	}

	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=10,maxPacketSize=2048)
	public Resp<List<Map<String,Object>>> query(Map<String, String> queryConditions, int pageSize, int curPage) {

		Document qryMatch = this.getCondtions(queryConditions);

		Document match = new Document("$match", qryMatch);
		// Document group = new Document("$group", sub_group);

		Document sortFields = new Document("createdTime", -1);

		Document sort = new Document("$sort", sortFields);

		Document skip = new Document("$skip", pageSize * curPage);
		Document limit = new Document("$limit", pageSize);

		List<Document> aggregateList = new ArrayList<Document>();
		aggregateList.add(match);
		aggregateList.add(sort);
		aggregateList.add(skip);
		aggregateList.add(limit);

		MongoCollection<Document> rpcLogColl = mongoDb.getCollection(PUTSUB_TOTAL);
		AggregateIterable<Document> resultset = rpcLogColl.aggregate(aggregateList);
		MongoCursor<Document> cursor = resultset.iterator();

		Resp<List<Map<String,Object>>> resp = new Resp<>();
		List<Map<String,Object>> rl = new ArrayList<>();
		resp.setData(rl);

		try {
			while (cursor.hasNext()) {
				Document log = cursor.next();
				Map<String,Object> vo = new HashMap<String,Object>();
				for(Map.Entry<String, Object> es : log.entrySet()) {
					if(es.getKey().equals("_id")) {
						continue;
					}
					vo.put(es.getKey(), es.getValue());
				}
				rl.add(vo);
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
			 match.put(Constants.CLIENT_ID, JMicroContext.get().getAccount().getClientId());
		 }
		
		/*String key = "startTime";
		String val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			Document st = new Document();
			st.put("$gte", Long.parseLong(val));
			match.put("createTime", st);
		}*/
		
		return match;
	}
	
}
