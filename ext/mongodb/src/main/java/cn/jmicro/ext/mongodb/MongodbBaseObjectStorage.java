package cn.jmicro.ext.mongodb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.DistinctIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.common.util.JsonUtils;

@Component
public class MongodbBaseObjectStorage implements IObjectStorage {

	private final static Logger logger = LoggerFactory.getLogger(MongodbBaseObjectStorage.class);
	
	@Inject
	private MongoDatabase mdb;
	
	private Object syncLocker = new Object();
	
	private ReentrantLock addLocker = new ReentrantLock();
	
	private ReentrantLock updateLocker = new ReentrantLock();
	
	private long lastAddTime = System.currentTimeMillis();
	
	private long lastUpdateTime = System.currentTimeMillis();
	
	private int maxCacheSize = 100;
	
	private long timeout = 1000*3;
	
	private Map<String,List<Document>> adds = new HashMap<>();
	
	private Map<String,List<Document>> updates = new HashMap<>();
	
	private Map<String,List<Document>> tempAdds = new HashMap<>();
	
	private Map<String,List<Document>> tempUpdates = new HashMap<>();
	
	private AtomicInteger addCnt = new AtomicInteger(0);
	
	private AtomicInteger updateCnt = new AtomicInteger(0);
	
	public void ready() {
		new Thread(this::doWork).start();
	}
	
	private void doWork() {
		while(true) {
			try {

				if(adds.isEmpty() && updates.isEmpty()) {
					synchronized(syncLocker) {
						try {
							syncLocker.wait(timeout);
						} catch (InterruptedException e) {
							logger.error("",e);
						}
					}
					continue;
				}
				
				long curTime = System.currentTimeMillis();
				if(!adds.isEmpty()) {
					if(addCnt.get() > this.maxCacheSize || curTime - lastAddTime > this.timeout) {
						
						boolean ls = addLocker.tryLock();
						if(ls) {
							try {
								addCnt.set(0);
								tempAdds.putAll(this.adds);
								this.adds.clear();
							}finally {
								if(ls) {
									addLocker.unlock();
								}
							}
							if(!tempAdds.isEmpty()) {
								for(Map.Entry<String, List<Document>> e : this.tempAdds.entrySet()) {
									doSave(e.getKey(),e.getValue());
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
									doUpdate(e.getKey(),e.getValue());
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


	private void doUpdate(String table,List<Document> batchUpdates) {
		long curTime = System.currentTimeMillis();
		MongoCollection<Document> coll = mdb.getCollection(table);
		for(Document d : batchUpdates) {
			updateOne(coll,d,curTime);
		}
	}
	
	private boolean updateOne(MongoCollection<Document> coll, Document d, long curTime) {
		Document filter = new Document();
		filter.put(ID, d.getInteger(ID));
		
		Document update = new Document();
		
		if(!d.containsKey(UPDATED_TIME)) {
			d.put(UPDATED_TIME, curTime);
		}
		
		if(!d.containsKey(CREATED_TIME)) {
			d.put(CREATED_TIME, curTime);
		}
		
		update.put("$set",  d);
		
		
		UpdateResult ur = coll.updateOne(filter,update,new UpdateOptions().upsert(true));
		return ur.getModifiedCount() != 0;
	}

	private void doSave(String table,List<Document> batchAdds) {
		mdb.getCollection(table).insertMany(batchAdds);
	}
	
	@Override
	public <T> boolean save(String table, Set<T> val,boolean async) {
		Object[] arr = new Object[val.size()];
		val.toArray(arr);
		return this.save(table, arr, async);
	}

	@Override
	public <T> boolean save(String table, T val,boolean async) {
		if(val == null) {
			return false;
		}
		
		Document d = null;
		if(val instanceof Document) {
			d = (Document)val;
		} else {
			d = Document.parse(JsonUtils.getIns().toJson(val));
		}
		
		if(!d.containsKey(UPDATED_TIME)) {
			d.put(UPDATED_TIME, System.currentTimeMillis());
		}
		
		if(!d.containsKey(CREATED_TIME)) {
			d.put(CREATED_TIME, System.currentTimeMillis());
		}
		
		if(async) {
			boolean ls = addLocker.tryLock();
			if(ls) {
				try {
					if(!this.adds.containsKey(table)) {
						this.adds.put(table, new ArrayList<>());
					}
					addCnt.incrementAndGet();
					this.adds.get(table).add(d);
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
			mdb.getCollection(table).insertOne(d);
		}
		
		return true;
	}

	@Override
	public <T> boolean save(String table, T[] vals,boolean async) {
		if(vals == null || vals.length == 0) {
			return false;
		}
		long curTime = System.currentTimeMillis();
		List<Document> llDocs = new ArrayList<>();
		for(Object v :vals) {
			Document d = null;
			if(v instanceof Document) {
				d = (Document)v;
			}else {
				d = Document.parse(JsonUtils.getIns().toJson(v));
			}
			if(!d.containsKey(CREATED_TIME)) {
				d.put(CREATED_TIME, curTime);
			}
			
			llDocs.add(d);
		}
		if(async) {
			boolean ls = addLocker.tryLock();
			if(ls) {
				try {
					addCnt.addAndGet(llDocs.size());
					if(!this.adds.containsKey(table)) {
						this.adds.put(table, new ArrayList<>());
					}
					this.adds.get(table).addAll(llDocs);
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
			mdb.getCollection(table).insertMany(llDocs);
		}
		
		return true;
	}

	@Override
	public boolean update(String table, Object id, Object val,boolean async) {
		
		
		Document d = null;
		if(val instanceof Document) {
			d = (Document)val;
		}else {
			 d = Document.parse(JsonUtils.getIns().toJson(val));
		}
		
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
			return updateOne(coll,d,System.currentTimeMillis());
		}
	}

	@Override
	public boolean deleteById(String table, Object id) {
		MongoCollection<Document> coll = mdb.getCollection(table);
		Document filter = new Document();
		filter.put(ID, id);
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
	public <T> List<T> query(String table,Map<String, Object> queryConditions,int offset,int pageSize) {
		MongoCollection<Document> coll = mdb.getCollection(table);
		Document filter = new Document();
		
		return null;
	}

	@Override
	public boolean updateOrSave(String table, Object id, Object val,boolean async) {
		if(!update(table,id,val,async)) {
			return save(table,val,async);
		}
		return true;
	}

	@Override
	public boolean update(String table, Object filter, Object updater) {
		
		Document up = null;
		if(updater instanceof Document) {
			up = (Document)updater;
		} else {
			 up = new Document();
			 Document up0 = Document.parse(JsonUtils.getIns().toJson(updater));
			 if(!up0.containsKey(UPDATED_TIME)) {
				 up0.put(UPDATED_TIME, System.currentTimeMillis());
			 }
			 if(!up0.containsKey(CREATED_TIME)) {
				 up0.put(CREATED_TIME, System.currentTimeMillis());
			 }
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
		UpdateResult ur = coll.updateOne(fi,up,new UpdateOptions().upsert(true));
		return ur.getModifiedCount() != 0;
	}

	
}
