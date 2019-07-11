package org.jmicro.api.codec;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public abstract interface ISerializer {

	public abstract void encode(DataOutput buffer,Object obj)  throws IOException;
	
	public abstract Object decode(DataInput buffer)  throws IOException;
	
}
