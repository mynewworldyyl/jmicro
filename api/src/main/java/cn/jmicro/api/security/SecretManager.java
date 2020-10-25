package cn.jmicro.api.security;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.ClassScannerUtils;
import cn.jmicro.api.IListener;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.exp.ExpUtils;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.rsa.EncryptUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;

@Component(level = 1)
public class SecretManager {

	private final static Logger logger = LoggerFactory.getLogger(SecretManager.class);

	private static final String PUBKEYS_PREFIX = "/META-INF/keys/";

	@Reference(namespace = "sec", version = "*", required = false)
	private ISecretService secretSrv;

	/*
	 * @Reference(namespace="sec", version="*",required=true) private
	 * IExchangeSecretService exchangeSrv;
	 */

	@Inject
	private IDataOperator op;

	@Inject
	private IRegistry registry;

	@Inject
	private ProcessInfo pi;

	// private String myPriStrKey;

	private RSAPrivateKey myPriKey;

	// private RSAPublicKey myPubKey;

	// private Map<String,String> publicStrKeys = new HashMap<>();

	private Map<String, RSAPublicKey> publicRsaKeys = new HashMap<>();

	private Map<Integer, String> processId2InstanceNames = new HashMap<>();

	private final Map<Integer, PBEKey> insId2AesKey = new HashMap<>();

	public String getPublicKey(String prefix) {
		if (!publicRsaKeys.containsKey(prefix)) {
			return null;
		}
		RSAPublicKey key = publicRsaKeys.get(prefix);
		return Base64.getEncoder().encodeToString(key.getEncoded());
	}

	public String exchange(int insId, byte[] secrect) {
		if (insId <= 0) {
			return "instance id invalid: " + insId;
		}

		if (secrect == null || secrect.length == 0) {
			return "secrect cannot be null for instance: " + insId;
		}

		String insName = this.getInstanceName(insId);
		if (Utils.isEmpty(insName)) {
			return "Instane name not found: " + insId;
		}
		SecretKey originalKey = new SecretKeySpec(secrect, 0, secrect.length, EncryptUtils.KEY_PBE);

		PBEKey k = new PBEKey(insName, insId, originalKey);

		insId2AesKey.put(insId, k);

		return null;
	}

	public void checkAndDecrypt(Message msg, boolean isUp) {

		try {

			if (msg.isRsaEnc() && (msg.isUpSsl() || msg.isDownSsl())) {

				byte[] data = null;
				if (!isUp && msg.isDownSsl() || isUp && msg.isUpSsl()) {
					// 上行包，服务端收到请求包,并且是非对称加密
					// 下行包，客户端收到响应包，并且是非对称加密
					if (myPriKey == null) {
						throw new CommonException("Private key is null when do rsa decrypt data!");
					}
					ByteBuffer bb = (ByteBuffer) msg.getPayload();
					data = EncryptUtils.decrypt(myPriKey, bb.array(), 0, bb.limit());
					msg.setPayload(ByteBuffer.wrap(data));
				}

				// 对于上行包，如果下行是安全的，则说明客户端有私钥，所以需要做签名
				if (msg.isSign()) {
					ByteBuffer bb = (ByteBuffer) msg.getPayload();
					RSAPublicKey pubKey = getPublicKey(msg.getInsId());
					if (!EncryptUtils.doCheck(bb.array(), 0, bb.limit(), msg.getSign(), pubKey)) {
						throw new CommonException("invalid sign");
					}
				}

				msg.setSign(null);
				msg.setSign(false);
			}

			if ((msg.isUpSsl() || msg.isDownSsl()) && !msg.isRsaEnc()) {
				// 对称解密
				if (msg.isSec()) {
					if (msg.getSec() == null || msg.getSec().length == 0) {
						throw new CommonException("Invalid sec data" + msg.getInsId());
					}
					byte[] secrect = EncryptUtils.decrypt(myPriKey, msg.getSec(), 0, msg.getSec().length);
					String insName = this.getInstanceName(msg.getInsId());
					if (Utils.isEmpty(insName)) {
						throw new CommonException("Instane name not found: " + msg.getInsId());
					}
					SecretKey originalKey = new SecretKeySpec(secrect, 0, secrect.length, EncryptUtils.KEY_PBE);
					PBEKey k = new PBEKey(insName, msg.getInsId(), originalKey);
					insId2AesKey.put(msg.getInsId(), k);
					msg.setSec(false);
					msg.setSec(null);
				}

				PBEKey k = this.insId2AesKey.get(msg.getInsId());
				if (k == null) {
					throw new CommonException("PBE key not found for: " + msg.getInsId());
				}
				ByteBuffer bb = (ByteBuffer) msg.getPayload();
				msg.setPayload(
						ByteBuffer.wrap(EncryptUtils.decryptPBE(bb.array(), 0, bb.limit(), msg.getSalt(), k.key)));

				msg.setSalt(null);

			}

		} catch (Exception e) {
			if (e instanceof CommonException) {
				throw (CommonException) e;
			} else {
				throw new CommonException("fail decrypt or check sign", e);
			}
		}
	}

