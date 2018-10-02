package org.jmicro.common.channel;

import java.io.IOException;

public interface IWritable<T> {

	void write(T o) throws IOException;
	
}
