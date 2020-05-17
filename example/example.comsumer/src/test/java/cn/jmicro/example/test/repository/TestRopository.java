package cn.jmicro.example.test.repository;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import cn.jmicro.choreography.api.IResourceResponsitory;
import cn.jmicro.common.CommonException;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestRopository extends JMicroBaseTestCase {
	
	@Test
	public void testIResourceResponsitory() throws IOException {
		String name = "GO.png";
		
		InputStream is = TestRopository.class.getResourceAsStream(name);
		byte[] data = new byte[is.available()];
		is.read(data, 0, data.length);
		
		IResourceResponsitory respo = of.getRemoteServie(IResourceResponsitory.class.getName(),"rrs", "0.0.1", null);
		int blockSize = respo.addResource(name, data.length);
		if(blockSize < 0) {
			throw new CommonException("File exist");
		}
		int blockNum = data.length / blockSize;
		for(int i = 0; i < blockNum; i++) {
			byte[] bd = new byte[blockSize];
			System.arraycopy(data, blockSize*i, bd, 0, blockSize);
			org.junit.Assert.assertTrue(respo.addResourceData(name, bd, i));
		}
		
		int lastSize = data.length % blockSize;
		if(lastSize > 0) {
			byte[] bd = new byte[lastSize];
			System.arraycopy(data, blockNum*blockSize, bd, 0, lastSize);
			org.junit.Assert.assertTrue(respo.addResourceData(name, bd, blockNum));
		}
		
		is.close();
		
		waitForReady(30*60);
	}
	
	@Test
	public void testLoadAndStorePicture() throws IOException {
		String name = "GO.png";
		
		FileOutputStream fos = new FileOutputStream("D:\\opensource\\resDataDir\\GO111.png");
		
		InputStream is = TestRopository.class.getResourceAsStream(name);
		byte[] data = new byte[is.available()];
		is.read(data, 0, data.length);
		
		int blockSize = 1024;
		int blockNum = data.length / blockSize;
		for(int i = 0; i < blockNum; i++) {
			byte[] bd = new byte[blockSize];
			System.arraycopy(data, blockSize*i, bd, 0, blockSize);
			fos.write(bd, 0, bd.length);
		}
		
		int lastSize = data.length % blockSize;
		if(lastSize > 0) {
			byte[] bd = new byte[lastSize];
			System.arraycopy(data, blockNum*blockSize, bd, 0, lastSize);
			fos.write(bd, 0, bd.length);
		}
		
		fos.close();
		is.close();
	}
	
}
