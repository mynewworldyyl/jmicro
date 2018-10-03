package org.jmicro.api.config;

import java.util.Map;

public interface IConfigLoader {

	Map<String,String> load(String[] args);
}
