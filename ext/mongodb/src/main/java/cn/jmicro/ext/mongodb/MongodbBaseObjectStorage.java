package cn.jmicro.ext.mongodb;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.data.AbstractReportElt;
import cn.jmicro.api.data.ReportDataJRso;
import cn.jmicro.api.data.ReportSeriesArrayEltJRso;
import cn.jmicro.api.data.ReportSeriesSingleEltJRso;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.storage.FileJRso;
import cn.jmicro.api.utils.DateUtils;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.FileUtils;
import cn.jmicro.common.util.JsonUtils;

@Component(level=1)
public class MongodbBaseObjectStorage implements IObjectStorage {

	private final static Logger logger = LoggerFactory.getLogger(MongodbBaseObjectStorage.class);
	
	public static final JsonWriterSettings settings = JsonWriterSettings.builder()
	         .int64Converter((value, writer) -> writer.writeNumber(value.toString()))
	         .build();
	
	@Inject
	private MongoDatabase mdb;
	
	@Inject(required=false)
	private GridFS fs;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	private Object syncLocker = new Object();
	
	private ReentrantLock addLocker = new ReentrantLock();
	
	private ReentrantLock updateLocker = new ReentrantLock();
	
	private long lastAddTime = TimeUtils.getCurTime();
	
	private long lastUpdateTime = TimeUtils.getCurTime();
	
	private int maxCacheSize = 100;
	
	private long timeout = 1000*3;
	
	private Map<String,SaveOp> saves = new HashMap<>();
	private Map<String,SaveOp> tempAdds = new HashMap<>();
	
	private Map<String,List<Document>> updates = new HashMap<>();
	private Map<String,List<Document>> tempUpdates = new HashMap<>();
	
	private AtomicInteger addCnt = new AtomicInteger(0);
	
	private AtomicInteger updateCnt = new AtomicInteger(0);
	
	public void jready() {
		new Thread(this::doWork).start();
	}
	
	private void doWork() {
		while(true) {
			try {

				if(saves.isEmpty() && updates.isEmpty()) {
					synchronized(syncLocker) {
						try {
							syncLocker.wait(timeout);
						} catch (InterruptedException e) {
							logger.error("",e);
						}
					}
					continue;
				}
				
				long curTime = TimeUtils.getCurTime();
				if(!saves.isEmpty()) {
					if(addCnt.get() > this.maxCacheSize || curTime - lastAddTime > this.timeout) {
						
						boolean ls = addLocker.tryLock();
						if(ls) {
							try {
								addCnt.set(0);
								tempAdds.putAll(this.saves);
								this.saves.clear();
							}finally {
								if(ls) {
									addLocker.unlock();
								}
							}
							if(!tempAdds.isEmpty()) {
								for(Map.Entry<String, SaveOp> e : this.tempAdds.entrySet()) {
									//doSaveForAsync(e.getValue());
									e.getValue().coll.insertMany(e.getValue().vals);
								}
								tempAdds.clear();
							}
							lastAddTime = curTime;
						} 
					}
				}
				
				if(!updates.isEmpty()) {
					if(updateCnt.get() > maxCacheSize || curTime - lastUpdateTime > this.timeout) {
						boolean ls = updateLocker.tryLock();
						if(ls) {
							try {
								updateCnt.set(0);
								tempUpdates.putAll(this.updates);
								updates.clear();
							} finally {
								if(ls) {
									updateLocker.unlock();
								}
							}
							if(!tempUpdates.isEmpty()) {
								for(Map.Entry<String, List<Document>> e : this.tempUpdates.entrySet()) {
									MongoCollection<Document> coll = mdb.getCollection(e.getKey());
									for(Document o : e.getValue()) {
										this.updateOneById(coll, o, curTime);
									}
								}
								tempUpdates.clear();
							}
							lastUpdateTime = curTime;
						} 
					}
				}
			} catch (Throwable e) {
				logger.error("",e);
			}
		}
	}
	
	@Override
	public boolean fileSystemEnable() {
		return this.fs != null;
	}