	private String getInstanceName(int insId) {
		if (processId2InstanceNames.containsKey(insId)) {
			return processId2InstanceNames.get(insId);
		}

		String p = ChoyConstants.INS_ROOT + "/" + insId;
		if (!op.exist(p)) {
			throw new CommonException("Instance not found for ID: " + insId);
		}

		String oldJson = op.getData(p);
		ProcessInfo pri = JsonUtils.getIns().fromJson(oldJson, ProcessInfo.class);
		processId2InstanceNames.put(insId, pri.getInstanceName());

		return pri.getInstanceName();
	}

	public void signAndEncrypt(Message msg, int insId, boolean isUp) {

		ByteBuffer bb = (ByteBuffer) msg.getPayload();

		if (msg.isRsaEnc() && (msg.isUpSsl() || msg.isDownSsl())) {
			// 对于上行包，如果下行是安全的，则说明客户端有私钥，所以需要做签名
			if (!isUp && msg.isUpSsl() || isUp && msg.isDownSsl()) {
				String sign = EncryptUtils.sign(bb.array(), 0, bb.limit(), this.myPriKey);
				if (Utils.isEmpty(sign)) {
					throw new CommonException("Fail to sign");
				}
				msg.setSign(sign);
				msg.setSign(true);
			}

			if (!isUp && msg.isDownSsl() || isUp && msg.isUpSsl()) {
				byte[] encData = encrypt(bb.array(), 0, bb.limit(), insId);
				if (encData == null || encData.length == 0) {
					throw new CommonException("Fail to encript data");
				}
				msg.setPayload(ByteBuffer.wrap(encData));
			}
		}

		if (!msg.isRsaEnc() && (msg.isUpSsl() || msg.isDownSsl())) {
			// 对称加密
			PBEKey sec = this.insId2AesKey.get(insId);
			if (sec == null) {
				sec = generatorPDESecret(insId);
				if (sec == null) {
					throw new CommonException("Fail to exchange aes secrect!");
				}

				byte[] sedata = sec.key.getEncoded();
				sedata = this.encrypt(sedata, 0, sedata.length, insId);

				msg.setSec(sedata);
				msg.setSec(true);
				
				insId2AesKey.put(insId, sec);

			}

			byte[] salt = getSalt();
			byte[] edata = EncryptUtils.encryptPBE(bb.array(), 0, bb.limit(), salt, sec.key);
			msg.setSalt(salt);
			msg.setPayload(ByteBuffer.wrap(edata));

		}

		return;
	}

	private byte[] getSalt() {
		byte[] salt = new byte[EncryptUtils.SALT_LEN];
		Random random = new Random();
		random.nextBytes(salt);
		return salt;
	}

