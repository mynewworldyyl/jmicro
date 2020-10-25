package cn.jmicro.api.rsa;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;

public class EncryptUtils {
	/**
	 * 字节数据转字符串专用集合
	 */
	private static final char[] HEX_CHAR = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e','f' };

	/**
	 * 签名算法
	 */
	public static final String SIGN_ALGORITHMS = "SHA1WithRSA";
	
	public static final String KEY_PBE = "PBEWITHMD5andDES";
	
	public static final int SALT_COUNT = 100;
	public static final int SALT_LEN = 8;
	
	public static final byte[] SALT_DEFAULT = new byte[] {1,2,3,4,5,7,3,6};
	
	public static final int ENCRY_SEC_LEN = 64;
	
	public static final int DECRY_SEC_LEN = 128;
	
	/**
	 * 随机生成密钥对
	 */
	public static Map<String,String> genRsaKey(String pwd) {
		// KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象
		KeyPairGenerator keyPairGen = null;
		try {
			keyPairGen = KeyPairGenerator.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			throw new CommonException("genKeyPair",e);
		}
		
		Map<String,String> kp = new HashMap<>();
		try {
			//初始化密钥对生成器，密钥大小为96-1024位
			keyPairGen.initialize(1024, new SecureRandom());
			
			//生成一个密钥对，保存在keyPair中
			KeyPair keyPair = keyPairGen.generateKeyPair();
			
			RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
			RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
			
			String publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
			//
			byte[] priData = privateKey.getEncoded();
			if(!Utils.isEmpty(pwd)) {
				SecretKey key = generatorPBEKey(pwd,KEY_PBE);
				priData = encryptPBE(priData,0,priData.length,null,key);
			}
			
			String privateKeyString = Base64.getEncoder().encodeToString(priData);
			
			kp.put("publicKey", publicKeyString);
			kp.put("privateKey", privateKeyString);
			return kp;
		} catch (Exception e) {
			throw new CommonException("genKeyPair",e);
		}
	}
	
