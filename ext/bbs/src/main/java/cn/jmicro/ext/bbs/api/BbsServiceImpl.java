package cn.jmicro.ext.bbs.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.json.JsonWriterSettings;

import com.google.gson.GsonBuilder;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.security.AccountManager;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.ext.bbs.entities.Note;
import cn.jmicro.ext.bbs.entities.NoteVo;
import cn.jmicro.ext.bbs.entities.Topic;
import cn.jmicro.ext.bbs.entities.TopicVo;

@Component(level=20001)
@Service(namespace="bbs", version="0.0.1", external=true, debugMode=0, showFront=false)
public class BbsServiceImpl implements IBbsService {

	@Inject
	private IObjectStorage os;
	
	private JsonWriterSettings settings = JsonWriterSettings.builder()
	         .int64Converter((value, writer) -> writer.writeNumber(value.toString()))
	         .build();
	
	@Inject
	private MongoDatabase mongoDb;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private AccountManager am;
	
	
	public void ready() {
	}
	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=10,maxPacketSize=2048)
	public Resp<Boolean> createTopic(Topic topic) {
		Resp<Boolean> r = new Resp<>(Resp.CODE_FAIL);
		if(Utils.isEmpty(topic.getContent())) {
			r.setMsg("Content cannot be null");
			r.setKey("ContentIsNull");
			r.setData(false);
			return r;
		}
		
		if(Utils.isEmpty(topic.getTitle())) {
			r.setMsg("Title cannot be null");
			r.setKey("TitleIsNull");
			r.setData(false);
			return r;
		}
		
		if(Utils.isEmpty(topic.getTopicType())) {
			r.setMsg("Title cannot be null");
			r.setKey("TitleIsNull");
			r.setData(false);
			return r;
		}
		
		long curTime = System.currentTimeMillis();
		ActInfo ai = JMicroContext.get().getAccount();
		
		topic.setClientId(ai.getClientId());
		topic.setCreatedBy(ai.getClientId());
		topic.setUpdatedBy(ai.getClientId());
		topic.setUpdatedTime(curTime);
		topic.setId(idGenerator.getLongId(Topic.class));
		topic.setLocked(false);
		topic.setCreatedTime(curTime);
		
		//os.save(T_TOPIC_NAME, topic, true);
		this.mongoDb.getCollection(T_TOPIC_NAME, Topic.class).insertOne(topic);
		
		r.setData(true);
		r.setCode(Resp.CODE_SUCCESS);
		return r;
	}

	@Override
	public Resp<List<Topic>> topicList(Map<String, String> qry, int pageSize, int curPage) {
		
		Document qryMatch = this.getCondtions(qry);

		Document match = new Document("$match", qryMatch);
		// Document group = new Document("$group", sub_group);

		Document sortFields = new Document(IObjectStorage.CREATED_TIME, -1);

		Document sort = new Document("$sort", sortFields);

		Document skip = new Document("$skip", pageSize * (curPage-1));
		Document limit = new Document("$limit", pageSize);

		List<Document> aggregateList = new ArrayList<Document>();
		aggregateList.add(match);
		aggregateList.add(sort);
		aggregateList.add(skip);
		aggregateList.add(limit);

		//MongoCollection<Document> rpcLogColl = mongoDb.getCollection(T_TOPIC_NAME);
		MongoCollection<Topic> rpcLogColl = this.mongoDb.getCollection(T_TOPIC_NAME, Topic.class);
		AggregateIterable<Topic> resultset = rpcLogColl.aggregate(aggregateList,Topic.class);
		MongoCursor<Topic> cursor = resultset.iterator();

		Resp<List<Topic>> resp = new Resp<>();
		List<Topic> rl = new ArrayList<>();
		resp.setData(rl);

		try {
			while (cursor.hasNext()) {
				Topic vo = cursor.next();
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

	@Override
	public Resp<Long> countTopic(Map<String, String> qry) {
		Document match = this.getCondtions(qry);
		MongoCollection<Document> rpcLogColl = mongoDb.getCollection(T_TOPIC_NAME);
		Resp<Long> resp = new Resp<>();
		Long cnt = rpcLogColl.countDocuments(match);
		resp.setData(cnt);
		resp.setCode(Resp.CODE_SUCCESS);
		return resp;
	}

	@Override
	public Resp<TopicVo> getTopic(long topicId) {
		Resp<TopicVo> resp = new Resp<>(Resp.CODE_FAIL);
		Document qryMatch = new Document("_id",topicId);
		FindIterable<Topic> fi = mongoDb.getCollection(T_TOPIC_NAME,Topic.class)
				.find(qryMatch, Topic.class);
		if(fi != null) {
			TopicVo vo = new TopicVo();
			Topic t = fi.first();
			//ActInfo ai = this.am.getAccountFromZK(t.getId());
			//vo.setCreaterId(createrId);
			Resp<List<NoteVo>> notes = this.topicNoteList(topicId, Integer.MAX_VALUE, 0);
			vo.setNotes(notes.getData());
			
			resp.setData(vo);
			resp.setCode(Resp.CODE_SUCCESS);
		}else {
			resp.setData(null);
			resp.setMsg("Not found");
			resp.setKey("NotFound");
		}
		return resp;
	}

	@Override
	public Resp<List<NoteVo>> topicNoteList(long topicId, int pageSize, int curPage) {
		
		Document qryMatch = new Document("topicId",topicId);

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

		MongoCollection<Note> rpcLogColl = mongoDb.getCollection(T_TOPIC_NAME,Note.class);
		AggregateIterable<Note> resultset = rpcLogColl.aggregate(aggregateList,Note.class);
		MongoCursor<Note> cursor = resultset.iterator();

		Resp<List<NoteVo>> resp = new Resp<>();
		List<NoteVo> rl = new ArrayList<>();
		resp.setData(rl);

		try {
			while (cursor.hasNext()) {
				Note vo = cursor.next();
				if (vo != null) {
					NoteVo nvo = new NoteVo();
					nvo.setNote(vo);
					rl.add(nvo);
				}
			}
			resp.setCode(Resp.CODE_SUCCESS);
		} finally {
			cursor.close();
		}

		return resp;
	
	}

	@Override
	public Resp<Boolean> createNote(Note note) {
		Resp<Boolean> r = new Resp<>(Resp.CODE_FAIL);
		if(Utils.isEmpty(note.getContent())) {
			r.setMsg("Content cannot be null");
			r.setKey("ContentIsNull");
			r.setData(false);
			return r;
		}
		
		if(note.getTopicId() <= 0) {
			r.setMsg("Invalid topic");
			r.setKey("InvalidTopicId");
			r.setData(false);
			return r;
		}
		
		long curTime = System.currentTimeMillis();
		ActInfo ai = JMicroContext.get().getAccount();
		
		note.setClientId(ai.getClientId());
		note.setCreatedBy(ai.getClientId());
		note.setUpdatedBy(ai.getClientId());
		note.setUpdatedTime(curTime);
		note.setId(idGenerator.getLongId(Note.class));
		note.setCreatedTime(curTime);
		
		os.save(T_NOTE_NAME, note,Note.class, true);
		r.setData(true);
		r.setCode(Resp.CODE_SUCCESS);
		return r;
	}
	
	private Document getCondtions(Map<String, String> queryConditions) {
		 Document match = new Document();
		
		String key = "startTime";
		String val = queryConditions.get(key);
		if(StringUtils.isNotEmpty(val)) {
			Document st = new Document();
			st.put("$gte", Long.parseLong(val));
			match.put("createdTime", st);
		}
		
		return match;
	}
	
	private Topic fromJson(String json) {
		GsonBuilder builder = new GsonBuilder();
		Topic psd = builder.create().fromJson(json, Topic.class);
		return psd;
	}
}