	private String getFileId(String fn) {
		return idGenerator.getStringId(FileJRso.class)+"."+FileUtils.getFileExt(fn);
	}
	
	@Override
	public RespJRso<String> saveSteam2Db(FileJRso pr, InputStream is) {

		RespJRso<String> r = new RespJRso<>(RespJRso.CODE_FAIL,pr.getId());
		
		if(Utils.isEmpty(pr.getId())) {
			pr.setId(getFileId(pr.getName()));
			r.setData(pr.getId());
		}
		
		GridFSInputFile ff = this.fs.createFile();
		
		ff.setChunkSize(pr.getSize());
		ff.setContentType(pr.getType());
		ff.setFilename(pr.getName());
		ff.setId(pr.getId());
		
		DBObject mt = new BasicDBObject();
		mt.putAll(pr.getAttr());
		mt.put("createdBy", pr.getCreatedBy());
		mt.put("clientId", pr.getClientId());
		mt.put("group", pr.getGroup());
		ff.setMetaData(mt);
		
		OutputStream fos = null;
		int bs = 1024*4;
		byte[] data = new byte[bs];
		
		int len = 0;
		
		try {
			fos = ff.getOutputStream();
			while((len = is.read(data, 0, bs)) > 0) {
				fos.write(data, 0, len);
			}
		} catch (IOException e) {
			logger.error("",e);
			try {
				if(fos != null) fos.close();
				if(is != null) is.close();
			} catch (IOException e1) {
				logger.error("",e1);
			}
		}
		
		pr.setLocalPath("");
		this.save(FileJRso.TABLE, pr, FileJRso.class, false);
		
		r.setCode(RespJRso.CODE_SUCCESS);
		return r;
	
	}

	@Override
	public RespJRso<String> saveByteArray2Db(FileJRso pr, byte[] byteData) {
		return saveSteam2Db(pr,new ByteArrayInputStream(byteData));
	}

	@Override
	public RespJRso<String> saveFile2Db(FileJRso pr) {
		RespJRso<String> r = new RespJRso<>(RespJRso.CODE_FAIL,pr.getId());
		if(Utils.isEmpty(pr.getLocalPath())) {
			r.setMsg("存储文件不存在");
			return r;
		}
		
		File f = new File(pr.getLocalPath());
		if(!f.exists()) {
			r.setMsg("存储文件未找到，请联系管理员");
			return r;
		}
		
		if(Utils.isEmpty(pr.getId())) {
			pr.setId(getFileId(pr.getName()));
			r.setData(pr.getId());
		}
		
		GridFSInputFile ff = this.fs.createFile();
		
		ff.setChunkSize(pr.getSize());
		ff.setContentType(pr.getType());
		ff.setFilename(pr.getName());
		ff.setId(pr.getId());
		
		DBObject mt = new BasicDBObject();
		mt.putAll(pr.getAttr());
		mt.put("createdBy", pr.getCreatedBy());
		mt.put("clientId", pr.getClientId());
		mt.put("group", pr.getGroup());
		ff.setMetaData(mt);
		
		OutputStream fos = null;
		int bs = 1024*4;
		byte[] data = new byte[bs];
		
		InputStream is = null;
		int len = 0;
		
		try {
			fos = ff.getOutputStream();
			is = new FileInputStream(f);
			while((len = is.read(data, 0, bs)) > 0) {
				fos.write(data, 0, len);
			}
		} catch (IOException e) {
			logger.error("",e);
			try {
				if(fos != null) fos.close();
				if(is != null) is.close();
			} catch (IOException e1) {
				logger.error("",e1);
			}
		}
		
		pr.setLocalPath("");
		this.save(FileJRso.TABLE, pr, FileJRso.class, false);
		
		r.setCode(RespJRso.CODE_SUCCESS);
		return r;
	}
	
	
	private boolean updateOneById(MongoCollection<Document> coll, Document d, long curTime) {
		Document filter = new Document();
		try {
			//String idKey = ID;
			if(!d.containsKey(ID)) {
				//无下横线整数ID优先级最高
				//idKey = _ID;
				throw new CommonException("id field not found: " + d.toJson());
			}
			
			filter.put(_ID, d.getInteger(ID));
			d.remove(ID);
			
			/*Object idv = d.get(idKey);
			if(idv != null && idv instanceof Integer) {
				filter.put(_ID, d.getInteger(ID));
			}else {
				filter.put(idKey, d.getLong(idKey));
			}*/
		} catch (Exception e) {
			filter.put(_ID, d.getObjectId(_ID));
		}
		
		Document update = new Document();
		
		if(!d.containsKey(UPDATED_TIME)) {
			d.put(UPDATED_TIME, curTime);
		}
		
		if(!d.containsKey(CREATED_TIME)) {
			d.put(CREATED_TIME, curTime);
		}
		
		update.put("$set",  d);
		
		UpdateResult ur = coll.updateOne(filter,update,new UpdateOptions().upsert(false));
		return ur.getModifiedCount() > 0;
	}

