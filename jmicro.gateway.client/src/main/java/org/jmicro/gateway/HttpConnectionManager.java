package org.jmicro.gateway;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpVersion;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

public class HttpConnectionManager {  
    private static HttpParams httpParams;  
    
    private static ClientConnectionManager connectionManager;  
    /** 
     * 最大连接数 
     */  
    public final static int MAX_TOTAL_CONNECTIONS = 1500;  

    /** 
     * 获取连接的最大等待时间 
     */  
    public final static int WAIT_TIMEOUT = 5000;  

    /** 
     * 每个路由最大连接数 
     */  
    public final static int MAX_ROUTE_CONNECTIONS = 400;  

    /** 
     * 连接超时时间 
     */  
    public final static int CONNECT_TIMEOUT = 30000;  

    /** 
     * 读取超时时间 
     */  
    public final static int READ_TIMEOUT = 30000;  
    
    
    private HttpConnectionManager() {
    	
    }

    static{
            httpParams = new BasicHttpParams();
            // 设置一些基本参数
            HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);
            HttpProtocolParams.setUseExpectContinue(httpParams, true);
            HttpProtocolParams.setUserAgent(httpParams,
                "Mozilla/5.0(Linux;U;Android 2.2.1;en-us;Nexus One Build.FRG83) "
                       + "AppleWebKit/553.1(KHTML,like Gecko) Version/4.0 Mobile Safari/533.1");
            
            // 设置最大连接数  
            ConnManagerParams.setMaxTotalConnections(httpParams, MAX_TOTAL_CONNECTIONS);  

            // 设置获取连接的最大等待时间  
            ConnManagerParams.setTimeout(httpParams, WAIT_TIMEOUT);  

            // 设置每个路由最大连接数  
            ConnPerRouteBean connPerRoute = new ConnPerRouteBean(MAX_ROUTE_CONNECTIONS);  

            ConnManagerParams.setMaxConnectionsPerRoute(httpParams,connPerRoute);  

            // 设置连接超时时间  
            HttpConnectionParams.setConnectionTimeout(httpParams, CONNECT_TIMEOUT);  

            // 设置读取超时时间  
            HttpConnectionParams.setSoTimeout(httpParams, READ_TIMEOUT);  
            
            // 设置HttpClient支持HTTP和HTTPS两种模式
            SchemeRegistry schReg = new SchemeRegistry();
            schReg.register(new Scheme("http", PlainSocketFactory .getSocketFactory(), 80));
            schReg.register(new Scheme("https", SSLSocketFactory .getSocketFactory(), 443));

            // 使用线程安全的连接管理来创建HttpClient
            connectionManager = new ThreadSafeClientConnManager(httpParams, schReg);
    }
    
    private static DefaultHttpClient httpClient;
    
    public synchronized static  DefaultHttpClient getHttpClient() {  
        DefaultHttpClient hc = null;
        try {
            hc = new DefaultHttpClient();
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());  
            //"111111"为制作证书时的密码  
            //如果安装了server.cer这个证书的话，那么下面这句代码不要也可以访问。
          /*  trustStore.load(new FileInputStream(new File(Constant.CertificateURL)), 
            		Constant.CertificatePass.toCharArray());*/
            SSLSocketFactory socketFactory = new SSLSocketFactory(trustStore);  
            //不校验域名  
            socketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);  
            //这个8446是和被访问端约定的端口，一般为443  
            Scheme sch = new Scheme("https", socketFactory, 8443);  
            hc.getConnectionManager().getSchemeRegistry().register(sch);
            
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hc;
    }  
    @Deprecated
    public synchronized static  DefaultHttpClient getHTTPSClientValidate() {  
        DefaultHttpClient hc = null;
        try {
            hc = new DefaultHttpClient();
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());  
            //"111111"为制作证书时的密码  
            //如果安装了server.cer这个证书的话，那么下面这句代码不要也可以访问。
           /* trustStore.load(new FileInputStream(new File(Constant.CertificateURL)), 
            		Constant.CertificatePass.toCharArray());*/
            SSLSocketFactory socketFactory = new SSLSocketFactory(trustStore);  
            //不校验域名  
            socketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);  
            //这个8446是和被访问端约定的端口，一般为443  
            Scheme sch = new Scheme("https", socketFactory, 8443);  
            hc.getConnectionManager().getSchemeRegistry().register(sch);
            
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hc;
    }  
    
    
    public synchronized static  DefaultHttpClient getHTTPSClient() {  
        DefaultHttpClient hc = null;
        try {
            hc = new DefaultHttpClient();
            //KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());  
            //"111111"为制作证书时的密码  
            //如果安装了server.cer这个证书的话，那么下面这句代码不要也可以访问。
            //trustStore.load(new FileInputStream(new File("E:\\server.keystore")), Constant.CertificatePass.toCharArray());
            //SSLSocketFactory socketFactory = new SSLSocketFactory(trustStore);  
            //不校验域名  
            //socketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);  
            //这个8446是和被访问端约定的端口，一般为443  
            //Scheme sch = new Scheme("https", socketFactory,443);  
            //hc.getConnectionManager().getSchemeRegistry().register(sch);
            
            //Secure Protocol implementation.    
            SSLContext ctx = SSLContext.getInstance("SSL");  
            //Implementation of a trust manager for X509 certificates    
            X509TrustManager tm = new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    // TODO Auto-generated method stub
                    return null;
                }
                
                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
                        String authType) throws CertificateException {
                    // TODO Auto-generated method stub
                    
                }
                
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
                        String authType) throws CertificateException {
                    // TODO Auto-generated method stub
                    
                }
            };
            ctx.init(null, new TrustManager[] { tm }, null);  
            SSLSocketFactory ssf = new SSLSocketFactory(ctx);  
            ClientConnectionManager ccm = hc.getConnectionManager();  
            //register https protocol in httpclient's scheme registry    
            SchemeRegistry sr = ccm.getSchemeRegistry();  
            sr.register(new Scheme("https", 443, ssf));
            
            
            
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hc;
    }  
}
