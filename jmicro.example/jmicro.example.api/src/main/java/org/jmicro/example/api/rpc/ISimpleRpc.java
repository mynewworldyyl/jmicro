package org.jmicro.example.api.rpc;

import org.jmicro.api.annotation.Service;

@Service(namespace="simpleRpc",version="0.0.1", monitorEnable=1)
public interface ISimpleRpc {
	String hello(String name);
}
