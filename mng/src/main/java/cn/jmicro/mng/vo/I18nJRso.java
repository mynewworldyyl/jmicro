package cn.jmicro.mng.vo;

import cn.jmicro.api.PersistVo;
import lombok.Data;

@Data
public class I18nJRso extends PersistVo {

	private String lan;
	
	private String country;
	
	private String key;
	
	private String val;
	
	private String desc;
	
	private String mod;
	
}
