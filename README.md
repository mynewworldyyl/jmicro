#  请移步到个人博客查看更多信息 https://www.cnblogs.com/jmicro/
Demo： http://jmicro.cn
Email： mynewworldyyl@gmail.com

# JMicro说明
1. 以简单方式实现微服务相关功能，包括服务注册，服务发现，服务监控，超时，重试，限速，降级，熔断，负载均衡等;
2. 模块之间确保最底偶合度，易于扩展，参考jmicro.api，基本上每个接口都只有一个方法;
3. 完全基于Java注解声明组件，没有任何XML，property之类的配置;
4. 为微服务定制的极其轻量IOC容器，目前代码大概2000行左右;
5. 监控埋点，可以详细监控服务每个细节，如每个RPC方法响应时间，异常次数，qps等等，并且监控的点非常易于替换或扩展;
6. 如果你喜欢，可以0配置启N个服务，但可以实时修改每个服务方法的配置，并且实时生效;
7. 简单一致的HTTP服务调用支持，这样就可以以HTTP方式接入任何客户端;
8. 每个请求，连接，消息有全局唯一标识，实现整个请求的全流程串连监控;
9. 运行example/expjmicro.tx样例，体验基于JMicro开发微服务;
10. 支持分布式事务，实现2PC及3PC策略;
11. 接口级的安全加密通信;
12. 统一日志收集及查询分类服务;
13. 统一RPC服务链路监控服务;
14. 消息服务;
15. 账号及权限服务;
16. API网关;
17. 全功能系统管理后台;
18. 统一资源管理;
19. 服务托管及服务协调（类似K8S）;
20. 系统级的资源监控；
21. 。。。

# 下载源代码
git checkout https://github.com/mynewworldyyl/jmicro.git

# 构建JMicro全部依赖包
进入到下载的源码目录，执行如下命令：

mvn clean install  -Dmaven.test.skip=true

# 启动Zookeeper，端口保持默认值2181
 参考：https://zookeeper.apache.org/doc/r3.4.13/zookeeperStarted.html
 
# 启动Redis，端口保持默认值6379
 Linux: https://redis.io/download
 
 Windows: https://github.com/MicrosoftArchive/redis/releases
 
# 构建运行服务提供方


进入provider目录

cd ${SRC_ROOT}\example\example.provider

构建运行包

mvn clean install -Pwith-deps -Dmaven.test.skip=true

运行服务

java -javaagent:${SRC_ROOT}/jmicro.agent/target/jmicro.agent-0.0.2-SNAPSHOT.jar -jar target/expjmicro.example.provider-0.0.2-SNAPSHOT-with-core.jar



# 构建运行服务消费方

打开一个新命令行窗口

进入comsumer目录

cd ${SRC_ROOT}\example\example.comsumer

构建运行包，-Pwith-deps表示构建可运行Jar，包括全部依赖，类似SpringBoot可运行Jar

mvn clean install -Pwith-deps  -Dmaven.test.skip=true

运行服务
java -javaagent:${SRC_ROOT}/jmicro.agent/target/jmicro.agent-0.0.2-SNAPSHOT.jar -jar target/expjmicro.example.comsumer-0.0.2-SNAPSHOT-with-core.jar

最后一行看到如下输出 ，即服务提供方返回的消息

Server say hello to: Hello JMicro

## 疑难解答

1 如果碰到编码解码，序列化相关错误，首先检查服务启动命令是否缺失如下参数
-javaagent:$JMICRO_ROOT/agent/target/jmicro-agent-0.0.2-SNAPSHOT.jar





