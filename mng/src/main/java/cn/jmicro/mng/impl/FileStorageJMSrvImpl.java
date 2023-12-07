package cn.jmicro.mng.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.storage.FileJRso;
import cn.jmicro.api.storage.IFileStorage;
import cn.jmicro.api.storage.IProgress;
import lombok.extern.slf4j.Slf4j;

@Component
@Service(version="0.0.1",external=false,debugMode=1,showFront=false,logLevel=MC.LOG_NO,namespace="fileServer")
@Slf4j
public class FileStorageJMSrvImpl implements IFileStorage {

	@Inject(required=false)
	private GridFS fs;
	
	@Inject
	private IObjectStorage os;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Override
	public void save(FileJRso file, IProgress p) {

		GridFSInputFile ff = this.fs.createFile();

		OutputStream fos = ff.getOutputStream();

		FileInputStream fis = null;
		try {
			
			os.save(FileJRso.TABLE, file, FileJRso.class, false);
			
			ff.setChunkSize(file.getSize());
			ff.setContentType(file.getType());
			ff.setFilename(file.getName());
			ff.setId(file.getId());

			DBObject mt = new BasicDBObject();
			if(file.getAttr() != null) {
				mt.putAll(file.getAttr());
			}

			mt.put("createdBy", file.getCreatedBy());
			mt.put("clientId", file.getClientId());
			ff.setMetaData(mt);
			
			fis = new FileInputStream(file.getLocalPath());
			int bs = 2048;
			byte[] d = new byte[bs];
			int s = 0;
			while((s = fis.read(d, 0, bs)) != -1) {
				if(s > 0) fos.write(d, 0, s);
			}
			
		} catch (IOException e) {
			log.error(file.getLocalPath(),e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					log.error("close: "+file.getLocalPath(),e);
				}
			}
			File dfile = new File(file.getLocalPath());
			dfile.delete();
		}
	}

}
