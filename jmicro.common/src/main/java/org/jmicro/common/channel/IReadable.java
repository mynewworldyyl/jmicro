package org.jmicro.common.channel;

import java.io.IOException;

public interface IReadable<T> {

	T read()  throws IOException;
}
