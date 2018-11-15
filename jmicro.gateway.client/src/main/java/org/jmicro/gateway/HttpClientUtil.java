package org.jmicro.gateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.jmicro.common.util.JsonUtils;
import org.jmicro.common.util.StringUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月16日 上午12:22:50
 *
 */
public class HttpClientUtil {

    private static final Log LOG = LogFactory.getLog(HttpClientUtil.class);

    @SuppressWarnings("deprecation")
    private static String doGetRequest(HttpClient httpclient, String md5Sign,
            String url) throws IOException {
        HttpGet httpget = new HttpGet(url);

        httpget.setHeader("Connection", "close");
        httpget.setHeader("sign", md5Sign);
        HttpResponse response = null;
        try {
            response = httpclient.execute(httpget);
        } catch (Exception e) {
            httpclient.getConnectionManager().closeExpiredConnections();
            httpclient.getConnectionManager().closeIdleConnections(0,
                    TimeUnit.SECONDS);
            response = httpclient.execute(httpget);
        }
        HttpEntity entity = response.getEntity();
        String result = null;
        if (entity != null) {
            result = toString(entity.getContent(), HTTP.UTF_8);
            entity.consumeContent();
        }
        return result;
    }

    public static String doGetRequest(String url, Map<String, String> params)
            throws IOException {
        Map<String, String> treeMap = params == null
                ? new TreeMap<String, String>()
                : new TreeMap<String, String>(params);
        StringBuilder entryptString = new StringBuilder("");
        StringBuilder urlString = new StringBuilder(url);
        // 拼接所有的参数
        if (params != null && !params.isEmpty() ) {
            urlString.append("?");
        }

        for (Entry<String, String> entry : treeMap.entrySet()) {
            String str = entry.getKey() + "=" + entry.getValue();
            entryptString.append(str);
            urlString.append(str).append("&");
        }

        if (urlString.toString().endsWith("&")) {
            urlString.deleteCharAt(urlString.length() - 1);
        }

        // 拼接系统密钥
        //entryptString.append(Constant.EMNP_ENCRYPT_KEY);

        // MD5加密

        /*String md5Sign = DigestUtils
                .md5DigestAsHex(entryptString.toString().getBytes("UTF-8"));*/
        
        HttpClient httpclient = HttpConnectionManager.getHttpClient();
        return doGetRequest(httpclient, null, urlString.toString());
    }

    /**
     * 发送PUT方式的远程请求
     * 
     * @param url
     *            请求地址
     * @param params
     *            参数
     * @return 返回处理结果
     * @throws UnsupportedEncodingException
     */
    @SuppressWarnings("deprecation")
    public static String doPutRequest(String url, Map<String, String> params)
            throws UnsupportedEncodingException {
        HttpPut httpPut = new HttpPut(url);
        List<NameValuePair> nvps = setNameValuePair(params);
        httpPut.setHeader("Connection", "close");

        Map<String, String> treeMap = params == null
                ? new TreeMap<String, String>()
                : new TreeMap<String, String>(params);
        StringBuilder entryptString = new StringBuilder("");
        for (Entry<String, String> entry : treeMap.entrySet()) {
            entryptString.append(entry.getKey() + "=" + entry.getValue());
        }
        
        httpPut.setHeader("sign", null);

        HttpResponse response = null;
        String result = null;
        HttpClient httpclient = HttpConnectionManager.getHttpClient();
        try {
            httpPut.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            response = httpclient.execute(httpPut);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                result = toString(entity.getContent(), HTTP.UTF_8);
                entity.consumeContent();
            }
        } catch (Exception e) {
            httpclient.getConnectionManager().closeExpiredConnections();
            httpclient.getConnectionManager().closeIdleConnections(0,
                    TimeUnit.SECONDS);
        }

