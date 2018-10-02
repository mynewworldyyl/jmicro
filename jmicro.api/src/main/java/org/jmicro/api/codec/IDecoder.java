package org.jmicro.api.codec;

import org.jmicro.api.server.RpcRequest;

public interface IDecoder {

	RpcRequest decode(byte[] data);
}
