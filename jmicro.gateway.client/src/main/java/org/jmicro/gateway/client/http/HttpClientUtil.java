package org.jmicro.gateway.client.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月16日 上午12:22:50
 *
 */
public class HttpClientUtil {

	private static final Log LOG = LogFactory.getLog(HttpClientUtil.class);

	public static byte[] doPostData(String url, ByteBuffer data,Map<String,String> headers){
			byte[] result = new byte[0];

			// 创建httpclient对象
			CloseableHttpClient client = HttpClients.createDefault();
			// 创建post方式请求对象
			HttpPost httpPost = new HttpPost(url);

			// 设置参数到请求对象中
			byte[] byteData= new byte[data.remaining()];
			data.get(byteData, 0, byteData.length);
			httpPost.setEntity(new ByteArrayEntity(byteData,0,byteData.length));

			// 设置header信息
			// 指定报文头【Content-type】、【User-Agent】
			//httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
			//httpPost.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

			headers.forEach((key,value)->{
				httpPost.setHeader(key, value);
			});
			
			try {
				// 执行请求操作，并拿到结果（同步阻塞）
				CloseableHttpResponse response = client.execute(httpPost);
				// 获取结果实体
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					// 按指定编码转换结果实体为String类型
					result = EntityUtils.toByteArray(entity);
				}
				EntityUtils.consume(entity);
				// 释放链接
				response.close();
				
			} catch (IOException e) {
				LOG.error("",e);
			}
			return result;
		}
}
