package cn.jmicro.gateway.img;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.http.HttpResponse;
import cn.jmicro.api.persist.IObjectStorage;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;

@Component
@Slf4j
public class ImgManager {
	
	//new String[]{"png","jpg","jpeg"}
	@Cfg("/ImgManager/imgSubfixs")
	private Set<String> imgSubfixs = new HashSet<>();
	{
		imgSubfixs.add("png");
		imgSubfixs.add("jpg");
		imgSubfixs.add("jpeg");
	}
	
	private Map<String,String> ct2Subfixes = new HashMap<>();
	{
		ct2Subfixes.put("image/jpeg", "jpeg");
		ct2Subfixes.put("image/jpg", "jpg");
		ct2Subfixes.put("image/png", "png");
		ct2Subfixes.put("image/gif", "gif");
	}
	
	@Inject(required=false)
	private GridFS fs;
	
	private String imgTempDir = null;
	
	public void jready() {
		imgTempDir = System.getProperty("user.dir")+"/imgTempDir/";
		File f = new File(imgTempDir);
		if(!f.exists()) {
			f.mkdir();
		}
	}

	/**
	 * 
	 * @param fn  100000@100x200  100000@100
	 * @param subfix jpg png jpeg
	 * @return
	 */
	public boolean getFile(String fn,HttpResponse resp) {
		if(!fn.contains("@")) {
			//只处理图片文件
			log.error(fn+" not support!");
			return false;
		}
		
		String subfix = null;
		int idx = fn.lastIndexOf(".");
		if(idx > 0) {
			subfix = fn.substring(idx+1);
			fn = fn.substring(0,idx);
		}
		
		String[] ar = fn.split("@");
		String srcFileId = ar[0]+"."+subfix;
		
		DBObject q = new BasicDBObject();
		q.put(IObjectStorage._ID, srcFileId);
		GridFSDBFile file = fs.findOne(q);
		if(file == null) {
			log.error(fn+" not found!");
			return false;
		}
		
		GridFSInputFile ff = this.fs.createFile();
		
		//ff.setChunkSize(zkrr.getSize());
		ff.setContentType(file.getContentType());
		ff.setId(fn+"."+subfix);
		ff.setFilename(ff.getId().toString());
		ff.setMetaData(file.getMetaData());
		
		OutputStream fos = ff.getOutputStream();
		File fp = null;
		try {
			fp = new File(this.imgTempDir+ff.getFilename());
			if(!fp.exists()) {
				fp.createNewFile();
			}
			
			String[] sizeAr = ar[1].split("[xX]");
			int w,h;
			h = w = Integer.parseInt(sizeAr[0]);
			if(sizeAr.length > 1) {
				h = Integer.parseInt(sizeAr[1]);
			}
			//做图片压缩转换
			 Thumbnails.of(file.getInputStream()).size(w, h).toFile(fp);
			 FileInputStream fis = null;
			 try {
				 fis = new FileInputStream(fp);
				 byte[] d = new byte[(int)fp.length()];
				 fis.read(d, 0, (int)fp.length());
				 fos.write(d, 0, d.length);
				 resp.setHeader("Content-Type",file.getContentType());
				 resp.write(d);
				 return true;
			 }finally {
				 if(fis != null) {
					 fis.close();
				 }
			 }
		} catch (IOException e) {
			log.error(ff.getFilename(),e);
			return false;
		}finally {
			if(fp != null) {
				fp.delete();
			}
			if(fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					log.error(ff.getFilename(),e);
				}
			}
		}
	}
}
