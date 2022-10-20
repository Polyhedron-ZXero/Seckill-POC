# 电商秒杀系统 (POC)

## 框架 & 工具

- Spring Boot
- Spring MVC
- Redis
- Sentinel
- RocketMQ
- MyBatis
- MySQL

## 项目运行指南

### 启动 MySQL 服务

使用管理员模式运行：
```shell
net start MySQL57
```
（版本 5.7.x 默认服务名称为 MySQL57）

### 启动 Redis

运行 `redis-server.exe`

### 启动 RocketMQ

1. 设置 `JAVA_HOME`、`CLASSPATH`，和 `ROCKETMQ_HOME` 系统变量
2. 打开 PowerShell 或 CMD，输入
    ```shell
    mqnamesrv
    ```
3. 另打开一个窗口，输入
    ```shell
    mqbroker -n 127.0.0.1:9876 autoCreateTopicEnable=true
    ```
    如果 `ROCKETMQ_HOME` 或 `JAVA_HOME` 路径中有空格，可能会找不到路径，解决方法：
    1. 修改 runbroker.cmd，在最后 set 命令中的 `%CLASSPATH%` 周围加上双引号
        ```shell
        set "JAVA_OPT=%JAVA_OPT% -cp "%CLASSPATH%""
        ```

### 打包

运行命令：
```shell
mvn package -DskipTests
```
或打开 Maven 选项卡 → Seckill → 生命周期，点击【切换“跳过测试”模式】按钮，然后双击 package

---

### Reference Documentation

For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.7.4/maven-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/2.7.4/maven-plugin/reference/html/#build-image)
* [Spring Web](https://docs.spring.io/spring-boot/docs/2.7.4/reference/htmlsingle/#web)

### Guides

The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
