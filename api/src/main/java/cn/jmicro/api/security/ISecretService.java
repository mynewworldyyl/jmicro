package cn.jmicro.api.security;

import java.util.List;

import cn.jmicro.api.Resp;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface ISecretService {
	
	/**
	 * 根据实例前缀拿取公钥
	 * @param instancePrefix  服务前缀，不同的前缀有不同的公钥，相同前缀只能一个公钥启用
	 * @return
	 */
	Resp<String> getPublicKeyByInstance(String instancePrefix);
	
	/**
	 * 我的公钥列表，只能拿取当前账号下的公钥列表
	 * @return
	 */
	Resp<List<JmicroPublicKey>> publicKeysList();
	
	/**
	 * 创建一个公私钥，同时可选给私钥加密保护
	 * 如果创建成功，返回公钥和私给调用者，服务端只保留公钥，不存储私钥，所以创建者一定要保留好私钥及私钥密码，私钥一旦丢失，
	 * 将无法找回，只能重新生成。
	 * @param instancePrefix 服务前缀
	 * @param password 私钥密码
	 * @return
	 */
	Resp<JmicroPublicKey> createSecret(String instancePrefix,String password);
	
	/**
	 * 删除未启用的公钥，删除后将无法恢复
	 * 启用中的公钥不能删除
	 * @param id
	 * @return
	 */
	Resp<Boolean> deletePublicKey(Long id);
	
	/**
	 * 更新公钥前缀
	 * 启用中的公钥不能更新
	 * @param id
	 * @param instancePrefix
	 * @return
	 */
	Resp<Boolean> updateInstancePrefix(Long id,String instancePrefix);
	
	/**
	 * 增加一个线下生成的公钥到系统中
	 * @param instancePrefix
	 * @param publicKey
	 * @return
	 */
	Resp<JmicroPublicKey> addPublicKeyForInstancePrefix(String instancePrefix, String publicKey);
	
	/**
	 * 启用公钥，公钥启用后，其他系统就可以根据前缀取得公钥，从而可以与前缀所对应的系统做安全通信
	 * 同一个前缀同一时刻只能有一个公钥被启用
	 * @param id
	 * @param enStatus
	 * @return
	 */
	Resp<Boolean> enablePublicKey(Long id,boolean enStatus);
}
