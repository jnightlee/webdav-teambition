# webdav-teambition
本项目实现了阿里Teambition网盘的webdav协议，只需要简单的配置一下cookies，就可以让Teambition变身为webdav协议的文件服务器。
基于此，你可以把Teambition挂载为Windows、Linux、Mac系统的磁盘，可以通过NAS系统做文件管理或文件同步，更多玩法等你挖掘
# 如何使用
支持两种登录方式：指定cookies或账号密码方式。具体看参数说明
## Jar包运行
[点击下载Jar包](https://github.com/zxbu/webdav-teambition/releases/latest)
> 建议自己下载源码编译，以获得最新代码
```bash
java -jar webdav-teambition.jar --teambition.userName="your userName" --teambition.password="your password"
```
## 容器运行
```bash
docker run -d --name=webdav-teambition -p 8080:8080 zx5253/webdav-teambition:latest  --teambition.userName="your userName" --teambition.password="your password"
```

# 参数说明
```bash
--teambition.cookies 
    如果采用指定cookies登录，此项必填，teambition官网cookies
--teambition.userName 
    如果采用账号密码登录，此项必填，你的手机号
--teambition.password 
    如果采用账号密码登录，此项必填，你的明文密码
--server.port
    非必填，服务器端口号，默认为8080
```
# QQ群
> 群号：789738128

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

# 小白教程
## 如何在群辉上挂载阿里Teambition或做同步盘
https://www.yuque.com/docs/share/e4271a8a-e300-48d6-b14c-b1ab9d11a1d9
