package org.jmicro.api.codec;

import java.io.DataInput;
import java.io.DataOutput;

public interface AbstractSerializeObject {

	public void encode(DataOutput buffer);
	
	public Object decode(DataInput buffer);
	
}
