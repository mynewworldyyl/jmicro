package org.myjmicro.uploadres;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.jmicro.api.Resp;
import cn.jmicro.choreography.api.PackageResource;
import cn.jmicro.choreography.api.genclient.IResourceResponsitory$Gateway$JMAsyncClient;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.gateway.client.ApiGatewayClient;
import cn.jmicro.gateway.client.ApiGatewayConfig;

public class UploadJmicroResource {

	public static void main(String[] args) {
		if(args == null || args.length == 0) {
			System.out.println("Have to specify resource directory");
			return;
		}
		
		Map<String,File> jarfiles = new HashMap<>();
		String resDir = args[0];
		if(Utils.isEmpty(resDir)) {
			System.out.println("Resource directory is NULL");
			return;
		}
		
		final String[] ats;
		if(args.length > 1 && !Utils.isEmpty(args[1])) {
			ats = args[1].split(",");
		} else {
			ats = new String[0];
		}
		
		findFile0(jarfiles,new File(resDir),ats);
		
		if(jarfiles.isEmpty()) {
			System.out.println("No resource file found in directory: " + resDir);
			return;
		}
		
		ApiGatewayClient.initClient(new ApiGatewayConfig(Constants.TYPE_SOCKET,"192.168.56.1","9092"));
		ApiGatewayClient socketClient =  ApiGatewayClient.getClient();
		
		IResourceResponsitory$Gateway$JMAsyncClient rr = 
				socketClient.getService(IResourceResponsitory$Gateway$JMAsyncClient.class,
						ApiGatewayClient.NS_REPONSITORY, "0.0.1");
		
		socketClient.loginJMAsync("jmicro", "0")
		.success((act,cxt) -> {
			System.out.println("Successfully login: " + act.getData().getActName());
			doUpload(jarfiles,rr,ats);
		})
		.fail((code,msg,cxt)->{
			System.out.println("Fail login msg:" +msg);
		});
	}
	
	private static void doUpload(Map<String, File> jarfiles,IResourceResponsitory$Gateway$JMAsyncClient rr,
			String[] artifactIds) {
		Map<String,Object> qry = new HashMap<>();
		qry.put("group", "cn.jmicro");
		//qry.put("status", PackageResource.STATUS_ENABLE);
		if(artifactIds != null && artifactIds.length > 0) {
			qry.put("artifactIds", StringUtils.join(artifactIds, ","));
		}
		
		Resp<List<PackageResource>> resp = rr.getResourceList(qry, Integer.MAX_VALUE, 1);
		if(resp.getData() == null || resp.getData().isEmpty()) {
			System.out.println("No package need upload!");
			return;
		}
		
		List<PackageResource> l = resp.getData();
		
		for(PackageResource res : l) {
			if(!jarfiles.containsKey(res.getName())) {
				System.out.println("Res file not found: " + res.getName());
				continue;
			}
			uploadOneFile(jarfiles.get(res.getName()),res,rr);
		}
		
	}

	private static void uploadOneFile(File file, PackageResource res,IResourceResponsitory$Gateway$JMAsyncClient rr) {
		res.setModifiedTime(file.lastModified());
		res.setSize(file.length());
		
		System.out.println("Update res: " + res.getName());
		Resp<PackageResource> resp  = rr.updateResource(res, true);
		if(resp.getCode() == 0) {
			PackageResource pr = resp.getData();
			int num = (int)(file.length() / pr.getBlockSize());
			
			System.out.println("Upload data: " + res.getName());
			
			FileInputStream fi = null;
			try {
				fi = new FileInputStream(file);
				int i = 0;
				while(i < num) {
					byte[] data = new byte[pr.getBlockSize()];
					int len = fi.read(data, 0, pr.getBlockSize());
					if(len > 0) {
						rr.addResourceData(pr.getId(), data, i);
					}
					i++;
				}
				
				int lastBlockSize = (int)(file.length() % pr.getBlockSize());
				if( lastBlockSize > 0) {
					byte[] data = new byte[lastBlockSize];
					int len = fi.read(data, 0, lastBlockSize);
					if(len > 0) {
						rr.addResourceData(pr.getId(), data, num+1);
					}
				}
				System.out.println("Upload finished: " + res.getName());
			}catch(Exception e) {
				e.printStackTrace();
			}finally {
				if(fi != null) {
					try {
						fi.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			System.out.println("Fail code: " +resp.getCode() + ", msg: " +resp.getMsg());
		}
	}

	private static void findFile0(Map<String,File> rst, File file,String[] filters) {
		if(file.isFile()) {
			String n = file.getName();
			if(file.getAbsolutePath().indexOf("target") > 1 && n.endsWith(".jar")) {
				if(filters != null && filters.length > 0) {
					for(String fl : filters) {
						if(n.contains(fl)) {
							rst.put(n, file);
							break;
						}
					}
				} else {
					rst.put(n, file);
				}
			}
		}else {
			File[] fs = file.listFiles((File dir, String name)->{
				if(name.equals("mng.web")) {
					return false;
				}
				File f = new File(dir,name);
				if(f.isDirectory()) {
					return true;
				} else {
					return name.endsWith(".jar");
				}
			});
			
			for(File f : fs) {
				//LOG.debug(f.getAbsolutePath());
				findFile0(rst,f,filters);
			}
		}
	}
}
