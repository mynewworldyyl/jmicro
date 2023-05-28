package cn.jmicro.api.codec;

import java.io.IOException;

import org.junit.Test;

public class TestExtraDecodrEncode {

	@Test
	public void testEncode() throws IOException{
		TestEntity e = new TestEntity();
		
		JDataOutput jo = new JDataOutput();
		SimpleCodecFactory.doExtraEncode(jo, e);
		
		JDataInput ji = new JDataInput(jo.getBuf());
		Object dov = SimpleCodecFactory.doExtraDecode(ji);
		
		System.out.println(dov);
		
	}


}