	private <T> void doSave(String table, List<T> batchAdds, Class<T> targetClass) {
		mdb.getCollection(table,targetClass).insertMany(batchAdds);
	}
	
	@Override
	public <T> boolean save(String table, List<T> val, Class<T> cls,boolean async) {

		if(val == null || val.isEmpty()) {
			return false;
		}
		
		MongoCollection coll = null;
		List lis = val;
		
		if(/*toDocument*/false) {
			lis = new ArrayList();
			for(Object v : val) {
				lis.add(toDocument(v));
			}
			coll = mdb.getCollection(table);
		} else {
			coll = mdb.getCollection(table,cls);
		}
		
		if(async) {
			boolean ls = addLocker.tryLock();
			if(ls) {
				try {
					if(!this.saves.containsKey(table)) {
						createOp(table,cls,coll);
					}
					this.saves.get(table).addAll(lis);
					addCnt.addAndGet(val.size());
				} finally {
					if(ls) {
						addLocker.unlock();
					}
				}
			} 
			
			if(ls) {
				synchronized(syncLocker) {
					syncLocker.notify();
				}
			}
		} else {
			coll.insertMany(lis);
		}
		
		return true;
	
	}

	private void createOp(String table, Class<?> cls, MongoCollection coll) {
		SaveOp op = new SaveOp();
		op.cls = cls;
		op.coll = coll;
		op.vals = new ArrayList();
		this.saves.put(table,op);
	}
	
	private Document toDocument(Object val) {
		Document d = null;
		if(val instanceof Document) {
			d = (Document)val;
		} else {
			d = Document.parse(JsonUtils.getIns().toJson(val));
		}
		
		if(d.containsKey(ID)) {
			try {
				d.put(ID, new Long(d.getInteger(ID)));
			} catch (Exception e) {}
		}
		
		if(!d.containsKey(UPDATED_TIME)) {
			d.put(UPDATED_TIME, TimeUtils.getCurTime());
		}
		
		if(!d.containsKey(CREATED_TIME)) {
			d.put(CREATED_TIME, TimeUtils.getCurTime());
		}
		return d;
	}
	
	@Override
	public <T> boolean saveSync(String table, T val,Class<T> cls) {
		return this.save(table, val, cls,false);
	}

	@Override
	public <T> boolean save(String table, T val,Class<T> cls,boolean async) {
		if(val == null) {
			return false;
		}
		
		MongoCollection coll = null;
		
		boolean toDocument = false;
		Object v = val;
		if(toDocument) {
			v = toDocument(val);
			coll = mdb.getCollection(table);
		} else {
			coll = mdb.getCollection(table,cls);
		}
		
		if(async) {
			boolean ls = addLocker.tryLock();
			if(ls) {
				try {
					if(!this.saves.containsKey(table)) {
						createOp(table,cls,coll);
					}
					addCnt.incrementAndGet();
					this.saves.get(table).add(v);
				} finally {
					if(ls) {
						addLocker.unlock();
					}
				}
			} 
			
			if(ls) {
				synchronized(syncLocker) {
					syncLocker.notify();
				}
			}
		} else {
			coll.insertOne(v);
		}
		
		return true;
	}

