package org.jmicro.api.codec;

import org.jmicro.api.annotation.CodecFactory;
import org.jmicro.common.Constants;

@CodecFactory(Constants.DEFAULT_CODEC_FACTORY)
public class DefaultCodecFactory implements ICodecFactory{

	private Decoder dec = new Decoder();
	
	private Encoder enc = new Encoder();
	
	@Override
	public IDecoder getDecoder() {
		return dec;
	}

	@Override
	public IEncoder getEncoder() {
		return enc;
	}

}
