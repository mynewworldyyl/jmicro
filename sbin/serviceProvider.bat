java ^
-Xbootclasspath/a:D:\opensource\github\jmicro\choreography\choreography.agent\data\JMicroAgent0\resourceDir/org/javassist/javassist-3.24.0-GA.jar ^
-javaagent:D:\opensource\github\jmicro\choreography\choreography.agent\data\JMicroAgent0\resourceDir\cn\jmicro/jmicro.agent-0.0.2-SNAPSHOT.jar ^
-jar D:\opensource\github\jmicro\example\example.provider\target\expjmicro.example.provider-0.0.2-SNAPSHOT-with-core.jar ^
-DclientId=0 -DadminClientId=0 -Dlog4j.configuration=D:/opensource/github/jmicro/log4j.xml -DpriKeyPwd=provider12345 -Dpwd=0 ^
-D/mongodb/password=jm*^&icrodb123Abc&^% -D/mongodb/username=jmicrodb ^