package org.jmicro.example.api.rpc;

import org.jmicro.api.annotation.Service;

@Service
public interface ISimpleRpc {
	String hello(String name);
}
