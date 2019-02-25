package org.jmicro.example.test;

import org.jmicro.api.ClassScannerUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class TestClassScannerUtils{

    private static final Logger logger = LoggerFactory.getLogger(TestClassScannerUtils.class);
    
    @Test
	public void testScannerManifest() {
    	ClassScannerUtils.getClasspathResourcePaths("META-INF/jmicro", "*.properties");
	}
    
	

}
