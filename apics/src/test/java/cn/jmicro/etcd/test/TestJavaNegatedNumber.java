package cn.jmicro.etcd.test;

import org.junit.Test;

public class TestJavaNegatedNumber {

	/**
	 *  x -1 = 255 =>  256
	 *  x -2 = 254 =>  256
	 *  
	 *  y = x + 256 将一个字节的负数转化为对应的正数公式，x表示一个byte的负数， y表示对应的正数，y至少两或以上个字节
	 */
	@Test
	public void testNegated2Positive() {
		System.out.println("255: "+((byte)255));
		System.out.println("254: "+((byte)254));
		System.out.println("253: "+((byte)253));
		System.out.println("252: "+((byte)252));
		System.out.println(".     .");
		System.out.println(".     .");
		System.out.println(".     .");
		System.out.println("130: "+((byte)130));
		System.out.println("129: "+((byte)129));
		System.out.println("128: "+((byte)128));
		System.out.println("127: "+((byte)127));
	}
}