	private PBEKey generatorPDESecret(int insId) {
		SecretKey sec = EncryptUtils.generatorPBEKey(Config.getInstanceName(), EncryptUtils.KEY_PBE);
		PBEKey pk = new PBEKey(this.getInstanceName(insId), insId, sec);
		return pk;
	}

	/*
	 * private PBEKey doExchangeSecrect0(int insId) {
	 * 
	 * Set<ServiceItem> items =
	 * this.registry.getServices(IExchangeSecretService.class.getName());
	 * ServiceItem directItem = null;
	 * 
	 * for(ServiceItem si : items) { if (si.getInsId() == insId) { directItem = si;
	 * break; } }
	 * 
	 * if(directItem == null) { throw new CommonException("Instance [" +insId
	 * +"] not found!"); }
	 * 
	 * JMicroContext.get().setParam(Constants.DIRECT_SERVICE_ITEM, directItem);
	 * 
	 * SecretKey sec = EncryptUtils.generatorPBEKey(Config.getInstanceName(),
	 * EncryptUtils.KEY_PBE); PBEKey pk = new
	 * PBEKey(directItem.getKey().getInstanceName(),insId,sec);
	 * 
	 * byte[] sdata = pk.key.getEncoded();
	 * 
	 * Resp<Boolean> r = exchangeSrv.exchange(pi.getId(), sdata);
	 * 
	 * JMicroContext.get().removeParam(Constants.DIRECT_SERVICE_ITEM);
	 * 
	 * if(r.getData()) { this.insId2AesKey.put(insId, pk); return pk; } else {
	 * logger.error(r.getMsg()); return null; } }
	 */

	private RSAPublicKey getPublicKey(int insId) {
		String prefix = getInsPrefix(insId);
		if (prefix == null) {
			logger.error("Invalid : " + insId);
			return null;
		}

		if (!publicRsaKeys.containsKey(prefix)) {
			loadPublicKey(prefix);
		}

		if (!publicRsaKeys.containsKey(prefix)) {
			throw new CommonException("Fail to load public key for instance: " + insId);
		}

		return publicRsaKeys.get(prefix);
	}

	private byte[] encrypt(byte[] data, int pos, int len, int insId) {
		RSAPublicKey pubKey = getPublicKey(insId);
		return EncryptUtils.encrypt(pubKey, data, pos, len);
	}

