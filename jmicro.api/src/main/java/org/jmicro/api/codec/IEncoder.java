package org.jmicro.api.codec;

import org.jmicro.api.server.RpcResponse;

public interface IEncoder {

	byte[] encode(RpcResponse message);
}
