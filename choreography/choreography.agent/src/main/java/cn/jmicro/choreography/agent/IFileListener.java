package cn.jmicro.choreography.agent;

public interface IFileListener {

	void onEvent(int type,String fileName,String content);
	
}
