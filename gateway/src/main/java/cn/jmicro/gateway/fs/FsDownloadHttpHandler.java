package cn.jmicro.gateway.fs;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.http.HttpRequest;
import cn.jmicro.api.http.HttpResponse;
import cn.jmicro.api.http.IHttpRequestHandler;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.common.Utils;
import lombok.extern.slf4j.Slf4j;

@Component("fsd")
@Slf4j
public class FsDownloadHttpHandler implements IHttpRequestHandler{

	@Inject(required=false)
	private GridFS fs;
	
	@Override
	public void handler(HttpRequest req, HttpResponse resp) {
		String uri = req.getUri();
		String[] ps = uri.split("/");
		String n = ps[ps.length-1];
		
		int idx = n.lastIndexOf(".");
		if(idx > 0) {
			n = n.substring(0,idx);
		}
		
		Long fid = Long.parseLong(n);//最后一个是文件ID
		DBObject q = new BasicDBObject();
		q.put(IObjectStorage._ID, fid);
		
		GridFSDBFile file = fs.findOne(q);
		if(file == null) {
			log.warn("Not found: " + uri);
			return;
		}
		
		/*
		InputStreamReader r = new InputStreamReader(file.getInputStream());
		BufferedReader br = new BufferedReader(r);
		
		StringBuffer sb = new StringBuffer();
		String str = null;
		try {
			while(null != (str = br.readLine())) {
				sb.append(str);
			}
		} catch (IOException e) {}
		
		log.info(sb.toString());*/
		
		resp.setHeader("Content-Type",file.getContentType());
		resp.write(file.getInputStream(), (int)file.getLength());
	}

	@Override
	public boolean match(HttpRequest req) {
		String uri = req.getUri();
		return !Utils.isEmpty(uri) && uri.startsWith("/_fs_");
	}

	
}
