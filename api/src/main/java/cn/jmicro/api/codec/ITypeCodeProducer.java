package cn.jmicro.api.codec;

public interface ITypeCodeProducer {

	short getTypeCode(String name);
	
	String getNameByCode(Short code);
	
}
