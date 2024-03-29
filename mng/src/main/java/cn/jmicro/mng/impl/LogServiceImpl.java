package cn.jmicro.mng.impl;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.gateway.ApiRequestJRso;
import cn.jmicro.api.gateway.ApiResponseJRso;
import cn.jmicro.api.mng.LogEntry;
import cn.jmicro.api.mng.LogItemJRso;
import cn.jmicro.api.monitor.JMFlatLogItemJRso;
import cn.jmicro.api.monitor.JMLogItemJRso;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.net.IReq;
import cn.jmicro.api.net.IResp;
import cn.jmicro.api.net.RpcRequestJRso;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.ext.mongodb.MongodbBaseObjectStorage;
import cn.jmicro.mng.Namespace;
import cn.jmicro.mng.api.ILogServiceJMSrv;

@Component
@Service(version="0.0.1",external=true,debugMode=1,showFront=false,logLevel=MC.LOG_NO,namespace=Namespace.NS)
public class LogServiceImpl implements ILogServiceJMSrv {

	private static final int FLAT_LOG = 1;
	
	private static final int GROUP_LOG = 2;
	
	@Inject
	private MongoDatabase mongoDb;
	
	private RpcRequesetDeserializedTypeAdapter rpcTpeAdatper = new RpcRequesetDeserializedTypeAdapter();
	private RpcResponseDeserializedTypeAdapter respTypeAdapter = new RpcResponseDeserializedTypeAdapter();
	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=10,maxPacketSize=256)
	public RespJRso<LogEntry> getByLinkId(Long linkId) {
		 RespJRso<LogEntry> resp = new RespJRso<>();
		 if(linkId == null || linkId <= 0) {
			resp.setCode(RespJRso.CODE_FAIL);
			resp.setMsg("Invalid linkId: " + linkId);
			return resp;
		 }
		 MongoCollection<Document> rpcLogColl = mongoDb.getCollection(JMLogItemJRso.TABLE);
		 Document match = new Document();
		 match.put("linkId", linkId);
		 
		 if(!PermissionManager.isCurAdmin(Config.getClientId())) {
			 match.put("clientId", JMicroContext.get().getAccount().getClientId());
		 }
		
		 Map<Long,LogEntry> logComsumerMap = new HashMap<>();
		 
		 Map<Long,List<JMLogItemJRso>> logProviderMap = new HashMap<>();
		 
		 LogEntry root = null;
		 
		 FindIterable<Document>  rst = rpcLogColl.find(match);
		 for(Document doc : rst) {
			 JMLogItemJRso mi = fromJson(doc.toJson(MongodbBaseObjectStorage.settings),JMLogItemJRso.class);
			 if(mi.isProvider()) {
				if(!logProviderMap.containsKey(mi.getReqId())) {
					logProviderMap.put(mi.getReqId(), new ArrayList<>());
				}
				logProviderMap.get(mi.getReqId()).add(mi);
			 } else {
				 LogEntry le = new LogEntry(mi);
				 le.setId(mi.getReqId()+"");
				 logComsumerMap.put(mi.getReqId(), le);
				 if(mi.isNl()) {
					 root = le;
				 }
			 }
		 }
		 
		 for(LogEntry le : logComsumerMap.values()) {
			 setChild(logComsumerMap,le);
			 if(logProviderMap.containsKey(le.getItem().getReqId())) {
				le.setProviderItems(logProviderMap.get(le.getItem().getReqId()));
			 }
		 }
		 
		 if(root != null) {
			 resp.setData(root);
			 resp.setCode(RespJRso.CODE_SUCCESS);
		 }else {
			 resp.setCode(RespJRso.CODE_FAIL);
			 resp.setMsg("The origin request item not found!");
		 }
		 
		 return resp;
	}

	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=10,maxPacketSize=4096)
	public RespJRso<List<LogEntry>> query(Map<String, String> queryConditions, int pageSize, int curPage) {

		Document qryMatch = this.getCondtions(queryConditions);
		
		qryMatch.put("reqId", new Document("$gt",0));
		qryMatch.put("nl", true);//链路起点
		
	/*	Document sub_group = new Document();
		sub_group.put("_id", "$leaveMethod");
		sub_group.put("count", new Document("$sum", 1));*/
		
		Document match = new Document("$match", qryMatch);
		//Document group = new Document("$group", sub_group);
		
		Document sortFields = new Document("costTime",-1);
		sortFields.put("linkId", -1);
		
		Document sort = new Document("$sort", sortFields);
		
		Document skip = new Document("$skip", pageSize*curPage);
		Document limit = new Document("$limit", pageSize);
		
		List<Document> aggregateList = new ArrayList<Document>();
		aggregateList.add(match);
		aggregateList.add(sort);
		aggregateList.add(skip);
		aggregateList.add(limit);
		
		MongoCollection<Document> rpcLogColl = mongoDb.getCollection(JMLogItemJRso.TABLE);
		AggregateIterable<Document> resultset = rpcLogColl.aggregate(aggregateList);
		MongoCursor<Document> cursor = resultset.iterator();
		
		RespJRso<List<LogEntry>> resp = new RespJRso<>();
		List<LogEntry> rl = new ArrayList<>();
		resp.setData(rl);
		
		try {
			while(cursor.hasNext()) {
				LogEntry le = new LogEntry();
				rl.add(le);
				Document log = cursor.next();
				JMLogItemJRso mi = fromJson(log.toJson(MongodbBaseObjectStorage.settings),JMLogItemJRso.class);
				le.setItem(mi);
				le.setId(mi.getReqId()+"");
				if(mi.getLinkId() > 0) {
					parseChild(rpcLogColl,le);
				}
			}
			resp.setCode(RespJRso.CODE_SUCCESS);
		} finally {
			cursor.close();
		}
		
		return resp;
	}
	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=10,maxPacketSize=2048,logLevel=MC.LOG_NO)
	public RespJRso<Long> count(Map<String, String> queryConditions) {
		Document match = this.getCondtions(queryConditions);
		
		match.put("reqId", new Document("$gt",0));//排除非RPC日志
		match.put("nl", true);//链路起点
		
		MongoCollection<Document> rpcLogColl = mongoDb.getCollection(JMLogItemJRso.TABLE);
		RespJRso<Long> resp = new RespJRso<>();
		Long cnt = rpcLogColl.countDocuments(match);
		resp.setData(cnt);
		resp.setCode(RespJRso.CODE_SUCCESS);
		
		return resp;
	}
	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=10,maxPacketSize=256)
	public RespJRso<Map<String,Object>> queryDict() {
		
		Map<String,Object> dists = new HashMap<>();
		dists.put("level", MC.LogKey2Val);
		dists.put("type", MC.MT_Key2Val);
		
		MongoCollection<Document> rpcLogColl = mongoDb.getCollection(JMLogItemJRso.TABLE);
		
		DistinctIterable<String> hostIte = rpcLogColl.distinct("remoteHost", String.class);
		Set<String> host = new HashSet<>();
		for(String h : hostIte) {
			host.add(h);
		}
		String[] hostArr = new String[host.size()];
		host.toArray(hostArr);
		dists.put("remoteHost", hostArr);
		
		DistinctIterable<String> localHostIte = rpcLogColl.distinct("localHost", String.class);
		Set<String> localHost = new HashSet<>();
		for(String h : localHostIte) {
			localHost.add(h);
		}
		String[] localHostArr = new String[host.size()];
		host.toArray(localHostArr);
		dists.put("localHost", localHostArr);
		
		DistinctIterable<String> instanceNameIte = rpcLogColl.distinct("instanceName", String.class);
		Set<String> instanceNames = new HashSet<>();
		for(String h : instanceNameIte) {
			instanceNames.add(h);
		}
		String[] instanceNamesArr = new String[instanceNames.size()];
		instanceNames.toArray(instanceNamesArr);
		dists.put("instanceName", instanceNamesArr);
		
		DistinctIterable<String> serviceNameIte = rpcLogColl.distinct("req.serviceName", String.class);
		Set<String> serviceNames = new HashSet<>();
		for(String h : serviceNameIte) {
			serviceNames.add(h);
		}
		String[] serviceNamesArr = new String[serviceNames.size()];
		serviceNames.toArray(serviceNamesArr);
		dists.put("serviceName", serviceNamesArr);
		
		DistinctIterable<String> namespaceIte = rpcLogColl.distinct("req.namespace", String.class);
		Set<String> namespaceItes = new HashSet<>();
		for(String h : namespaceIte) {
			namespaceItes.add(h);
		}
		String[] namespaceItesArr = new String[namespaceItes.size()];
		namespaceItes.toArray(namespaceItesArr);
		dists.put("namespace", namespaceItesArr);
		
		DistinctIterable<String> versionIte = rpcLogColl.distinct("req.version", String.class);
		Set<String> versions = new HashSet<>();
		for(String h : versionIte) {
			versions.add(h);
		}
		String[] versionArr = new String[versions.size()];
		versions.toArray(versionArr);
		dists.put("version", versionArr);
		
		DistinctIterable<String> methodIte = rpcLogColl.distinct("req.method", String.class);
		Set<String> methods = new HashSet<>();
		for(String h : methodIte) {
			methods.add(h);
		}
		String[] methodArr = new String[methods.size()];
		methods.toArray(methodArr);
		dists.put("method", methodArr);
		
		DistinctIterable<String> actIte = rpcLogColl.distinct("act", String.class);
		Set<String> acts = new HashSet<>();
		for(String h : actIte) {
			if(h != null && !"null".equals(h.toLowerCase())) {
				acts.add(h);
			}
		}
		String[] actsArr = new String[acts.size()];
		acts.toArray(actsArr);
		dists.put("act", actsArr);
		
		RespJRso<Map<String,Object>> resp = new RespJRso<>();
		resp.setData(dists);
		resp.setCode(RespJRso.CODE_SUCCESS);
		
		return resp;
	}
	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=10,maxPacketSize=2048)
	public RespJRso<Integer> countLog(int showType,Map<String, String> queryConditions) {

		if(showType == FLAT_LOG) {
			return countFlatLog(queryConditions);
		}
		
		RespJRso<Integer> resp = new RespJRso<>();

		Document qryMatch = this.getLogCondtions(queryConditions);
		
		Document groupDoc = new Document();
		
		groupDoc.put("_id", null);
		groupDoc.put("total", new Document("$sum",1));
		
		Document match = new Document("$match", qryMatch);
		//Document unwind = new Document("$unwind", "$items");
		Document group = new Document("$group", groupDoc);

		List<Document> aggregateList = new ArrayList<Document>();
		aggregateList.add(match);
		//aggregateList.add(unwind);
		
		String val = queryConditions.get("noLog");
		if(StringUtils.isNotEmpty(val) && "true".equals(val)) {
			aggregateList.add(new Document("$match",new Document("items.level",new Document("$ne",MC.LOG_NO))));
		}
		
		aggregateList.add(group);

		MongoCollection<Document> rpcLogColl = mongoDb.getCollection(JMLogItemJRso.TABLE);
		AggregateIterable<Document> resultset = rpcLogColl.aggregate(aggregateList);

		Document log = resultset.first();
		if(log != null) {
			resp.setData(log.getInteger("total"));
			resp.setCode(RespJRso.CODE_SUCCESS);
		}else {
			resp.setCode(RespJRso.CODE_FAIL);
			resp.setMsg("No data to found!");
		}
	
		return resp;
	}
	
	private RespJRso<Integer> countFlatLog(Map<String, String> queryConditions) {

		RespJRso<Integer> resp = new RespJRso<>();

		Document qryMatch = this.getLogCondtions(queryConditions);
		//qryMatch.put("items.desc", new Document("$ne",Constants.INVALID_LOG_DESC));
		//qryMatch.remove("createTime");
		
		Document groupDoc = new Document();
		
		groupDoc.put("_id", null);
		groupDoc.put("total", new Document("$sum",1));
		
		Document qmatch =  getFlatMatchCondition(queryConditions);
		
		List<Document> aggregateList = new ArrayList<Document>();
		aggregateList.add(new Document("$match", qryMatch));
		aggregateList.add(new Document("$unwind", "$items"));
		
		aggregateList.add(new Document("$match",qmatch));
		
		aggregateList.add(new Document("$group", groupDoc));

		MongoCollection<Document> rpcLogColl = mongoDb.getCollection(JMLogItemJRso.TABLE);
		AggregateIterable<Document> resultset = rpcLogColl.aggregate(aggregateList);

		Document log = resultset.first();
		if(log != null) {
			resp.setData(log.getInteger("total"));
			resp.setCode(RespJRso.CODE_SUCCESS);
		}else {
			resp.setCode(RespJRso.CODE_FAIL);
			resp.setMsg("No data to found!");
		}
	
		return resp;
	}
	

	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=10,maxPacketSize=2048)
	public RespJRso<List<JMLogItemJRso>> queryLog(Map<String, String> queryConditions, int pageSize, int curPage) {

		Document qryMatch = this.getLogCondtions(queryConditions);
		
		Document match = new Document("$match", qryMatch);
		//Document unwind = new Document("$unwind", "$items");
		
		Document sort = new Document("$sort", new Document("createTime", -1));
		Document skip = new Document("$skip", pageSize*curPage);
		Document limit = new Document("$limit", pageSize);
		
		List<Document> aggregateList = new ArrayList<Document>();
		aggregateList.add(match);
		//aggregateList.add(unwind);
		
		String val = queryConditions.get("noLog");
		if(StringUtils.isNotEmpty(val) && "true".equals(val)) {
			aggregateList.add(new Document("$match",new Document("items.level",new Document("$ne",MC.LOG_NO))));
		}
		
		aggregateList.add(sort);
		aggregateList.add(skip);
		aggregateList.add(limit);
		
		MongoCollection<Document> rpcLogColl = mongoDb.getCollection(JMLogItemJRso.TABLE);
		AggregateIterable<Document> resultset = rpcLogColl.aggregate(aggregateList);
		MongoCursor<Document> cursor = resultset.iterator();
		
		RespJRso<List<JMLogItemJRso>> resp = new RespJRso<>();
		List<JMLogItemJRso> rl = new ArrayList<>();
		resp.setData(rl);
		
		try {
			while(cursor.hasNext()) {
				Document log = cursor.next();
				/*Document liDoc = log.get("items", Document.class);
				LogItem li = JsonUtils.getIns().fromJson(liDoc.toJson(settings), LogItem.class);
				log.remove("items");*/
				JMLogItemJRso mi = fromJson(log.toJson(MongodbBaseObjectStorage.settings),JMLogItemJRso.class);
				//mi.setItems(null);
				//li.setItem(mi);
				if(mi.getItems() != null && !mi.getItems().isEmpty()) {
					mi.getItems().sort((i1,i2)->{
						return i1.getTime() > i2.getTime() ? -1 : (i1.getTime() == i2.getTime() ? 0 : 1);
					});
				}
				rl.add(mi);
			}
			resp.setCode(RespJRso.CODE_SUCCESS);
		} finally {
			cursor.close();
		}
		
		return resp;
	
	}
	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=10,maxPacketSize=2048)
	public RespJRso<List<JMFlatLogItemJRso>> queryFlatLog(Map<String, String> queryConditions, int pageSize, int curPage) {

		Document qryMatch = this.getLogCondtions(queryConditions);
		//qryMatch.remove("createTime");
		
		//qryMatch.put("items.desc", new Document("$ne",Constants.INVALID_LOG_DESC));
		
		
		Document qmatch = getFlatMatchCondition(queryConditions);
		
		List<Document> aggregateList = new ArrayList<Document>();
		aggregateList.add(new Document("$match", qryMatch));
		aggregateList.add(new Document("$unwind", "$items"));
		aggregateList.add(new Document("$match",qmatch));
		
		aggregateList.add(new Document("$sort", new Document("items.time", -1)));
		aggregateList.add(new Document("$skip", pageSize*curPage));
		aggregateList.add(new Document("$limit", pageSize));
		
		MongoCollection<Document> rpcLogColl = mongoDb.getCollection(JMLogItemJRso.TABLE);
		AggregateIterable<Document> resultset = rpcLogColl.aggregate(aggregateList);
		MongoCursor<Document> cursor = resultset.iterator();
		
		RespJRso<List<JMFlatLogItemJRso>> resp = new RespJRso<>();
		List<JMFlatLogItemJRso> rl = new ArrayList<>();
		resp.setData(rl);
		
		try {
			while(cursor.hasNext()) {
				Document log = cursor.next();
				/*Document liDoc = log.get("items", Document.class);
				LogItem li = JsonUtils.getIns().fromJson(liDoc.toJson(settings), LogItem.class);
				log.remove("items");*/
				JMFlatLogItemJRso mi = fromJson(log.toJson(MongodbBaseObjectStorage.settings),JMFlatLogItemJRso.class);
				rl.add(mi);
			}
			resp.setCode(RespJRso.CODE_SUCCESS);
		} finally {
			cursor.close();
		}
		
		return resp;
	
	}
	
	private Document getFlatMatchCondition(Map<String, String> queryConditions) {
		Document qmatch = new Document();
		String v = queryConditions.get("tag");
		if(StringUtils.isNotEmpty(v)) {
			qmatch.put("items.tag", new Document("$regex",v));
		}
		
		String key = "desc";
		String val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			qmatch.put("items.desc", new Document("$regex",val));
		}
		
		key = "startTime";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			Document st = new Document();
			st.put("$gte", Long.parseLong(val));
			qmatch.put("items.time", st);
		}
		
		key = "endTime";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			Document st = (Document)qmatch.get("createTime");
			if(st == null) {
				st = new Document();
				qmatch.put("items.time", st);
			}
			st.put("$lte", Long.parseLong(val));
		}
		
		this.matchLogLevel(qmatch, queryConditions);
		
		return qmatch;
	}

	//@Override
	//@SMethod(perType=false,needLogin=true,maxSpeed=10,maxPacketSize=2048)
	public RespJRso<List<LogItemJRso>> queryLog1(Map<String, String> queryConditions, int pageSize, int curPage) {

		Document qryMatch = this.getLogCondtions(queryConditions);
		
		Document match = new Document("$match", qryMatch);
		Document unwind = new Document("$unwind", "$items");
		
		Document sort = new Document("$sort", new Document("items.time", -1));
		Document skip = new Document("$skip", pageSize*curPage);
		Document limit = new Document("$limit", pageSize);
		
		List<Document> aggregateList = new ArrayList<Document>();
		aggregateList.add(match);
		aggregateList.add(unwind);
		
		String val = queryConditions.get("noLog");
		if(StringUtils.isNotEmpty(val) && "true".equals(val)) {
			aggregateList.add(new Document("$match",new Document("items.level",new Document("$ne",MC.LOG_NO))));
		}
		
		aggregateList.add(sort);
		aggregateList.add(skip);
		aggregateList.add(limit);
		
		MongoCollection<Document> rpcLogColl = mongoDb.getCollection(JMLogItemJRso.TABLE);
		AggregateIterable<Document> resultset = rpcLogColl.aggregate(aggregateList);
		MongoCursor<Document> cursor = resultset.iterator();
		
		RespJRso<List<LogItemJRso>> resp = new RespJRso<>();
		List<LogItemJRso> rl = new ArrayList<>();
		resp.setData(rl);
		
		try {
			while(cursor.hasNext()) {
				Document log = cursor.next();
				Document liDoc = log.get("items", Document.class);
				LogItemJRso li = JsonUtils.getIns().fromJson(liDoc.toJson(MongodbBaseObjectStorage.settings), LogItemJRso.class);
				log.remove("items");
				JMLogItemJRso mi = fromJson(log.toJson(MongodbBaseObjectStorage.settings),JMLogItemJRso.class);
				mi.setItems(null);
				li.setItem(mi);
				rl.add(li);
			}
			resp.setCode(RespJRso.CODE_SUCCESS);
		} finally {
			cursor.close();
		}
		
		return resp;
	
	}
	
	private Document getLogCondtions(Map<String, String> queryConditions) {
		Document match = this.getCondtions(queryConditions);
		
		if(!PermissionManager.isCurAdmin(Config.getClientId())) {
			 match.put("clientId", JMicroContext.get().getAccount().getClientId());
		 }
		
		//match.put("$where", "this.items.length>0");
		
		String key = "reqParentId";
		String val = queryConditions.get(key);
		if(StringUtils.isEmpty(val)) {
			match.remove(key);
		}
		
		key = "provider";
		val = queryConditions.get(key);
		if(StringUtils.isEmpty(val)) {
			match.remove(key);
		}
		
		//db.getCollection(JMLogItem.TABLE).find({"items":{"$elemMatch":{"tag":{$regex:"cn"}}}} )
		key = "tag";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			//Document tagRegex = new Document("items.tag", new Document("$regex",val));
			match.put("items.tag", new Document("$regex",val));
		}
		
		key = "desc";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			match.put("items.desc", new Document("$regex",val));
		}
		
		return match;
	}

	private Document getCondtions(Map<String, String> queryConditions) {
		Document match = new Document();
		
		 if(!PermissionManager.isCurAdmin(Config.getClientId())) {
			 match.put("clientId", JMicroContext.get().getAccount().getClientId());
		 }
		 
		String key = "reqParentId";
		String val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			match.put(key, Long.parseLong(val));
		} /*else {
			match.put(key, -1);
		}*/
		
		key = "provider";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			match.put(key, Boolean.parseBoolean(val));
		}else {
			match.put(key, false);
		}
		
		key = "linkId";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			match.put(key, Long.parseLong(val));
		}
		
		key = "reqId";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			match.put(key, Long.parseLong(val));
		}
		
		key = "startTime";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			Document st = new Document();
			st.put("$gte", Long.parseLong(val));
			match.put("createTime", st);
		}
		
		key = "endTime";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			Document st = (Document)match.get("createTime");
			if(st == null) {
				st = new Document();
				match.put("createTime", st);
			}
			st.put("$lte", Long.parseLong(val));
		}
		
		key = "act";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			match.put(key, val);
		}
		
		key = "remoteHost";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			match.put(key, val);
		}
		
		key = "remotePort";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			match.put(key, val);
		}
		
		key = "localHost";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			match.put(key, val);
		}
		
		key = "instanceName";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			match.put(key, val);
		}
		
		key = "serviceName";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			match.put("req.serviceName", val);
		}
		
		key = "namespace";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			match.put("req.namespace", val);
		}
		
		key = "version";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			match.put("req.version", val);
		}
		

		key = "method";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			match.put("req.method", val);
		}
		
		key = "impCls";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			match.put("impCls", val);
		}
		
		this.matchLogLevel(match, queryConditions);
		
		/*key = "type";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			match.put("items.type", Short.parseShort(val));
		}*/
		
		key = "success";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			match.put("resp.success", Boolean.parseBoolean(val));
		}
		
		key = "configId";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			match.put(key, val);
		}
		
		key = "configTag";
		val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			match.put("tag", val);
		}
		
		return match;
	}
	
	

	private void matchLogLevel(Document qmatch, Map<String,String> queryConditions) {
		String v = queryConditions.get("level");
		if(StringUtils.isNotEmpty(v)) {
			String op = queryConditions.get("op");
			if(Utils.isEmpty(op)) {
				op = ">=";
			}
			if("=".equals(op)) {
				qmatch.put("items.level", Integer.parseInt(v));
			}else if(">=".equals(op)) {
				qmatch.put("items.level", new Document("$gte",Integer.parseInt(v)));
			}else if(">".equals(op)) {
				qmatch.put("items.level", new Document("$gt",Integer.parseInt(v)));
			}
		}
		
	}

	private void parseChild(MongoCollection<Document> rpcLogColl,LogEntry root) {
		 Document match = new Document();
		 match.put("linkId", root.getItem().getLinkId());
		 match.put("provider", false);
		
		 Map<Long,LogEntry> logMap = new HashMap<>();
		 
		 FindIterable<Document>  rst = rpcLogColl.find(match);
		 for(Document doc : rst) {
			 JMLogItemJRso mi = fromJson(doc.toJson(MongodbBaseObjectStorage.settings),JMLogItemJRso.class);
			 LogEntry le = new LogEntry(mi);
			 le.setId(mi.getReqId()+"");
			 logMap.put(mi.getReqId(), le);
		 }
		 
		 setChild(logMap,root);
		 
		for(LogEntry le : logMap.values()) {
			 setChild(logMap,le);
		}
		 
	}

	private void setChild(Map<Long, LogEntry> logMap, LogEntry root) {
		long pid = root.getItem().getReqId();
		if(pid <= 0) {
			return;
		}
		for(LogEntry le : logMap.values()) {
			 if(le.getItem().getReqParentId() == pid) {
				 root.getChildren().add(le);
			 }
		}
	}
	
	private <T> T fromJson(String json,Class<T> cls) {
		GsonBuilder builder = new GsonBuilder();
		//builder.registerTypeAdapter(IReq.class,rpcTpeAdatper);
		//builder.registerTypeAdapter(IResp.class, respTypeAdapter);
		builder.registerTypeAdapter(IReq.class, this.rpcTpeAdatper);
		builder.registerTypeAdapter(IResp.class, this.respTypeAdapter);
		return builder.create().fromJson(json, cls);
	}

	private class RpcRequesetDeserializedTypeAdapter implements JsonDeserializer<IReq> {

		@Override
		public IReq deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			if(typeOfT == IReq.class) {
				JsonObject jo = (JsonObject)json;
				/*JsonElement argsStr = jo.get("args");
				
				Object[] args = null;
				if(argsStr != null) {
					args = JsonUtils.getIns().getObjectArray(argsStr.getAsString(),false);
				}
				jo.remove("args");*/
				if(jo.has("impl") && jo.has("transport")) {
					RpcRequestJRso r = context.deserialize(json, RpcRequestJRso.class);
					//r.setArgs(args);
					return r;
				}else {
					ApiRequestJRso ar = context.deserialize(json, ApiRequestJRso.class);
					//ar.setArgs(args);
					return ar;
				}
			}
			return null;
		}
	}
	
	private class RpcResponseDeserializedTypeAdapter implements JsonDeserializer<IResp> {

		@Override
		public IResp deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			if(typeOfT == IResp.class) {
				JsonObject jo = (JsonObject)json;
				if(jo.has("id") && jo.has("reqId")) {
					ApiResponseJRso ar = context.deserialize(json, ApiResponseJRso.class);
					//ar.setArgs(args);
					return ar;
				} else {
					RespJRso r = context.deserialize(json, RespJRso.class);
					//r.setArgs(args);
					return r;
				}
			}
			return null;
		}
	}
}
