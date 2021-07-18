package cn.jmicro.ext.bbs.api;

import java.util.ArrayList;
import java.util.HashMap;
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
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.security.AccountManager;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.ext.bbs.entities.NoteJRso;
import cn.jmicro.ext.bbs.entities.TopicJRso;
import cn.jmicro.ext.bbs.entities.TopicVoJRso;

@Component(level=20001)
@Service(version="0.0.1", external=true, debugMode=0, showFront=false)
public class BbsServiceImpl implements IBbsServiceJMSrv {

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
	@SMethod(perType=false,needLogin=true,maxSpeed=1,maxPacketSize=204800)
	public RespJRso<Boolean> updateNote(NoteJRso note) {
		//this.mongoDb.getCollection(T_TOPIC_NAME, Topic.class).
		RespJRso<Boolean> r = new RespJRso<>(RespJRso.CODE_FAIL);
		ActInfoJRso ai = JMicroContext.get().getAccount();
		Map<String,Object> q = new HashMap<>();
		q.put(IObjectStorage._ID, note.getId());
		
		NoteJRso t = this.os.getOne(T_NOTE_NAME, q, NoteJRso.class);
		if(t != null && t.getCreatedBy() == ai.getId()) {
			t.setContent(note.getContent());
			this.os.update(T_NOTE_NAME, q, t,NoteJRso.class);
			r.setCode(RespJRso.CODE_SUCCESS);
			r.setData(true);
		} else {
			r.setMsg("No permission");
			r.setKey("NoPermission");
		}
		return r;
	}
	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=1,maxPacketSize=256)
	public RespJRso<Boolean> deleteNote(Long noteId) {
		
		RespJRso<Boolean> r = new RespJRso<>(RespJRso.CODE_FAIL);
		ActInfoJRso ai = JMicroContext.get().getAccount();
		Map<String,Object> q = new HashMap<>();
		q.put(IObjectStorage._ID, noteId);
		
		NoteJRso t = this.os.getOne(T_NOTE_NAME, q, NoteJRso.class);
		if(t != null && t.getCreatedBy() == ai.getId()) {
			this.os.deleteById(T_NOTE_NAME, noteId, IObjectStorage._ID);
			r.setCode(RespJRso.CODE_SUCCESS);
			r.setData(true);
		}else {
			r.setMsg("No permission");
			r.setKey("NoPermission");
		}
		return r;
	}
	
	private void addReadNum(TopicJRso t) {
		Map<String,Object> q = new HashMap<>();
		q.put(IObjectStorage._ID, t.getId());
		t.setReadNum(t.getReadNum()+1);
		this.os.update(T_TOPIC_NAME, q, t,TopicJRso.class);
	}
	
	private void addNoteNum(long tid) {
		Map<String,Object> q = new HashMap<>();
		q.put(IObjectStorage._ID, tid);
		TopicJRso t = this.os.getOne(T_TOPIC_NAME, q, TopicJRso.class);
		if(t != null) {
			t.setNoteNum(t.getNoteNum()+1);
			this.os.update(T_TOPIC_NAME, q, t,TopicJRso.class);
		}
	}
	
	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=1,maxPacketSize=204800)
	public RespJRso<Boolean> updateTopic(TopicJRso topic) {
		//this.mongoDb.getCollection(T_TOPIC_NAME, Topic.class).
		RespJRso<Boolean> r = new RespJRso<>(RespJRso.CODE_FAIL);
		ActInfoJRso ai = JMicroContext.get().getAccount();
		Map<String,Object> q = new HashMap<>();
		q.put(IObjectStorage._ID, topic.getId());
		
		TopicJRso t = this.os.getOne(T_TOPIC_NAME, q, TopicJRso.class);
		if(t != null && t.getCreatedBy() == ai.getId()) {
			t.setContent(topic.getContent());
			t.setTitle(topic.getTitle());
			t.setTopicType(topic.getTopicType());
			this.os.update(T_TOPIC_NAME, q, t,TopicJRso.class);
			r.setCode(RespJRso.CODE_SUCCESS);
			r.setData(true);
		} else {
			r.setMsg("No permission");
			r.setKey("NoPermission");
		}
		return r;
	}

	@Override
	public RespJRso<Boolean> deleteTopic(Long topicId) {
		
		RespJRso<Boolean> r = new RespJRso<>(RespJRso.CODE_FAIL);
		ActInfoJRso ai = JMicroContext.get().getAccount();
		Map<String,Object> q = new HashMap<>();
		q.put(IObjectStorage._ID, topicId);
		
		TopicJRso t = this.os.getOne(T_TOPIC_NAME, q, TopicJRso.class);
		if(t != null && t.getCreatedBy() == ai.getId()) {
			this.os.deleteById(T_TOPIC_NAME, topicId,IObjectStorage._ID);
			r.setCode(RespJRso.CODE_SUCCESS);
			r.setData(true);
		}else {
			r.setMsg("No permission");
			r.setKey("NoPermission");
		}
		return r;
	}

	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=10,maxPacketSize=204800)
	public RespJRso<Boolean> createTopic(TopicJRso topic) {
		RespJRso<Boolean> r = new RespJRso<>(RespJRso.CODE_FAIL);
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
		
		long curTime = TimeUtils.getCurTime();
		ActInfoJRso ai = JMicroContext.get().getAccount();
		
		topic.setClientId(ai.getClientId());
		topic.setCreatedBy(ai.getId());
		topic.setUpdatedTime(curTime);
		topic.setId(idGenerator.getLongId(TopicJRso.class));
		topic.setLocked(false);
		topic.setCreatedTime(curTime);
		topic.setCreaterName(ai.getActName());
		
		this.mongoDb.getCollection(T_TOPIC_NAME, TopicJRso.class).insertOne(topic);
		
		r.setData(true);
		r.setCode(RespJRso.CODE_SUCCESS);
		return r;
	}

	@Override
	public RespJRso<List<TopicJRso>> topicList(Map<String, String> qry, int pageSize, int curPage) {
		
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
		MongoCollection<TopicJRso> rpcLogColl = this.mongoDb.getCollection(T_TOPIC_NAME, TopicJRso.class);
		AggregateIterable<TopicJRso> resultset = rpcLogColl.aggregate(aggregateList,TopicJRso.class);
		MongoCursor<TopicJRso> cursor = resultset.iterator();

		RespJRso<List<TopicJRso>> resp = new RespJRso<>();
		List<TopicJRso> rl = new ArrayList<>();
		resp.setData(rl);

		try {
			while (cursor.hasNext()) {
				TopicJRso vo = cursor.next();
				vo.setContent("");
				if (vo != null) {
					rl.add(vo);
				}
			}
			resp.setCode(RespJRso.CODE_SUCCESS);
		} finally {
			cursor.close();
		}

		return resp;
	}

	@Override
	public RespJRso<Long> countTopic(Map<String, String> qry) {
		Document match = this.getCondtions(qry);
		MongoCollection<Document> rpcLogColl = mongoDb.getCollection(T_TOPIC_NAME);
		RespJRso<Long> resp = new RespJRso<>();
		Long cnt = rpcLogColl.countDocuments(match);
		resp.setData(cnt);
		resp.setCode(RespJRso.CODE_SUCCESS);
		return resp;
	}

	@Override
	public RespJRso<TopicVoJRso> getTopic(long topicId) {
		RespJRso<TopicVoJRso> resp = new RespJRso<>(RespJRso.CODE_FAIL);
		Document qryMatch = new Document("_id",topicId);
		FindIterable<TopicJRso> fi = mongoDb.getCollection(T_TOPIC_NAME,TopicJRso.class)
				.find(qryMatch, TopicJRso.class);
		TopicJRso t = null;
		if(fi != null && (t=fi.first()) != null) {
			TopicVoJRso vo = new TopicVoJRso();
			vo.setTopic(t);
			RespJRso<List<NoteJRso>> notes = this.topicNoteList(topicId, Integer.MAX_VALUE, 0);
			vo.setNotes(notes.getData());
			resp.setData(vo);
			resp.setCode(RespJRso.CODE_SUCCESS);
			addReadNum(t);
		} else {
			resp.setData(null);
			resp.setMsg("Not found");
			resp.setKey("NotFound");
		}
		return resp;
	}

	@Override
	public RespJRso<List<NoteJRso>> topicNoteList(long topicId, int pageSize, int curPage) {
		
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

		MongoCollection<NoteJRso> rpcLogColl = mongoDb.getCollection(T_NOTE_NAME,NoteJRso.class);
		AggregateIterable<NoteJRso> resultset = rpcLogColl.aggregate(aggregateList,NoteJRso.class);
		MongoCursor<NoteJRso> cursor = resultset.iterator();

		RespJRso<List<NoteJRso>> resp = new RespJRso<>();
		List<NoteJRso> rl = new ArrayList<>();
		resp.setData(rl);

		try {
			while (cursor.hasNext()) {
				NoteJRso vo = cursor.next();
				if (vo != null) {
					rl.add(vo);
				}
			}
			resp.setCode(RespJRso.CODE_SUCCESS);
		} finally {
			cursor.close();
		}

		return resp;
	
	}

	@Override
	public RespJRso<NoteJRso> createNote(NoteJRso note) {
		RespJRso<NoteJRso> r = new RespJRso<>(RespJRso.CODE_FAIL);
		if(Utils.isEmpty(note.getContent())) {
			r.setMsg("Content cannot be null");
			r.setKey("ContentIsNull");
			r.setData(null);
			return r;
		}
		
		if(note.getTopicId() <= 0) {
			r.setMsg("Invalid topic");
			r.setKey("InvalidTopicId");
			r.setData(null);
			return r;
		}
		
		long curTime = TimeUtils.getCurTime();
		ActInfoJRso ai = JMicroContext.get().getAccount();
		
		note.setClientId(ai.getClientId());
		note.setCreatedBy(ai.getId());
		note.setId(idGenerator.getLongId(NoteJRso.class));
		note.setCreatedTime(curTime);
		note.setCreaterName(ai.getActName());
		
		os.save(T_NOTE_NAME, note,NoteJRso.class, true,false);
		
		addNoteNum(note.getTopicId());
		
		r.setData(note);
		r.setCode(RespJRso.CODE_SUCCESS);
		
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
	
	private TopicJRso fromJson(String json) {
		GsonBuilder builder = new GsonBuilder();
		TopicJRso psd = builder.create().fromJson(json, TopicJRso.class);
		return psd;
	}
}
