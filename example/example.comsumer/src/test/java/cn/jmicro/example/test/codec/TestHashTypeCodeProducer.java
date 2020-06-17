package cn.jmicro.example.test.codec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import cn.jmicro.api.ClassScannerUtils;
import cn.jmicro.api.codec.HashTypeCodeProducer;

public class TestHashTypeCodeProducer {

	@Test
	public void testGetHash() {
		
		Map<Short,Set<String>> code2name = new HashMap<>();
		Map<String,Short> name2code = new HashMap<>();
		
		HashTypeCodeProducer hcp = new HashTypeCodeProducer();
		List<String> sos = ClassScannerUtils.getClasspathResourcePaths("org","*");
		if(sos != null && !sos.isEmpty()) {
			for(String c : sos){
				short hs = hcp.getTypeCode(c);
				if(!code2name.containsKey(hs)) {
					code2name.put(hs, new HashSet<>());
				}
				code2name.get(hs).add(c);
				name2code.put(c,hs);
				System.out.println(c + ":" + hs);
			}
			
			System.out.println("======================================");
			System.out.println("Total class: " + sos.size());
			int cnt = 0;
			for(Map.Entry<Short, Set<String>> e : code2name.entrySet()) {
				if(e.getValue().size() > 1) {
					cnt++;
					System.out.println("Repeat elt: " + e.getValue());
				}
			}
			System.out.println("Repeat cnt: " + cnt);
		}
		
	}
}
