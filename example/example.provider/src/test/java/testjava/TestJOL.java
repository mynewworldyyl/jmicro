package testjava;

import org.junit.Test;
import org.openjdk.jol.info.ClassLayout;

public class TestJOL {

	@Test
	public void testObject01() {
		//System.out.println(ClassLayout.parseInstance(new Object()).toPrintable());
		
		System.out.println(ClassLayout.parseInstance(new TV()).toPrintable());
	}
	
	@Test
	public void testStringConstant() {
		//System.out.println(ClassLayout.parseInstance(new Object()).toPrintable());
		String str1 = "你好";
		String str2 =new String("你好");
		String str3 = str2.intern();
		
		System.out.println(str1 == str2);
		System.out.println(str2 == str3);
		System.out.println(str1 == str3);
	}
	
	@Test
	public void testCombineWrite() {
		 System.out.println("单次执行 (ms) = " + runCaseOne()/100_0000);
	     System.out.println("拆分两次执行 (ms) = " + runCaseTwo()/100_0000);
	}
	
	public static long runCaseOne() {
        long start = System.nanoTime();
        int i = ITERATIONS;
        while (--i != 0) {
            int slot = i & MASK;
            byte b = (byte) i;
            arrayA[slot] = b;
            arrayB[slot] = b;
            arrayC[slot] = b;
            arrayD[slot] = b;
            
            arrayE[slot] = b;
            arrayF[slot] = b;
            arrayG[slot] = b;
            arrayH[slot] = b;
        }
        return System.nanoTime() - start;
    }

    public static long runCaseTwo() {
        long start = System.nanoTime();
        int i = ITERATIONS;
        while (--i != 0) {
            int slot = i & MASK;
            byte b = (byte) i;
            arrayA[slot] = b;
            arrayB[slot] = b;
            arrayC[slot] = b;
            arrayD[slot] = b;
        }
        i = ITERATIONS;
        while (--i != 0) {
            int slot = i & MASK;
            byte b = (byte) i;
            //arrayD[slot] = b;
            arrayE[slot] = b;
            arrayF[slot] = b;
            arrayG[slot] = b;
            arrayH[slot] = b;
        }
        return System.nanoTime() - start;
    }
    
	private static final int ITERATIONS = Integer.MAX_VALUE;
    private static final int ITEMS = 1 << 24;
    private static final int MASK = ITEMS - 1;
    private static final byte[] arrayA = new byte[ITEMS];
    private static final byte[] arrayB = new byte[ITEMS];
    private static final byte[] arrayC = new byte[ITEMS];
    private static final byte[] arrayD = new byte[ITEMS];
    private static final byte[] arrayE = new byte[ITEMS];
    private static final byte[] arrayF = new byte[ITEMS];
    private static final byte[] arrayG = new byte[ITEMS];
    private static final byte[] arrayH = new byte[ITEMS];
}

 class TV{
	int iv;
}