	private void loadPublicKey(String prefix) {
		Resp<String> r = secretSrv.getPublicKeyByInstance(prefix);
		if (r.getCode() != 0) {
			String msg = "code: " + r.getCode() + ", msg: " + r.getMsg();
			logger.error(msg);
			throw new CommonException(msg);
		}

		try {
			// publicStrKeys.put(prefix, r.getData());
			RSAPublicKey pk = EncryptUtils.loadPublicKeyByStr(r.getData());
			publicRsaKeys.put(prefix, pk);
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	private String getInsPrefix(int insId) {
		String instanceName = this.getInstanceName(insId);
		if (Utils.isEmpty(instanceName)) {
			throw new CommonException("Instance name not found for: " + insId);
		}
		return getInsPrefix(instanceName);
	}

	private String getInsPrefix(String instanceName) {
		if (Utils.isEmpty(instanceName)) {
			throw new CommonException("Instance name can not be null ");
		}
		int idx = instanceName.length() - 1;

		while (idx > 0 && ExpUtils.isNumber(instanceName.charAt(idx))) {
			idx--;
		}

		if (idx > 0) {
			return instanceName.substring(0, idx + 1);
		}
		return null;
	}

	private String loadKeyContent(String priKey, String defPath) {

		InputStream is = null;
		String priFile = Config.getCommandParam(priKey);

		try {

			if (!Utils.isEmpty(priFile)) {
				if (new File(priFile).exists()) {
					is = new FileInputStream(priFile);
				}
				if (is == null) {
					is = SecretManager.class.getResourceAsStream(priFile);
				}
			}

			if (is == null) {
				priFile = defPath;
				if (!priFile.startsWith("/")) {
					priFile = "/" + priFile;
				}
				if (new File(priFile).exists()) {
					is = new FileInputStream(priFile);
				}
				if (is == null) {
					is = SecretManager.class.getResourceAsStream(priFile);
				}
			}

		} catch (FileNotFoundException e) {
			logger.warn("Private key file " + priFile + "  not found", e);
		}

		if (is == null) {
			return null;
		}

		StringBuffer sb = new StringBuffer();

		String line = null;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is));) {
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			return sb.toString();
		} catch (Exception e) {
			logger.error("error read key file: " + priFile, e);
		}
		return null;
	}

	public void ready() {

		String priStr = loadKeyContent("priKey",
				PUBKEYS_PREFIX + "jmicro_" + getInsPrefix(Config.getInstanceName()) + "_pri.key");
		if (!Utils.isEmpty(priStr)) {
			try {
				String pwd = Config.getCommandParam(Constants.PRIVATE_KEY_PWD);
				if (!Utils.isEmpty(pwd)) {
					SecretKey key = EncryptUtils.generatorPBEKey(pwd, EncryptUtils.KEY_PBE);
					byte[] data = Base64.getDecoder().decode(priStr);
					data = EncryptUtils.decryptPBE(data, 0, data.length, null, key);
					myPriKey = EncryptUtils.loadPrivateKey(data);
				} else {
					myPriKey = EncryptUtils.loadPrivateKeyByStr(priStr);
				}
			} catch (Exception e) {
				logger.error("", e);
			}
		}

		List<String> configFiles = ClassScannerUtils.getClasspathResourcePaths("META-INF/keys", "*pub.key");
		for (String fn : configFiles) {
			String pstr = loadKeyContent("", fn);
			if (!Utils.isEmpty(pstr)) {
				try {

					String name = fn.trim();
					name = name.substring(name.lastIndexOf("/") + 1, name.length());
					String prefix = name.split("_")[1];

					RSAPublicKey pkey = EncryptUtils.loadPublicKeyByStr(pstr);
					publicRsaKeys.put(prefix, pkey);
				} catch (Exception e) {
					logger.error("", e);
				}
			}
		}

		String pubPaths = Config.getCommandParam(Constants.PUBLIC_KEYS_FILES);
		if (!Utils.isEmpty(pubPaths)) {
			String[] paths = pubPaths.split(",");
			if (paths != null && paths.length > 0) {
				for (String p : paths) {
					p = p.trim();
					String fn = p.substring(p.lastIndexOf("/") + 1, p.length());
					String prefix = fn.split("_")[1];
					loadPublicKeys(p, prefix);
				}
			}
		}

		op.addChildrenListener(ChoyConstants.INS_ROOT, (opType, root, id, data) -> {
			if (opType == IListener.REMOVE) {
				Integer iid = Integer.parseInt(id);
				if (processId2InstanceNames.containsKey(iid)) {
					processId2InstanceNames.remove(iid);
				}
				
				if(insId2AesKey.containsKey(iid)) {
					insId2AesKey.remove(iid);
				}
			}
		});

	}

	private void loadPublicKeys(String file, String prefix) {
		String pstr = loadKeyContent("", file);
		if (!Utils.isEmpty(pstr)) {
			try {
				RSAPublicKey pkey = EncryptUtils.loadPublicKeyByStr(pstr);
				publicRsaKeys.put(prefix, pkey);
			} catch (Exception e) {
				logger.error("Load key file error:" + file, e);
			}
		}
	}

	private class PBEKey {
		// 对方的实例名称
		private String instanceName;
		// 对方的实例ID
		private int insId;
		private SecretKey key;

		private PBEKey() {

		}

		private PBEKey(String insName, int insId, SecretKey k) {
			this.instanceName = insName;
			this.insId = insId;
			this.key = k;
		}

	}
}
