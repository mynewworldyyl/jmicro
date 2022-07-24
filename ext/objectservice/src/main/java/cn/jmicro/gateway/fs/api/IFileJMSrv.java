package cn.jmicro.gateway.fs.api;

import java.util.List;
import java.util.Map;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.storage.FileJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IFileJMSrv {

	IPromise<RespJRso<List<FileJRso>>> getFileList(Map<String,Object> qry,int pageSize,int curPage);
	
	IPromise<RespJRso<FileJRso>> addFile(FileJRso pr);
	
	IPromise<RespJRso<FileJRso>> getFile(String resId);
	
	//IPromise<RespJRso<FileJRso>> updateFile(FileJRso pr,boolean updateFile);
	
	IPromise<RespJRso<Boolean>> addFileData(String id, byte[] data, int blockNum);
	
	IPromise<RespJRso<Boolean>> deleteFile(String id);
	
	IPromise<RespJRso<byte[]>> downFileData(int downloadId, int blockNum);
	
	IPromise<RespJRso<Integer>> initDownloadFile(int actId,String resId);
	
	IPromise<RespJRso<Map<String,Object>>> queryDict();
	
	IPromise<RespJRso<String>> save2Db(FileJRso pr);
	
}