	@Override
	public <T> boolean save(String table, T[] vals,Class<T> cls,boolean async) {
		if(vals == null || vals.length == 0) {
			return false;
		}
		
		long curTime = TimeUtils.getCurTime();
		MongoCollection coll = null;
		
		List lis = Arrays.asList(vals);
		boolean toDocument = false;
		if(toDocument) {
			lis = new ArrayList();
			for(int i = 0; i < vals.length; i++) {
				lis.add(toDocument(vals[i]));
			}
			coll = mdb.getCollection(table);
		} else {
			
			coll = mdb.getCollection(table,cls);
		}
		
		if(async) {
			boolean ls = addLocker.tryLock();
			if(ls) {
				try {
					if(!this.saves.containsKey(table)) {
						createOp(table,cls,coll);
					}
					addCnt.incrementAndGet();
					this.saves.get(table).addAll(lis);;
				} finally {
					if(ls) {
						addLocker.unlock();
					}
				}
			} 
			
			if(ls) {
				synchronized(syncLocker) {
					syncLocker.notify();
				}
			}
		} else {
			coll.insertMany(lis);
		}
		
		return true;
	}

	@Override
	public <T>  boolean updateById(String table,T val,Class<T> targetClass,String idName,boolean async) {
		
		Document d = null;
		if(val instanceof Document) {
			d = (Document)val;
		} else {
			 d = Document.parse(JsonUtils.getIns().toJson(val));
		}
		
		/*if(!d.containsKey(idName)) {
			if(d.containsKey(ID)) {
				d.put(idName, d.getLong(ID));
				d.remove(ID);
			}else if(d.containsKey(_ID)) {
				d.put(idName, d.getLong(_ID));
			}
		}*/
		
		if(async) {
			
			boolean ls = updateLocker.tryLock();
			if(ls) {
				try {
					updateCnt.incrementAndGet();
					if(!this.updates.containsKey(table)) {
						this.updates.put(table, new ArrayList<>());
					}
					this.updates.get(table).add(d);
				}finally {
					if(ls) {
						updateLocker.unlock();
					}
				}
				synchronized(syncLocker) {
					syncLocker.notify();
				}
			} 
			
			//异步更新不保证结果绝对成功
			return true;
		} else {
			MongoCollection<Document> coll = mdb.getCollection(table);
			return updateOneById(coll,d,TimeUtils.getCurTime());
		}
	}

	@Override
	public boolean deleteById(String table, Object id,String idName) {
		MongoCollection<Document> coll = mdb.getCollection(table);
		Document filter = new Document();
		filter.put(idName, id);
		DeleteResult rst = coll.deleteOne(filter);
		return rst.getDeletedCount() > 0;
	}
	
	@Override
	public <T> Set<T> distinct(String table, String fieldName, Class<T> cls) {
		DistinctIterable<T> rst = mdb.getCollection(table).distinct(fieldName,cls);
		if(rst != null) {
			Set<T> l = new HashSet<>();
			Iterator<T> ite = rst.iterator();
			while(ite.hasNext()) {
				l.add(ite.next());
			}
			return l;
		}
		return null;
	}

	public int deleteByQuery(String table, Object query) {
		Document filter = null;
		if(query instanceof Document) {
			 filter = (Document) query; 
		}else {
			filter = Document.parse(JsonUtils.getIns().toJson(query));
		}
		MongoCollection<Document> coll = mdb.getCollection(table);
		DeleteResult dr = coll.deleteMany(filter);
		return (int)dr.getDeletedCount();
	}

	@Override
	public <T> List<T> query(String table,Map<String, Object> queryConditions, Class<T> targetClass,
			int pageSize,int curPage) {
		return this.query(table,queryConditions,targetClass,pageSize,curPage,null,null,0);
	}

	@Override
	public <T> boolean updateOrSaveById(String table, T val,Class<T> cls,String tidName,boolean async) {
		if(async) {
			updateById(table,val,cls,tidName,true);
		} else {
			if(!updateById(table,val,cls,tidName,false)) {
				return save(table,val,cls,false);
			}
		}
		return true;
	}