        return result;
    }

    @SuppressWarnings({ "deprecation" })
    public static String doPostRequest(String url, Map<String, String> param) {
        HttpPost httpost = new HttpPost(url);
        List<NameValuePair> nvps = setNameValuePair(param);
        try {
            HttpClient httpclient = HttpConnectionManager.getHttpClient();
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            httpost.setHeader("Connection", "close");

            Map<String, String> treeMap = param == null
                    ? new TreeMap<String, String>()
                    : new TreeMap<String, String>(param);
            StringBuilder entryptString = new StringBuilder("");
            for (Entry<String, String> entry : treeMap.entrySet()) {
                entryptString.append(entry.getKey() + "="
                        + (StringUtils.isBlank(entry.getValue()) ? ""
                                : entry.getValue()));
            }

            HttpResponse response = null;

            try {
                response = httpclient.execute(httpost);
            } catch (Exception e) {
                httpclient.getConnectionManager().closeExpiredConnections();
                httpclient.getConnectionManager().closeIdleConnections(0,
                        TimeUnit.SECONDS);
                response = httpclient.execute(httpost);
            }

            HttpEntity entity = response.getEntity();
            String result = null;
            if (entity != null) {
                result = toString(entity.getContent(), HTTP.UTF_8);
                entity.consumeContent();
            }
            return result;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return "";
    }

    @SuppressWarnings("deprecation")
    public static String readResponse(final HttpEntity httpEntity)
            throws Exception {
        if (httpEntity != null) {
            InputStreamReader inputStreamReader = null;
            inputStreamReader = new InputStreamReader(httpEntity.getContent());
            BufferedReader bufferedReader = new BufferedReader(
                    inputStreamReader);
            StringBuffer sb = new StringBuffer();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(new String(line.getBytes(),
                        Charset.forName(HTTP.UTF_8)));
            }
            if (inputStreamReader != null) {
                inputStreamReader.close();
            }
            return sb.toString();
        }
        return "";
    }

    @SuppressWarnings("rawtypes")
    private static List<NameValuePair> setNameValuePair(Map param) {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        if (param == null) {
            return nvps;
        }

        Iterator entries = param.entrySet().iterator();
        Map.Entry entry;
        while (entries.hasNext()) {
            entry = (Map.Entry) entries.next();
            String name = (String) entry.getKey();
            Object valueObj = entry.getValue();
            if (null == valueObj) {
                nvps.add(new BasicNameValuePair(name, ""));
            } else if (valueObj instanceof String[]) {
                String[] values = (String[]) valueObj;
                for (int i = 0; i < values.length; i++) {
                    nvps.add(new BasicNameValuePair(name, values[i]));
                }
            } else {
                nvps.add(new BasicNameValuePair(name, valueObj.toString()));
            }
        }

        return nvps;
    }

    private static String toString(InputStream in, String encode) {
        StringBuffer result = new StringBuffer();
        try {
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(in, encode));
            String tempLine = rd.readLine();
            while (tempLine != null) {
                result.append(tempLine);
                tempLine = rd.readLine();
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return result.toString();
    }

   

    @SuppressWarnings({ "deprecation" })
    public static String doPostRequest(String url, String md5Sign,
            Map<String, String> param) {
        HttpPost httpost = new HttpPost(url);
        List<NameValuePair> nvps = setNameValuePair(param);
        try {
            HttpClient httpclient = HttpConnectionManager.getHttpClient();
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            httpost.setHeader("Connection", "close");

            httpost.setHeader("sign", md5Sign);

            HttpResponse response = null;

            try {
                response = httpclient.execute(httpost);
            } catch (Exception e) {
                httpclient.getConnectionManager().closeExpiredConnections();
                httpclient.getConnectionManager().closeIdleConnections(0,
                        TimeUnit.SECONDS);
                response = httpclient.execute(httpost);
            }

            HttpEntity entity = response.getEntity();
            String result = null;
            if (entity != null) {
                result = toString(entity.getContent(), HTTP.UTF_8);
                entity.consumeContent();
            }
            return result;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return "";
    }

    @SuppressWarnings("deprecation")
    public static String sendJsonContentType(String url, String json) {
        HttpResponse httpResp = null;
        try {
            HttpParams params = new BasicHttpParams();
            HttpClient client = new DefaultHttpClient(params);
            HttpPost post = new HttpPost(url);

            // StringEntity entity = new StringEntity(json,"utf-8");//解决中文乱码问题
            // entity.setContentEncoding("UTF-8");
            // entity.setContentType("application/json");
            // post.setEntity(entity);

            StringBuilder sb = new StringBuilder(json);

            StringEntity entity = new StringEntity(json, "utf-8");// 解决中文乱码问题
            entity.setContentEncoding("UTF-8");
            entity.setContentType("application/json");
            post.setEntity(entity);

            try {
                httpResp = client.execute(post);
            } catch (Exception e) {
                client.getConnectionManager().closeExpiredConnections();
                client.getConnectionManager().closeIdleConnections(0,
                        TimeUnit.SECONDS);
                httpResp = client.execute(post);
            }

            HttpEntity respEntity = httpResp.getEntity();
            String result = null;
            if (respEntity != null) {
                result = toString(respEntity.getContent(), HTTP.UTF_8);
                respEntity.consumeContent();
            }
            return result;
        } catch (Exception e1) {
            e1.printStackTrace();
            return "";
        }
    }

    public static InputStream getUrlInputStream(String strUrl)
            throws IOException {
        InputStream is = null;
        URL url = null;
        url = new URL(strUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();// 利用HttpURLConnection对象,我们可以从网络中获取网页数据.
        conn.setDoInput(true);
        conn.connect();
        is = conn.getInputStream(); // 得到网络返回的输入流
        return is;
    }

}
