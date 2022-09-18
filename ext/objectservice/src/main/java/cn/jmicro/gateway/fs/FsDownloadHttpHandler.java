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

@Component("fsd")
//@Slf4j
public class FsDownloadHttpHandler implements IHttpRequestHandler{

	@Inject(required=false)
	private GridFS fs;
	
	@Inject
	private ImgManager imgMng;
	
	/**
	 * logo@400x400.png
	 */
	@Override
	public boolean handle(HttpRequest req, HttpResponse resp) {
		String uri = req.getUri();
		String[] ps = uri.split("/");
		String n = ps[ps.length-1];
		return downloadFile(req,resp,n,false);
	}
	
	public boolean downloadFile(HttpRequest req, HttpResponse resp, String fileId, boolean deleteFile) {
		String uri = req.getUri();
		// Long fid = Long.parseLong(n);//最后一个是文件ID
		DBObject q = new BasicDBObject();
		q.put(IObjectStorage._ID, fileId);

		GridFSDBFile file = fs.findOne(q);
		if (file == null) {
			boolean suc = this.imgMng.getFile(fileId, resp);
			if (suc) {
				return true;
			}
		}

		/*
		 * InputStreamReader r = new InputStreamReader(file.getInputStream());
		 * BufferedReader br = new BufferedReader(r);
		 * 
		 * StringBuffer sb = new StringBuffer(); String str = null; try { while(null !=
		 * (str = br.readLine())) { sb.append(str); } } catch (IOException e) {}
		 * 
		 * log.info(sb.toString());
		 */

		if (file != null) {
			resp.setHeader("Content-Type", file.getContentType());
			resp.write(file.getInputStream(), (int) file.getLength());
			if(deleteFile) {
				fs.remove(q);
				
			}
		} else {
			resp.setHeader("Content-Type", Constants.HTTP_JSON_CONTENT_TYPE);
			resp.write("{'code':'1','msg':'" + uri + "'}");
		}
		return true;
	}

	@Override
	public boolean match(HttpRequest req) {
		String uri = req.getUri();
		return !Utils.isEmpty(uri) && uri.startsWith(Constants.HTTP_fsContext);
	}

}
