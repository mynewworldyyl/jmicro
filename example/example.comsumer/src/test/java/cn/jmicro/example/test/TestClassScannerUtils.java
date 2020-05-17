package cn.jmicro.example.test;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.ClassScannerUtils;



public class TestClassScannerUtils{

    private static final Logger logger = LoggerFactory.getLogger(TestClassScannerUtils.class);
    
    @Test
	public void testScannerManifest() {
    	ClassScannerUtils.getClasspathResourcePaths("META-INF/jmicro", "*.properties");
	}
    
	

}
