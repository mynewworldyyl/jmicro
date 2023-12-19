package cn.jmicro.ext.dataservice.api;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.codegenerator.AsyncClientProxy;


/**
 * 人脸类类服务
 * 对IDataApiJMSrv进一步封装
 * 
 * @author Yulei Ye
 */
@AsyncClientProxy
public interface IFaceService{
	
	/**
	 * 人脸图片质量检测
	 * @param fileId 文件ID
	 * @return
	 */
	IPromise<RespJRso<String>> faceDetect(String fileId);
	
	/**
	 * 将WAV编码语音转为文字
	 * @param speech  WAV数据的BASE64字符数据
	 * @return
	 */
	IPromise<RespJRso<String>> voiceToText(String speech);
	
	/**
	 * AI对话
	 * @param msg
	 * @return
	 */
	IPromise<RespJRso<String>> aiChat(String msg);
	
	/**
	 * 整合voiceToText和aiChat两个功能并返回结果
	 * @param fileId 语音文件ID
	 * @return
	 */
	IPromise<RespJRso<String>> voidAiChat(String fileId);
	
	
	/**
	 * 
	 * @param fileId
	 * @return
	 */
	IPromise<RespJRso<String>> hanzi2Pinyin(String hanzi);
	
	//IPromise<RespJRso<String>> testAiService(String msg);
	
}
