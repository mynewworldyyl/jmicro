package org.jmicro.api.codec;

public interface ICodecFactory {

	IDecoder getDecoder();
	
	IEncoder getEncoder();
	
}
