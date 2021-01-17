# webdav-teambition
本项目实现了阿里Teambition网盘的webdav协议，只需要简单的配置一下cookies，就可以让Teambition变身为webdav协议的文件服务器。
基于此，你可以把Teambition挂载为Windows、Linux、Mac系统的磁盘，可以通过NAS系统做文件管理或文件同步，更多玩法等你挖掘
# 如何使用
## Jar包运行
[点击下载Jar包](https://github.com/zxbu/webdav-teambition/releases/download/0.0.2/webdav-teambition-0.0.2-SNAPSHOT.jar)
```bash
java -jar webdav-teambition-0.0.2-SNAPSHOT.jar --teambition.cookies="your cookies here"
```
## 容器运行
```bash
docker run -d --name=webdav-teambition -p 8080:8080 zx5253/webdav-teambition:latest --teambition.cookies="your cookies here"
```

# 参数说明
```bash
--teambition.cookies 
    必填，teambition官网cookies
--server.port
    非必填，服务器端口号，默认为8080
```

# Chrome获取Cookies方式
1. 用Chrome打开官网，保证是登录状态 https://www.teambition.com/
2. 参考这篇百度经验 https://jingyan.baidu.com/article/0aa2237505193488cd0d647f.html
3. 要确保完整复制看到的cookies，并在启动参数时左右加上双引号

# 功能说明
## 支持的功能
1. 查看文件夹、查看文件
2. 文件移动目录
3. 文件重命名
4. 文件下载
5. 文件删除
6. 文件上传（支持大文件自动分批上传）
## 暂不支持的功能
1. 权限校验
2. 移动文件到其他目录的同时，修改文件名。比如 /a.zip 移动到 /b/a1.zip，是不支持的
3. 文件上传断点续传
4. 文件下载断点续传
5. 同级目录下文件数量不能超过10000个（建议不超过100，否则性能比较差）
