# webdav-teambition
本项目实现了阿里Teambition网盘的webdav协议，只需要简单的配置一下cookies，就可以让Teambition变身为webdav协议的文件服务器。
基于此，你可以把Teambition挂载为Windows、Linux、Mac系统的磁盘，可以通过NAS系统做文件管理或文件同步，更多玩法等你挖掘
# 如何使用
## Jar包运行
[点击下载Jar包](https://github.com/zxbu/webdav-teambition/releases/download/0.0.1/webdav-teambition-0.0.1-SNAPSHOT.jar)
```bash
java -jar webdav-teambition-0.0.1-SNAPSHOT.jar --teambition.cookies="your cookies here"
```
## 容器运行
// todo 
```bash

```

# 参数说明
```bash
--teambition.cookies 
    必填，teambition官网cookies
```

# Chrome获取Cookies方式
1. 用Chrome打开官网，保证是登录状态 https://www.teambition.com/
2. 参考这篇百度经验 https://jingyan.baidu.com/article/0aa2237505193488cd0d647f.html
3. 要确保完整复制看到的cookies，并在启动参数时左右加上双引号