	@Override
	public <T>  int update(String table, Object filter, Object updater,Class<T> targetClass) {
		
		Document up = null;
		if(updater instanceof Document) {
			up = (Document)updater;
		} else {
			 up = new Document();
			 Document up0 = Document.parse(JsonUtils.getIns().toJson(updater));
			 if(!up0.containsKey(UPDATED_TIME)) {
				 up0.put(UPDATED_TIME, TimeUtils.getCurTime());
			 }
			/* if(!up0.containsKey(CREATED_TIME)) {
				 up0.put(CREATED_TIME, TimeUtils.getCurTime());
			 }*/
			 up.put("$set",up0);
		}
		
		/*if(!up.containsKey("$set")) {
			Document up0 = new Document();
			up0.put("$set",  up);
			up= up0;
		}*/
		
		Document fi = null;
		if(filter instanceof Document) {
			fi = (Document)filter;
		} else if(filter instanceof Map) {
			 fi = Document.parse(JsonUtils.getIns().toJson(filter));
		} else {
			fi = new Document();
			fi.put(ID, Integer.parseInt(filter.toString()));
		}
	
		MongoCollection<Document> coll = mdb.getCollection(table);
		UpdateResult ur = coll.updateMany(fi,up,new UpdateOptions().upsert(false));
		return (int)ur.getModifiedCount();
	}

	@Override
	public <T> List<T> query(String table, Map<String, Object> queryConditions, Class<T> targetClass) {
		return this.query(table,queryConditions,targetClass,-1,-1,null,null,0);
	}
	
	@Override
	public <T> List<T> query(String table, Map<String, Object> queryConditions, Class<T> targetClass, String orderBy,
			Integer asc) {
		return this.query(table,queryConditions,targetClass,-1,-1,null,orderBy,asc);
	}

	@Override
	public <T> List<T> query(String table, Map<String, Object> queryConditions, Class<T> targetClass, int pageSize,
			int curPage, String[] colums, String orderBy, Integer asc) {

		 Document match = this.getCondtions(queryConditions);
		 FindIterable<T> rst = mdb.getCollection(table)
				 .find(match, targetClass);
		 
		 if(colums != null && colums.length > 0) {
			 Document prj = new Document();
			 for(String c : colums) {
				 if(!Utils.isEmpty(c)) {
					 prj.put(c, 1);
				 }
			 }
			 rst.projection(prj);
		 }
		 
		 if(!Utils.isEmpty(orderBy)) {
			 rst.sort(new Document(orderBy,asc));
		 }
				 
		 if(pageSize > 0) {
			 rst.limit(pageSize);
		 }
		 
		 if(pageSize > 0 && curPage > -1) {
			 rst.skip(pageSize*curPage);
		 }
		
		 return resultList(rst);
	}

	private <T> List<T> resultList(FindIterable<T> rst) {
		 List<T> arr = new ArrayList<>();
		 Iterator<T> ite = rst.iterator();
		 while(ite.hasNext()) {
			 arr.add(ite.next());
		 }
		return arr;
	}

	@Override
	public <T> List<T> query(String table, Map<String, Object> queryConditions, Class<T> targetClass, int pageSize,
			int curPage, String orderBy, Integer asc) {
		return this.query(table,queryConditions,targetClass,pageSize,curPage,null,orderBy,asc);
	}

	@Override
	public int count(String table, Map<String, Object> queryConditions) {
		Document match = this.getCondtions(queryConditions);
		MongoCollection<Document> rpcLogColl = mdb.getCollection(table);
		return (int)rpcLogColl.countDocuments(match);
	}
	
	private Document getCondtions(Map<String, Object> queryConditions) {
		if(queryConditions instanceof Document) {
			return (Document)queryConditions;
		}
		Document match = Document.parse(JsonUtils.getIns().toJson(queryConditions));
		/*for(String key : queryConditions.keySet()) {
			match.put(key, queryConditions.get(key));
		}*/
		return match;
	}

