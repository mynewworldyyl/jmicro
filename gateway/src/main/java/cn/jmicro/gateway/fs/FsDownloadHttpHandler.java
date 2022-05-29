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
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.gateway.img.ImgManager;
import lombok.extern.slf4j.Slf4j;

@Component("fsd")
@Slf4j
public class FsDownloadHttpHandler implements IHttpRequestHandler{

	@Inject(required=false)
	private GridFS fs;
	
	@Inject
	private ImgManager imgMng;
	
	/**
	 * logo@400x400.png
	 */
	@Override
	public void handle(HttpRequest req, HttpResponse resp) {
		String uri = req.getUri();
		String[] ps = uri.split("/");
		String n = ps[ps.length-1];
		
		String subfix = null;
		int idx = n.lastIndexOf(".");
		if(idx > 0) {
			n = n.substring(0,idx);
			subfix = ps[ps.length-1].substring(idx+1);
		}
		
		//Long fid = Long.parseLong(n);//最后一个是文件ID
		DBObject q = new BasicDBObject();
		q.put(IObjectStorage._ID, n);
		
		GridFSDBFile file = fs.findOne(q);
		if(file == null) {
			boolean suc = this.imgMng.getFile(n, subfix, resp);
			if(suc) {
				return;
			}
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
		if(file != null) {
			resp.setHeader("Content-Type",file.getContentType());
			resp.write(file.getInputStream(), (int)file.getLength());
		} else {
			resp.setHeader("Content-Type",Constants.HTTP_JSON_CONTENT_TYPE);
			resp.write("{'code':'1','msg':'"+uri+"'}");
		}
		
	}

	@Override
	public boolean match(HttpRequest req) {
		String uri = req.getUri();
		return !Utils.isEmpty(uri) && uri.startsWith(Constants.HTTP_fsContext);
	}

	
}
