package cn.jmicro.api.storage;

import cn.jmicro.api.RespJRso;

public interface IProgress {

	void onStart(FileJRso file);
	
	void onPregress(FileJRso file,Integer percent);
	
	void onEnd(FileJRso file,RespJRso<?> resp);
	 
}