	@Override
	public <T> T getOne(String table, Map<String, Object> queryConditions, Class<T> targetClass) {
		 Document match = this.getCondtions(queryConditions);
		 FindIterable<T> rst = mdb.getCollection(table,targetClass).find(match);
		 return rst.first();
		 
		/* if(doc != null) {
			
			 T ov = JsonUtils.getIns().fromJson(doc.toJson(settings), targetClass);
			 try {
				 if(doc.containsKey(_ID)) {
					 Field idf = targetClass.getDeclaredField(ID);
					 if(idf != null) {
						 String idv = doc.get(_ID).toString();
						 idf.setAccessible(true);
						 idf.set(ov, JsonUtils.getIns().fromJson(idv, idf.getType()));
					 }
				 }
			} catch (JsonSyntaxException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				//logger.error("",e);
			}
			 
			 return ov;
		 }*/
	}
	
	@Override
	public List<Map<String, Object>> getFields(String table, Map<String, Object> filter, String[] fields) {
		 if(fields == null) {
			 throw new NullPointerException("get fields is NULL");
		 }
		 
		 Document fs = new Document();
		 fs.putAll(filter);
		 
		 Document prj = new Document();
		 for(String f : fields) {
			 prj.put(f, 1);
		 }
		 FindIterable<Document> rst = mdb.getCollection(table,Document.class).find(fs).projection(prj);
		 
		 List<Map<String, Object>> l = new ArrayList<>();
		 MongoCursor<Document>  ite = rst.iterator();
		 
		 while(ite.hasNext()) {
			 l.add(ite.next());
		 }
		 
		 return l;
	}
	
	/**
	 * 
	 * @param filter 数据过虑条件
	 * @param table  表名
	 * @param keys  要选择的列名
	 * @param grpFieldName 分组名
	 * @param dayNum 查询多少天的数据
	 * @param timeLen 日期格式，参考ReportDataJRso.TIME_LEN_YMD,TIME_LEN_MD,TIME_LEN_YM
	 * @param counter 是否统计记录条数，默认否
	 * @return
	 */
	public ReportDataJRso<AbstractReportElt> statisDataByCreatedDate(Map<String,Object> filter, String table,
			String[] keys, String grpFieldName, Byte dayNum, Byte timeLen,Boolean counter) {
		
		ReportDataJRso<AbstractReportElt> report = new ReportDataJRso<>();
		
		long ed =  getYesterday();
		report.setCategories(getDateCategories(ed,dayNum,timeLen));
		
		/*
		String[] keys = new String[] {"settlement","amount"};
		String grpFieldName = "dateStr";
		String table = ShareBuyJRso.TABLE;
		*/
		
		if(counter) {
			String[] ks = new String[keys.length+1];
			System.arraycopy(keys, 0, ks, 0, keys.length);
			ks[keys.length] = "count";
			keys = ks;
		}
		
		ReportSeriesArrayEltJRso[] series = new ReportSeriesArrayEltJRso[keys.length];
		report.setSeries(series);
		
		//report.setSeries(new ReportSeriesArrayEltJRso("buyNum","购买数",dayNum), 0);
		for(int i = 0; i < keys.length; i++) {
			report.setSeries(new ReportSeriesArrayEltJRso(keys[i], null, dayNum), i);
		}

		List<Document> aggregateList = new ArrayList<Document>();
		
		Document qryMatch = Document.parse(JsonUtils.getIns().toJson(filter));
		//qryMatch.put("shareActId", aid);//由自己的分享带来的购买
		//qryMatch.put("status", ShareBuyJRso.STATUS_FINISH);
		
		Document timeLimit = new Document();
		timeLimit.put("$gt", ed - dayNum * Constants.DAY_IN_MILLIS);
		timeLimit.put("$lt",ed);
		
		qryMatch.put("createdTime", timeLimit);
		
		Document match = new Document("$match", qryMatch);
		aggregateList.add(match);
		
		Document p = new Document();
		p.append(IObjectStorage._ID, 0);
		for(String k : keys) {
			p.append(k, 1);
		}
		
		//1970-01-01
		switch(timeLen) {
		case ReportDataJRso.TIME_LEN_YM:
			p.append(grpFieldName, new Document("$substr",Arrays.asList("$"+grpFieldName,0,7)));
			break;
		case ReportDataJRso.TIME_LEN_MD:
			p.append(grpFieldName, new Document("$substr",Arrays.asList("$"+grpFieldName,5,5)));
					break;
		case ReportDataJRso.TIME_LEN_YMD:
			p.append(grpFieldName, 1);
			break;
		case ReportDataJRso.TIME_LEN_Y:
			p.append(grpFieldName, new Document("$substr",Arrays.asList("$"+grpFieldName,0,4)));
			break;
		case ReportDataJRso.TIME_LEN_M:
			p.append(grpFieldName, new Document("$substr",Arrays.asList("$"+grpFieldName,5,2)));
			break;
		case ReportDataJRso.TIME_LEN_D:
			p.append(grpFieldName, new Document("$substr",Arrays.asList("$"+grpFieldName,8,2)));
			break;
		default:
			p.append(grpFieldName, 1);
		}
		
		/*if(timeLen == ReportDataJRso.TIME_LEN_YM) {
			//ProductName: { $substr: [ "$ProductName", 0, 7]
			p.append(grpFieldName, new Document("$substr",Arrays.asList("$"+grpFieldName,0,7)));
		} else if(timeLen == ReportDataJRso.TIME_LEN_MD){
			//1970-01-01
			p.append(grpFieldName, new Document("$substr",Arrays.asList("$"+grpFieldName,5,5)));
		} else {
			p.append(grpFieldName, 1);
		}*/
		
		Document prj = new Document("$project", p);
		aggregateList.add(prj);
		
		
		Document groupDoc = new Document();
		groupDoc.append("_id", "$"+grpFieldName);
		for(String k : keys) {
			groupDoc.append(k, new Document("$sum","$"+k));
		}
		
		if(counter) {
			//计算条数
			groupDoc.append("count", new Document("$sum",1));
		}
		
		Document grp = new Document("$group", groupDoc);
		aggregateList.add(grp);
		
		qryData(report, table, aggregateList, keys);
		
		return report;
	}
	
