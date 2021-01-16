# webdav-teambition
本项目实现了阿里Teambition网盘的webdav协议，只需要简单的配置一下cookies，就可以让Teambition变身为webdav协议的文件服务器。
基于此，你可以把Teambition挂载为Windows、Linux、Mac系统的磁盘，可以通过NAS系统做文件管理或文件同步，更多玩法等你挖掘
# 如何使用
## Jar包运行
点击下载Jar包
```bash
mvn -DskipTests=true package
java -jar target/webdav-teambition-0.0.1-SNAPSHOT.jar --teambition.cookies="your cookies here"
```
## 容器运行
```bash
```
