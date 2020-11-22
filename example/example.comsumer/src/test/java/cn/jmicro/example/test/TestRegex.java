package cn.jmicro.example.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class TestRegex {

	@Test
	public void testRegexMatch() {
		/*String idmatch = "[0-9a-zA-Z_\\.\\-]+";	
		String data = "cn.jmicro.api.executor.IExecutorInfo##security0.executorPool_NIO-BossGroup##0.0.1##security0##192.168.56.1##63296##hello##";
		String rexp = "^"+idmatch+"##"+idmatch+"##"+idmatch+"##"+idmatch+
				"##\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}##\\d{1,5}##[a-zA-Z_][a-zA-Z0-9_]*##";*/
        
		String data = "cn.jmicro.example.api.rpc.ISimpleRpc##simpleRpc##0.0.1##exampleProdiver0##192.168.56.1##60630##hello##Ljava/lang/String;##abc";
		String rexp = "^cn.jmicro.example.api.rpc.ISimpleRpc##[0-9a-zA-Z_\\.\\-]+##[0-9a-zA-Z_\\.\\-]+##[0-9a-zA-Z_\\.\\-]+##\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}##\\d{1,5}##hello##[;L\\]\\.,\\/a-zA-Z0-9]*##[a-zA-Z0-9\\_\\-]*$";
		Pattern pattern = Pattern.compile(rexp);
        Matcher matcher = pattern.matcher(data);
        if(matcher.find()){
            System.out.println(matcher.group());
        }else{
            System.out.println("nothing");
        }

		
	}
}
