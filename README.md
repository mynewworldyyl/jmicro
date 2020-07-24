#  请移步到个人博客查看更多信息 https://www.cnblogs.com/jmicro/
Demo
http://124.70.152.7

# JMicro说明
1. 以简单方式实现微服务相关功能，包括服务注册，服务发现，服务监控，超时，重试，限速，降级，熔断，负载均衡等
2. 模块之间确保最底偶合度，非常易于扩展，参考jmicro.api，基本上每个接口都只有一个方法
3. 完全基于Java注解声明组件，没有任何XML，property之类的配置;
4. 为微服务定制的极其轻量IOC容器，目前代码大概1500行左右;
5. 监控埋点，可以详细监控服务每个细节，如每个RPC方法响应时间，异常次数，qps等等，并且监控的点非常易于替换或扩展;
6. 如果你喜欢，可以0配置启N个服务，但可以实时修改每个服务方法的配置，并且实时生效；
7. 简单一致的HTTP支持，这样就可以以HTTP方式接入任何客户端；
8. 每个请求，连接，消息有全局唯一标识，实现整个请求的全流程串连监控；
9. 运行jmicro.example样例，体验基于JMicro开发服务有多简单；
10. 基本上无同步块代码（当然前提能保证线程安全），确保框架不存在性能瓶颈；
11. 可选通过线程和协程（FIBER）做请求分发；
12. 客户端一个请求多个响应，类似订阅
13. 更多功能会持继增加

# 下载源代码
git checkout https://github.com/mynewworldyyl/jmicro.git

# 构建JMicro全部依赖包
进入到下载的源码目录，执行如下命令：

maven clean install  -Dmaven.test.skip=true

# 启动Zookeeper，端口保持默认值2181
 参考：https://zookeeper.apache.org/doc/r3.4.13/zookeeperStarted.html
 
# 启动Redis，端口保持默认值6379
 Linux: https://redis.io/download
 
 Windows: https://github.com/MicrosoftArchive/redis/releases
 
# 构建运行服务提供方

打开命令行窗口，进入
cd ${SRC_ROOT}\jmicro.example
maven clean install  -Dmaven.test.skip=true


进入provider目录

cd ${SRC_ROOT}\jmicro.example\jmicro.example.provider

构建运行包

mvn clean install -Pbuild-main -Dmaven.test.skip=true

运行服务

java -jar target/jmicro.example.provider-0.0.1-SNAPSHOT-jar-with-dependencies.jar -javaagent:${SRC_ROOT}/jmicro.agent/target/jmicro.agent-0.0.1-SNAPSHOT.jar


# 构建运行服务消费方

打开一个新命令行窗口

进入comsumer目录

cd ${SRC_ROOT}\jmicro.example\jmicro.example.comsumer

构建运行包

mvn clean install -Pbuild-main  -Dmaven.test.skip=true

运行服务

java -jar target/jmicro.example.comsumer-0.0.1-SNAPSHOT-jar-with-dependencies.jar -javaagent:${SRC_ROOT}/jmicro.agent/target/jmicro.agent-0.0.1-SNAPSHOT.jar

最后一行看到如下输出 ，即服务提供方返回的消息

Server say hello to: Hello JMicro

## 疑难解答

1 如果碰到编码解码，序列化相关错误，首先检查服务启动命令是否缺失如下参数
-javaagent:$JMICRO_ROOT/agent/target/jmicro-agent-0.0.1-SNAPSHOT.jar





