package cn.jmicro.api.security;

import java.util.List;

import cn.jmicro.api.Resp;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface ISecretService {
	
	Resp<String> getPublicKeyByInstance(String instancePrefix);
	
	Resp<List<JmicroPublicKey>> publicKeysList();
	
	Resp<JmicroPublicKey> createSecret(String instancePrefix,String password);
	
	Resp<Boolean> deletePublicKey(Long id);
	
	Resp<Boolean> updateInstancePrefix(Long id,String instancePrefix);
	
	Resp<JmicroPublicKey> addPublicKeyForInstancePrefix(String instancePrefix, String publicKey);
	
	Resp<Boolean> enablePublicKey(Long id,boolean enStatus);
}
