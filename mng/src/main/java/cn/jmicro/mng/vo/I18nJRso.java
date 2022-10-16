package cn.jmicro.mng.vo;

import cn.jmicro.api.PersistVo;
import cn.jmicro.api.annotation.SO;
import lombok.Data;
import lombok.Serial;

@Data
@SO
@Serial
public class I18nJRso extends PersistVo {

	private String lan;
	
	private String country;
	
	private String key;
	
	private String val;
	
	private String desc;
	
	private String mod;
	
}