	public ReportDataJRso<AbstractReportElt> statisDataByGroupName(
			Map<String,Object> filter, String table, String[] keys, String grpFieldName, Boolean counter) {
		
		ReportDataJRso<AbstractReportElt> report = new ReportDataJRso<>();
	
		if(counter) {
			String[] ks = new String[keys.length+1];
			System.arraycopy(keys, 0, ks, 0, keys.length);
			ks[keys.length] = "count";
			keys = ks;
		}

		List<Document> aggregateList = new ArrayList<Document>();
		
		Document qryMatch = Document.parse(JsonUtils.getIns().toJson(filter));
		Document match = new Document("$match", qryMatch);
		aggregateList.add(match);
		
		Document p = new Document();
		p.append(IObjectStorage._ID, 0);
		for(String k : keys) {
			p.append(k, 1);
		}
		
		Document prj = new Document("$project", p);
		aggregateList.add(prj);
		
		Document groupDoc = new Document();
		
		if(Utils.isEmpty(grpFieldName)) {
			groupDoc.append("_id", null);
		}else {
			groupDoc.append("_id", "$" + grpFieldName);
		}
		
		for(String k : keys) {
			groupDoc.append(k, new Document("$sum","$"+k));
		}
		
		if(counter) {
			//计算条数
			groupDoc.append("count", new Document("$sum",1));
		}
		
		Document grp = new Document("$group", groupDoc);
		aggregateList.add(grp);
		
		MongoCollection<Document> rpcLogColl = this.mdb.getCollection(table);
		MongoCursor<Document> cursor = rpcLogColl.aggregate(aggregateList).iterator();
		
		if(Utils.isEmpty(grpFieldName)) {
			qrySingleReportData(report, table, cursor, keys);
		}else {
			qryGrpData(report, table, cursor, keys);
		}
		
		return report;
	}
	
