# Blob Download Android App

这是一个Android应用程序，用于在网页中打开指定地址并实现blob文件下载功能。

## 功能特性

- 在WebView中打开指定网页
- 自动检测并监听下载按钮点击
- 支持blob URL和直接文件链接下载
- 实时显示下载进度
- 获取下载文件名
- 监听下载开始和结束事件
- 将文件下载到指定目录（Downloads/BlobDownloads/）

## 项目结构

```
app/
├── src/main/
│   ├── java/com/example/blobdownload/
│   │   ├── MainActivity.java          # 主活动类
│   │   ├── DownloadManager.java       # 下载管理器
│   │   └── WebAppInterface.java       # WebView JavaScript接口
│   ├── res/
│   │   ├── layout/activity_main.xml   # 主界面布局
│   │   ├── values/                    # 资源文件
│   │   └── xml/                       # 配置文件
│   └── AndroidManifest.xml            # 应用清单
├── build.gradle                       # 应用级构建配置
└── proguard-rules.pro                 # 混淆规则

build.gradle                           # 项目级构建配置
settings.gradle                        # 项目设置
gradle.properties                      # Gradle属性
```

## 使用方法

1. 点击"打开网页"按钮加载指定网页
2. 应用会自动注入JavaScript代码监听下载按钮
3. 点击网页中的下载按钮时，应用会拦截并开始下载
4. 下载过程中会显示进度条和文件名
5. 下载完成后文件会保存到Downloads/BlobDownloads/目录

## 权限要求

- `INTERNET`: 网络访问权限
- `WRITE_EXTERNAL_STORAGE`: 写入外部存储权限
- `READ_EXTERNAL_STORAGE`: 读取外部存储权限
- `ACCESS_NETWORK_STATE`: 网络状态访问权限

## 技术实现

- 使用WebView加载网页
- JavaScript接口实现WebView和Android通信
- AsyncTask处理文件下载
- 进度监听和状态回调
- 文件流读写和进度计算

## 构建和运行

1. 确保已安装Android Studio
2. 打开项目
3. 同步Gradle依赖
4. 连接Android设备或启动模拟器
5. 点击运行按钮

## 注意事项

- 需要Android 5.0 (API 21) 或更高版本
- 首次运行需要授予存储权限
- 确保设备有足够的存储空间
- 网络连接稳定以确保下载成功
# BlobDownload
