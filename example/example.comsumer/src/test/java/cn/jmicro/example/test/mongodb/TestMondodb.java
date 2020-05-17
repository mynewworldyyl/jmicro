package cn.jmicro.example.test.mongodb;

import org.bson.Document;
import org.junit.Test;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import cn.jmicro.test.JMicroBaseTestCase;

public class TestMondodb extends JMicroBaseTestCase{

	@Test
	public void testSave() {
		MongoDatabase m = of.get(MongoDatabase.class);
		MongoCollection<Document> coll = m.getCollection("testcoll");
		Document doc = new Document();
		doc.put("key1", "23");
		coll.insertOne(doc);
	}
	
}
