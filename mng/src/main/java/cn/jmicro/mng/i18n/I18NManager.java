package cn.jmicro.mng.i18n;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.StringUtils;

@Component
public class I18NManager {
	
	private static Class<?> DEFAULT_RESOURCE_BUNDLE_NAME = I18NManager.class;
	
	public void setDefauleCls(Class<?> cls) {
		if(cls == null) {
			throw new NullPointerException();
		}
		DEFAULT_RESOURCE_BUNDLE_NAME = cls;
	}
	
	public Map<String,String> all(String resPath,String lan) {
		
		Map<String,String> rst = new HashMap<>();
		try {
			String path = resPath.replaceAll("\\.","/");
			if(StringUtils.isEmpty(lan)) {
				lan = "zh";
			}
			Locale locale = Locale.forLanguageTag(lan);
			ResourceBundle bundle = ResourceBundle.getBundle(path, locale, I18NManager.class.getClassLoader());
			for(String k: bundle.keySet()) {
				rst.put(k, new String(bundle.getString(k).getBytes("ISO-8859-1"),Constants.CHARSET));
			}
			return rst;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rst;
	}
	
	public String getString(String key,Locale locale,ClassLoader loader, Class<?> clazz, String...args) {
		String path = clazz.getName().replace('.', '/');
		ResourceBundle bundle = null;
		try {
			bundle = ResourceBundle.getBundle(path, locale, loader);
		} catch (Exception e) {
		}
		
		String msg = null;
		try {
			msg = bundle.getString(key);
		} catch (MissingResourceException e) {
			msg = key;
		}
		
		if(StringUtils.isNotEmpty(msg)) {
			msg = MessageFormat.format(msg, args);
		}
		return msg;
	}
	
	
	public String getString(String key,ClassLoader loader,Locale locale) {
		return this.getString(key,locale,loader,DEFAULT_RESOURCE_BUNDLE_NAME);
	}
	
	public String getString(String key,Locale locale,ClassLoader loader,String...args) {
		return this.getString(key,locale,loader,DEFAULT_RESOURCE_BUNDLE_NAME,args);
	}
	
	public String getString(String key,Locale locale) {
		return this.getString(key,locale,Thread.class.getClassLoader(),DEFAULT_RESOURCE_BUNDLE_NAME);
	}
	
	public String getString(String key,Locale locale,String...args) {
		return this.getString(key,locale,Thread.class.getClassLoader(),DEFAULT_RESOURCE_BUNDLE_NAME,args);
	}
	
	public String getString(String key,String...args) {
		return this.getString(key,getLocale(),Thread.class.getClassLoader(),DEFAULT_RESOURCE_BUNDLE_NAME,args);
	}
	
	public Locale getLocale() {
		Locale locale = null;
		if(locale == null) {
			locale =  Locale.getDefault();
		}
		return locale;
	}
}