	public static SecretKey generatorPBEKey(String password,String alg) {
		//加 密 口令与密钥
        try {
			PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray());
			SecretKeyFactory factory = SecretKeyFactory.getInstance(alg);
			SecretKey key = factory.generateSecret(pbeKeySpec);
			return key;
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new CommonException("",e);
		}
	}

	/** 
     * PBE 解密 
     * 
     * @param data 需要解密的字节数组 
     * @param key  密钥 
     * @param salt 盐 
     * @return 
     */  
	public static byte[] decryptPBE(byte[] data,int pos,int len,byte[] salt,SecretKey key) {  
        try {
			//获取密钥  
			Cipher cipher = Cipher.getInstance(EncryptUtils.KEY_PBE);  
			if(salt == null || salt.length > 0) {
        		salt = SALT_DEFAULT;
        	}
        	PBEParameterSpec parameterSpec = new PBEParameterSpec(salt, SALT_COUNT);
			cipher.init(Cipher.DECRYPT_MODE, key,parameterSpec);  
			return cipher.doFinal(data,pos,len);
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
				| BadPaddingException | InvalidAlgorithmParameterException e) {
			throw new CommonException("decryptPBE error",e);
		}
    }  
	
	public static byte[] encryptPBE(byte[] data,int pos,int len, byte[] salt, SecretKey key) {  
        try {
        	Cipher cipher = Cipher.getInstance(EncryptUtils.KEY_PBE);  
        	if(salt == null || salt.length > 0) {
        		salt = SALT_DEFAULT;
        	}
        	PBEParameterSpec parameterSpec = new PBEParameterSpec(salt, SALT_COUNT);
			cipher.init(Cipher.ENCRYPT_MODE, key,parameterSpec);  
			return cipher.doFinal(data,pos,len);
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
				| BadPaddingException | InvalidAlgorithmParameterException e) {
			throw new CommonException("encryptPBE error",e);
		}  
    
    }  
  
	/**
	 * 从字符串中加载公钥
	 * 
	 * @param publicKeyStr 公钥数据字符串
	 * @throws Exception 加载公钥时产生的异常
	 */
	public static RSAPublicKey loadPublicKeyByStr(String publicKeyStr) {
		try {
			byte[] buffer = Base64.getDecoder().decode(publicKeyStr);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
			return (RSAPublicKey) keyFactory.generatePublic(keySpec);
		} catch (Exception e) {
			throw new CommonException("",e);
		}
	}

	public static RSAPrivateKey loadPrivateKeyByStr(String privateKeyStr) {
		byte[] buffer = Base64.getDecoder().decode(privateKeyStr);
		return loadPrivateKey(buffer);
	}
	
	public static RSAPrivateKey loadPrivateKey(byte[] data) {
		try {
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(data);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new CommonException("",e);
		}
	}

	/**
	 * 公钥加密过程
	 * 
	 * @param publicKey     公钥
	 * @param plainTextData 明文数据
	 * @return
	 * @throws Exception 加密过程中的异常信息
	 */
	public static byte[] encrypt(RSAPublicKey publicKey, byte[] plainTextData, int pos, int len0) {
		if (publicKey == null) {
			throw new CommonException("加密公钥为空, 请设置");
		}

		try {
			//使用默认RSA
			Cipher cipher = Cipher.getInstance("RSA");
			//cipher= Cipher.getInstance("RSA", new BouncyCastleProvider());
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			
			List<byte[]> eds = new ArrayList<>();
			
			int offset = pos + len0;
			
			for(int i = pos; i < offset; i += ENCRY_SEC_LEN) {
				int len = i + ENCRY_SEC_LEN < offset ? ENCRY_SEC_LEN : offset - i;
				byte[] ed = cipher.doFinal(plainTextData, i, len);
				eds.add(ed);
			}
			
			int size = 0;
			for( byte[] d : eds ) {
				size += d.length;
			}
			
			byte[] output = new byte[size];
			int os = 0;
			for(int i = 0; i < eds.size(); i++) {
				byte[] d = eds.get(i);
				System.arraycopy(d, 0, output, os, d.length);
				os += d.length; 
			}
			
			return output;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException 
				|IllegalBlockSizeException |BadPaddingException e) {
			throw new CommonException("",e);
		}
	}

	/**
	 * 私钥解密过程
	 * 
	 * @param privateKey 私钥
	 * @param cipherData 密文数据
	 * @return 明文
	 * @throws Exception 解密过程中的异常信息
	 */
	public static byte[] decrypt(RSAPrivateKey privateKey, byte[] cipherData,int pos,int len0) {
		if (privateKey == null) {
			throw new CommonException("解密私钥为空, 请设置");
		}
		Cipher cipher = null;
		try {
			// 使用默认RSA
			cipher = Cipher.getInstance("RSA");
			// cipher= Cipher.getInstance("RSA", new BouncyCastleProvider());
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			
			List<byte[]> eds = new ArrayList<>();
			
			int offset = pos + len0;
			
			for(int i = pos; i < offset; i += DECRY_SEC_LEN) {
				int len = i+DECRY_SEC_LEN < offset ? DECRY_SEC_LEN : offset - i;
				byte[] ed = cipher.doFinal(cipherData, i, len);
				eds.add(ed);
			}
			
			int size = 0;
			for(byte[] d : eds ) {
				size += d.length;
			}
			
			byte[] output = new byte[size];
			int offset0 = 0;
			for(int i = 0; i < eds.size(); i++) {
				byte[] d = eds.get(i);
				System.arraycopy(d, 0, output, offset0, d.length);
				offset0 += d.length; 
			}
			
			return output;
			
		} catch (Exception e) {
			throw new CommonException("",e);
		}
	}
	
	/**
	 * 私钥解密过程
	 * 
	 * @param privateKey 私钥
	 * @param cipherData 密文数据
	 * @return 明文
	 * @throws Exception 解密过程中的异常信息
	 */
	public static String decrypt(RSAPrivateKey privateKey, String cipherData) {
		if (privateKey == null) {
			throw new CommonException("解密私钥为空, 请设置");
		}
		try {
			byte[] data = cipherData.getBytes(Constants.CHARSET);
			return new String(decrypt(privateKey,data,0,data.length));
		} catch (UnsupportedEncodingException e) {
			throw new CommonException("",e);
		}
	}

	
	/**
	 * RSA签名
	 * 
	 * @param content    待签名数据
	 * @param privateKey 商户私钥
	 * @param encode     字符集编码
	 * @return 签名值
	 */
	public static String sign(String content, String privateKey) {
		return sign(content,privateKey,Constants.CHARSET);
	}
	
	public static String sign(byte[] content,int pos,int len, PrivateKey privateKey) {
		try {
			java.security.Signature signature = java.security.Signature.getInstance(SIGN_ALGORITHMS);
			signature.initSign(privateKey);
			signature.update(content,pos,len);
			byte[] signed = signature.sign();
			return Base64.getEncoder().encodeToString(signed);
		} catch (Exception e) {
			throw new CommonException("",e);
		}
	}

	public static String sign(String content, String privateKey, String encode) {
		try {
			PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey));
			KeyFactory keyf = KeyFactory.getInstance("RSA");
			PrivateKey priKey = keyf.generatePrivate(priPKCS8);
			byte[] data = content.getBytes(Constants.CHARSET);
			return sign(data,0,data.length,priKey);
		} catch (UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new CommonException("",e);
		}
	}

	/**
	 * RSA验签名检查
	 * 
	 * @param content   待签名数据
	 * @param sign      签名值
	 * @param publicKey 分配给开发商公钥
	 * @param encode    字符集编码
	 * @return 布尔值
	 */
	public static boolean doCheck(String content, String sign, String publicKey, String encode) {
		try {
			byte[] data = content.getBytes(Constants.CHARSET);
			return doCheck(data,0,data.length,sign,publicKey);
		} catch (UnsupportedEncodingException e) {
			throw new CommonException("",e);
		}
	}
	
	public static boolean doCheck(byte[] content,int pos,int len, String sign, String publicKey) {
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			byte[] encodedKey = Base64.getDecoder().decode(publicKey);
			PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));
			return doCheck(content,pos,len,sign,pubKey);
		} catch (Exception e) {
			throw new CommonException("",e);
		}
	}
	
	public static boolean doCheck(byte[] content,int pos,int len, String sign, PublicKey pubKey ) {
		try {
			java.security.Signature signature = java.security.Signature.getInstance(SIGN_ALGORITHMS);
			signature.initVerify(pubKey);
			signature.update(content,pos,len);
			boolean bverify = signature.verify(Base64.getDecoder().decode(sign));
			return bverify;
		} catch (Exception e) {
			throw new CommonException("",e);
		}
	}


	public static boolean doCheck(String content, String sign, String publicKey) {
		return doCheck(content,sign,publicKey,Constants.CHARSET);
	}
	
	public static void main(String[] args) {
		Map<String,String> kp = genRsaKey(null);
		System.out.println(kp.get("publicKey"));
		System.out.println(kp.get("privateKey"));
		
	}
}