	private void qrySingleReportData(ReportDataJRso<AbstractReportElt> report, String table, 
			MongoCursor<Document> cursor, String[] keys) {

		ReportSeriesSingleEltJRso[] series = new ReportSeriesSingleEltJRso[keys.length];
		report.setSeries(series);
		
		for(int i = 0; i < keys.length; i++) {
			report.setSeries(new ReportSeriesSingleEltJRso(keys[i], null,0D), i);
		}
		
		if(cursor.hasNext()) {
			Document d = cursor.next();
			for(int i = 0; i < keys.length; i++) {
				if(d.containsKey(keys[i])) {
					AbstractReportElt s = report.getSeries(keys[i]);
					s.setData(0, new Double(d.getInteger(keys[i])));
				}
			}
		}
	}
	
	private void qryGrpData(ReportDataJRso<AbstractReportElt> report, String table, 
			MongoCursor<Document> cursor, String[] keys) {
		
		Set<String> cs = new HashSet<>();
		
		List<Document> rst = new ArrayList<>();
		
		while(cursor.hasNext()) {
			Document d = cursor.next();
			rst.add(d);
			cs.add(d.getString("_id"));
		}
		
		String[] categories = cs.toArray(new String[cs.size()]);
		report.setCategories(categories);
		
		ReportSeriesArrayEltJRso[] series = new ReportSeriesArrayEltJRso[keys.length];
		report.setSeries(series);
		
		for(int i = 0; i < keys.length; i++) {
			report.setSeries(new ReportSeriesArrayEltJRso(keys[i], null, categories.length), i);
		}
		
		for(Document d : rst) {
			String ck = d.getString("_id");
			int idx = report.getIndex(ck);
			for(String k : keys) {
				report.addData(idx, k, new Double(d.getInteger(k)));
			}
		}
		
	}

	private void qryData(ReportDataJRso<AbstractReportElt> report, String table, List<Document> aggregateList,String[] keys) {
		MongoCollection<Document> rpcLogColl = this.mdb.getCollection(table);
		MongoCursor<Document> cursor = rpcLogColl.aggregate(aggregateList).iterator();
		while(cursor.hasNext()) {
			Document d = cursor.next();
			String ck = d.getString("_id");
			int idx = report.getIndex(ck);
			if(idx < 0) continue;
			for(String k : keys) {
				report.addData(idx,k,new Double(d.getInteger(k)));
			}
		}
		
	}

	private String[] getDateCategories(long ed,Byte dayNum,Byte timeLen) {
		String[] categories = new String[dayNum];
		long ct = ed;
		
		String pattern = "YYYY-MM";
		if(timeLen == ReportDataJRso.TIME_LEN_YM) {
			pattern = "YYYY-MM-dd";
		}else if(timeLen == ReportDataJRso.TIME_LEN_MD){
			pattern = "MM-dd";
		}
		
		switch(timeLen) {
		case ReportDataJRso.TIME_LEN_YM:
			pattern = "YYYY-MM";
			break;
		case ReportDataJRso.TIME_LEN_MD:
			pattern = "MM-dd";
			break;
		case ReportDataJRso.TIME_LEN_YMD:
			pattern = "YYYY-MM-dd";
			break;
		case ReportDataJRso.TIME_LEN_Y:
			pattern = "YYYY";
			break;
		case ReportDataJRso.TIME_LEN_M:
			pattern = "MM";
			break;
		case ReportDataJRso.TIME_LEN_D:
			pattern = "dd";
			break;
		default:
			pattern = "MM-dd";
		}
		
		for(int i = dayNum-1; i >= 0; i--) {
			categories[i] = DateUtils.formatDate(new Date(ct),pattern);
			ct -= Constants.DAY_IN_MILLIS;
		}
		return categories;
	}

	private long getYesterday() {
		Calendar c = Calendar.getInstance();
		//转换为昨天的结束时间
		c.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH)-1);
		c.set(Calendar.HOUR_OF_DAY, 23);
		c.set(Calendar.MINUTE, 59);
		c.set(Calendar.SECOND, 59);
		c.set(Calendar.MILLISECOND, 999);
		return c.getTimeInMillis();
	}


	class SaveOp {
		List vals;
		Class<?> cls;
		MongoCollection<?> coll;
		
		void addAll(List l) {
			vals.addAll(l);
		}
		
		void add(Object obj) {
			vals.add(obj);
		}
	}
	
}
