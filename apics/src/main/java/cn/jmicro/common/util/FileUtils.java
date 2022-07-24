package cn.jmicro.common.util;

import cn.jmicro.common.Utils;

public class FileUtils {

	public static final String getFileExt(String fileName) {
		if(Utils.isEmpty(fileName)) return "";
		if(!fileName.contains(".")) return "";
		return fileName.substring(fileName.indexOf(".")+1);
	}
}
