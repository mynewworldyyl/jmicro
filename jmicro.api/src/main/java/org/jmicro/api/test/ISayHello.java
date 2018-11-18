package org.jmicro.api.test;

import org.jmicro.api.annotation.Service;

@Service(namespace="testsayhello",version="0.0.1")
public interface ISayHello {
	String hello(String name);
}
