package cn.jmicro.ext.mongodb;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
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

import com.google.gson.JsonSyntaxException;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.util.JsonUtils;

@Component
public class MongodbBaseObjectStorage implements IObjectStorage {

	private final static Logger logger = LoggerFactory.getLogger(MongodbBaseObjectStorage.class);
	
	public static final JsonWriterSettings settings = JsonWriterSettings.builder()
	         .int64Converter((value, writer) -> writer.writeNumber(value.toString()))
	         .build();
	
	@Inject
	private MongoDatabase mdb;
	
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
	
	public void ready() {
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
			}catch(Throwable e) {
				logger.error("",e);
			}
		}
	}
	
	private boolean updateOneById(MongoCollection<Document> coll, Document d, long curTime) {
		Document filter = new Document();
		try {
			String idKey = ID;
			if(d.containsKey(_ID)) {
				//无下横线整数ID优先级最高
				idKey = _ID;
			}
			
			Object idv = d.get(idKey);
			if(idv != null && idv instanceof Integer) {
				filter.put(idKey, d.getInteger(idKey));
			}else {
				filter.put(idKey, d.getLong(idKey));
			}
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
		return ur.getModifiedCount() != 0;
	}

	private <T> void doSave(String table, List<T> batchAdds, Class<T> targetClass) {
		mdb.getCollection(table,targetClass).insertMany(batchAdds);
	}
	
	@Override
	public <T> boolean save(String table, List<T> val, Class<T> cls,boolean async,boolean toDocument) {

		if(val == null || val.isEmpty()) {
			return false;
		}
		
		MongoCollection coll = null;
		List lis = val;
		
		if(true) {
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
	public <T> boolean save(String table, T val,Class<T> cls,boolean async,boolean toDocument) {
		if(val == null) {
			return false;
		}
		
		MongoCollection coll = null;
		
		Object v = val;
		if(true) {
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
	public <T> boolean save(String table, T[] vals,Class<T> cls,boolean async,boolean toDocument) {
		if(vals == null || vals.length == 0) {
			return false;
		}
		
		long curTime = TimeUtils.getCurTime();
		MongoCollection coll = null;
		
		List lis = Arrays.asList(vals);
		
		if(true) {
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
		 Document match = this.getCondtions(queryConditions);
		 FindIterable<T> rst = mdb.getCollection(table)
				 .find(match, targetClass)
				 .limit(pageSize)
				 .skip(pageSize*curPage);
		 List<T> arr = new ArrayList<>();
		 Iterator<T> ite = rst.iterator();
		 while(ite.hasNext()) {
			 arr.add(ite.next());
		 }
		return arr;
	}

	@Override
	public <T> boolean updateOrSaveById(String table, T val,Class<T> cls,String tidName,boolean async) {
		if(async) {
			updateById(table,val,cls,tidName,true);
		} else {
			if(!updateById(table,val,cls,tidName,false)) {
				return save(table,val,cls,false,true);
			}
		}
		return true;
	}

	@Override
	public <T>  boolean update(String table, Object filter, Object updater,Class<T> targetClass) {
		
		Document up = null;
		if(updater instanceof Document) {
			up = (Document)updater;
		} else {
			 up = new Document();
			 Document up0 = Document.parse(JsonUtils.getIns().toJson(updater));
			 if(!up0.containsKey(UPDATED_TIME)) {
				 up0.put(UPDATED_TIME, TimeUtils.getCurTime());
			 }
			 /*if(!up0.containsKey(CREATED_TIME)) {
				 up0.put(CREATED_TIME, TimeUtils.getCurTime());
			 }*/
			 up.put("$set",  up0);
		}
		
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
		UpdateResult ur = coll.updateOne(fi,up,new UpdateOptions().upsert(false));
		return ur.getModifiedCount() != 0;
	}

	@Override
	public <T> List<T> query(String table, Map<String, Object> queryConditions, Class<T> targetClass) {
		 Document match = this.getCondtions(queryConditions);
		 FindIterable<T> rst = mdb.getCollection(table).find(match, targetClass);
		 List<T> arr = new ArrayList<>();
		 Iterator<T> ite = rst.iterator();
		 while(ite.hasNext()) {
			 arr.add(ite.next());
		 }
		return arr;
	}

	@Override
	public long count(String table, Map<String, Object> queryConditions) {
		Document match = this.getCondtions(queryConditions);
		MongoCollection<Document> rpcLogColl = mdb.getCollection(table);
		return rpcLogColl.countDocuments(match);
	}
	
	private Document getCondtions(Map<String, Object> queryConditions) {
		Document match = new Document();
		for(String key : queryConditions.keySet()) {
			match.put(key, queryConditions.get(key));
		}
		return match;
	}

	@Override
	public <T> T getOne(String table, Map<String, Object> queryConditions, Class<T> targetClass) {
		 Document match = this.getCondtions(queryConditions);
		 FindIterable<Document> rst = mdb.getCollection(table,Document.class).find(match);
		 Document doc = rst.first();
		 if(doc != null) {
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
		 }
		 return null;
	}
	
	@Override
	public Map<String, Object> getFields(String table, Map<String, Object> filter, String[] fields) {
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
		 return rst.first();
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
