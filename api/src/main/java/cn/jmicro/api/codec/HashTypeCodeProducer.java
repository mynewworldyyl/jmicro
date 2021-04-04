package cn.jmicro.api.codec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.jmicro.common.CommonException;
import cn.jmicro.common.util.StringUtils;

public class HashTypeCodeProducer implements ITypeCodeProducer {

	private Map<String,Short> name2Types = new HashMap<>();
	
	private Set<Short> types = new HashSet<>();
	
	@Override
	public short getTypeCode(String name) {
		if(StringUtils.isEmpty(name)) {
			throw new CommonException("Type code class name cannot be null");
		}
		
		if(name2Types.containsKey(name)) {
			return name2Types.get(name);
		} else {
			return hs(name);
		}
	}
	
	// 32768 + X = -32768 => X = -65536
	// 32769 + X = -32767 => X = -65536
	// 65535 + X = -1 	  => X = -65536
	private short hs(String name) {
        char[] charr = name.toCharArray();
		int h = 0;
		
        if(charr.length > 0) {
            for (int i = 0; i < charr.length; i++) {
                h = 17 * h + charr[i];
            }
        }
        
        if(h > 65535) {
			h = h % 65535;
		}
        
        if(h > Short.MAX_VALUE) {
        	h = h - 65535; //h between -1 ~ -32768
        }
        
        return (short)h;
    }

	@Override
	public String getNameByCode(Short code) {
		// TODO Auto-generated method stub
		return null;
	}

}
