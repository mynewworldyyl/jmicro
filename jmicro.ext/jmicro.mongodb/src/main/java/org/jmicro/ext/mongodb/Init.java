package org.jmicro.ext.mongodb;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.config.Config;
import org.jmicro.api.objectfactory.IPostFactoryListener;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

@Component(lazy=false, level=99)
public class Init implements IPostFactoryListener{
	
	private final static Logger logger = LoggerFactory.getLogger(Init.class);
	
	//private MapperProxy mapperProxy = new MapperProxy();
	
	private IObjectFactory of;
	
	public void init() {
	}

	@Override
	public void preInit(IObjectFactory of) {
		this.of = of;
		Config cfg = of.get(Config.class);
		String  host = cfg.getString("/mongodb/host", "localhost");
		int  port = cfg.getInt("/mongodb/port", 27017);
		
		String  username = cfg.getString("/mongodb/username", "");
		String  password = cfg.getString("/mongodb/password", null);
		String  dbname = cfg.getString("/mongodb/dbname", "jmicrodb");
		
		//数据库链接选项        
        MongoClientOptions mongoClientOptions = MongoClientOptions.builder().build();
 
        //数据库链接方式
        /** MongoDB 3.0以下版本使用该方法 */
        /*MongoCredential mongoCredential = MongoCredential.createMongoCRCredential("name", "dbname", "password".toCharArray());*/
 
        /** MongoDB 3.0及其以上版本使用该方法 */
        //MongoCredential mongoCredential = MongoCredential.createScramSha1Credential(username, dbname,password!=null?password.toCharArray():null);
        MongoCredential mongoCredential = null;
        if(StringUtils.isNotEmpty(username)) {
        	mongoCredential =  MongoCredential.createScramSha1Credential(username, dbname,password!=null?password.toCharArray():null);
        }else {
        	mongoCredential = MongoCredential.createMongoX509Credential();
        }
        //(username, dbname,password!=null?password.toCharArray():null);
        
        //数据库链接地址
        ServerAddress serverAddress = new ServerAddress(host, port);
 
        //获取数据库链接client
        MongoClient client = new MongoClient(serverAddress,mongoClientOptions);
 
        //获取数据库对象
        MongoDatabase mdb = client.getDatabase(dbname);
        
        DB db = client.getDB(dbname);

        of.regist(MongoClient.class, client);
        of.regist(MongoDatabase.class, mdb);
        of.regist(DB.class, db);
        
        logger.info("Mongodb connected successfully!");
		
	}

	@Override
	public void afterInit(IObjectFactory of) {
	}

	@Override
	public int runLevel() {
		return 0;
	}
	
	
}
