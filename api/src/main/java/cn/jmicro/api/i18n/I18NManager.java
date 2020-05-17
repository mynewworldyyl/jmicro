package cn.jmicro.api.i18n;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.JMethod;

@Component
public class I18NManager {

	@Inject(required=false)
	private Set<II18N> i18ns = new HashSet<>();
	
	private Map<String,Map<String,String>> lang2Values = new HashMap<>();
	
	@JMethod("ready")
	public void ready() {
		if(i18ns.isEmpty()) {
			return;
		}
		for(II18N i : this.i18ns) {
			Map<String,String> langMap = this.lang2Values.get(i.key());
			if(langMap == null) {
				langMap = new HashMap<String,String>();
				this.lang2Values.put(i.key(), langMap);
			}
			langMap.putAll(i.values());
		}
	}
	
	public String value(String lang,String key){
		if(this.lang2Values.isEmpty()) {
			this.ready();
		}
		if(this.lang2Values.containsKey(lang)) {
			return lang2Values.get(lang).get(key);
		}
		return "";
	}
	
	public Map<String,String> values(String lang){
		if(this.lang2Values.isEmpty()) {
			this.ready();
		}
		return lang2Values.get(lang);
	}
	
	public void regist(String lang,String key,String val){
		Map<String,String> langMap = this.lang2Values.get(lang);
		if(langMap == null) {
			langMap = new HashMap<String,String>();
			this.lang2Values.put(lang, langMap);
		}
		langMap.put(key, val);
		
	}
	
	public void regist(String lang,Map<String,String> values){
		Map<String,String> langMap = this.lang2Values.get(lang);
		if(langMap == null) {
			langMap = new HashMap<String,String>();
			this.lang2Values.put(lang, langMap);
		}
		langMap.putAll(values);
		
	}
	
}
