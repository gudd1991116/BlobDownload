# Blob Download Android App

一个专门用于处理网页中Blob URL下载的Android应用程序，支持确认下载对话框、文件名捕获、多种文件格式下载等功能。

## 📋 项目概述

该项目通过WebView加载网页，使用JavaScript注入技术拦截Blob URL创建和下载事件，实现从网页中安全下载Blob文件到Android设备。项目采用模块化设计，包含确认下载、文件类型识别、权限管理等完整功能。

## 🚀 核心功能

### 主要特性
- **Blob URL拦截** - 自动拦截网页中的Blob URL创建和下载请求
- **确认下载对话框** - 用户友好的下载确认界面，显示文件详细信息
- **文件名捕获** - 智能捕获原始文件名，支持多种文件格式
- **多格式支持** - 支持PDF、Office文档、图片、视频、音频等常见格式
- **权限管理** - 完整的存储权限请求和管理机制
- **通知系统** - 下载完成后显示系统通知
- **CORS处理** - 自动处理跨域请求问题

### 技术亮点
- JavaScript注入技术实现网页交互
- Base64编码/解码处理二进制数据
- FileProvider安全文件共享
- 多线程异步下载处理
- 智能MIME类型识别

## 🏗️ 项目架构

### 核心类结构
```
com.example.blobdownload/
├── MainActivityTest2.java          # 主活动类 - WebView管理和用户交互
├── BlobDownloadHelper.java         # Blob处理核心 - JavaScript代码生成
├── FileSaveHelper.java             # 文件保存管理 - 存储和权限处理
├── MimeTypeHelper.java             # MIME类型识别 - 文件格式映射
└── DownloadHelper.java             # 下载辅助类 - 下载流程控制
```

### 资源文件结构
```
res/
├── layout/
│   ├── activity_main.xml           # 主界面布局
│   └── dialog_confirm_download.xml # 确认下载对话框
├── values/
│   ├── colors.xml                  # 颜色资源
│   ├── strings.xml                 # 字符串资源
│   └── themes.xml                  # 主题样式
└── xml/
    └── fp_paths.xml                # FileProvider路径配置
```

## 🔄 处理流程详解

### 1. 应用启动流程
```
MainActivityTest2.onCreate()
├── setupWebView()                  # 配置WebView设置
├── 注入JavaScript代码
│   ├── injectBlobCacheCode()       # 注入Blob缓存代码
│   └── injectFileNameCaptureCode() # 注入文件名捕获代码
└── 加载目标网页
```

### 2. Blob URL拦截流程
```
用户点击下载链接
├── DownloadListener.onDownloadStart()
├── downBlobUrl()                   # 处理Blob URL
├── JavaScript获取文件信息
│   ├── 从blobCache获取Blob对象
│   ├── 从blobFileNameMap获取文件名
│   └── 获取文件大小和MIME类型
└── 调用Android.showDownloadConfirmDialog()
```

### 3. 确认下载流程
```
显示确认对话框
├── showConfirmDownloadDialog()
├── 用户选择确认/取消
├── 如果确认 → performActualDownload()
├── JavaScript读取Blob数据
│   ├── FileReader.readAsDataURL()
│   └── 转换为Base64字符串
└── 调用Android下载方法
```

### 4. 文件保存流程
```
接收Base64数据
├── FileSaveHelper.convertBase64StringToFileAndStoreItWithName()
├── Base64解码
├── 文件写入Downloads目录
├── 创建系统通知
└── 显示下载完成提示
```

## 💻 代码流程分析

### JavaScript注入机制

#### 1. Blob缓存代码注入
```javascript
// 拦截URL.createObjectURL调用
var originalCreateObjectURL = URL.createObjectURL;
URL.createObjectURL = function(blob) {
    var url = originalCreateObjectURL.call(this, blob);
    window.blobCache[url] = blob;  // 缓存Blob对象
    return url;
};
```

#### 2. 文件名捕获代码注入
```javascript
// 拦截<a>标签创建和点击事件
document.createElement = function(tagName) {
    var element = originalCreateElement.call(this, tagName);
    if (tagName.toLowerCase() === 'a') {
        element.click = function() {
            var href = this.getAttribute('href');
            var download = this.getAttribute('download');
            if (href && href.startsWith('blob:') && download) {
                window.blobFileNameMap[href] = download;  // 映射文件名
            }
            return originalClick.call(this);
        };
    }
    return element;
};
```

### Android Native层处理

#### 1. WebView配置
```java
// 启用JavaScript和DOM存储
webSettings.setJavaScriptEnabled(true);
webSettings.setDomStorageEnabled(true);
webSettings.setAllowFileAccess(true);
webSettings.setAllowUniversalAccessFromFileURLs(true);

// 设置下载监听器
webView.setDownloadListener(new DownloadListener() {
    @Override
    public void onDownloadStart(String url, String userAgent, 
                               String contentDisposition, String mimeType, 
                               long contentLength) {
        if (url.startsWith("blob:")) {
            downBlobUrl(url);
        }
    }
});
```

#### 2. JavaScript接口
```java
@JavascriptInterface
public void showDownloadConfirmDialog(String url, String fileName, 
                                    long fileSize, String mimeType) {
    runOnUiThread(() -> {
        showConfirmDownloadDialog(url, fileName, fileSize, mimeType);
    });
}

@JavascriptInterface
public void downloadFileWithName(String base64Data, String fileName) {
    FileSaveHelper.getInstance().convertBase64StringToFileAndStoreItWithName(
        context, base64Data, fileName, fileMimeType);
}
```

## 🔐 权限管理

### 必需权限
```xml
<!-- 网络权限 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- 存储权限 -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

<!-- 通知权限 -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Android 11+ 管理外部存储权限 -->
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
```

### 权限请求处理
```java
// 运行时权限检查
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    if (activity.checkSelfPermission(WRITE_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED) {
        activity.requestPermissions(
            new String[]{WRITE_EXTERNAL_STORAGE}, 
            REQUEST_STORAGE_PERMISSION);
    }
}
```

## 📁 文件下载转换

### Base64处理流程
```java
// 1. 接收JavaScript传递的Base64数据
public void downloadFileWithName(String base64Data, String fileName) {
    // 2. 清理Base64数据（移除data:前缀）
    String base64Data = base64PDf;
    if (base64Data.startsWith("data:")) {
        int commaIndex = base64Data.indexOf(',');
        if (commaIndex > 0) {
            base64Data = base64Data.substring(commaIndex + 1);
        }
    }
    
    // 3. Base64解码
    byte[] fileBytes = Base64.decode(base64Data, 0);
    
    // 4. 写入文件
    FileOutputStream os = new FileOutputStream(filePath);
    os.write(fileBytes);
    os.close();
}
```

### MIME类型识别
```java
// 根据文件扩展名确定MIME类型
public static String getMimeTypeFromExtension(String extension) {
    switch (extension.toLowerCase()) {
        case "pdf": return "application/pdf";
        case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        case "jpg": case "jpeg": return "image/jpeg";
        case "mp4": return "video/mp4";
        // ... 更多格式支持
        default: return "application/octet-stream";
    }
}
```

## 🛠️ 使用方法

### 1. 环境要求
- Android Studio 4.0+
- Android SDK API 21+ (Android 5.0)
- Java 8+

### 2. 构建步骤
```bash
# 1. 克隆项目
git clone [repository-url]

# 2. 打开Android Studio
# 3. 同步Gradle依赖
# 4. 连接Android设备或启动模拟器
# 5. 点击运行按钮
```

### 3. 使用流程
1. **启动应用** - 应用自动加载目标网页
2. **等待注入** - 系统自动注入JavaScript代码
3. **点击下载** - 在网页中点击任何下载链接
4. **确认下载** - 在弹出的对话框中查看文件信息并确认
5. **完成下载** - 文件保存到Downloads目录，显示通知

### 4. 自定义配置
```java
// 修改目标URL
private static final String TARGET_URL = "https://your-target-website.com";

// 修改保存路径
final File dwldsPath = new File(Environment.getExternalStoragePublicDirectory(
    Environment.DIRECTORY_DOWNLOADS) + "/" + finalFileName);
```

## 🔧 技术知识点

### 1. WebView与JavaScript交互
- **addJavascriptInterface()** - 注册Java对象供JavaScript调用
- **evaluateJavascript()** - 执行JavaScript代码并获取返回值
- **@JavascriptInterface** - 标记可被JavaScript调用的方法

### 2. Blob URL处理
- **URL.createObjectURL()** - 创建Blob URL
- **FileReader API** - 读取Blob数据
- **Base64编码** - 将二进制数据转换为文本格式

### 3. 文件系统操作
- **FileProvider** - 安全的文件共享机制
- **Environment.getExternalStoragePublicDirectory()** - 获取公共存储目录
- **MimeTypeMap** - Android MIME类型映射

### 4. 权限管理
- **运行时权限** - Android 6.0+动态权限请求
- **作用域存储** - Android 10+存储访问限制
- **权限检查** - checkSelfPermission()方法

### 5. 通知系统
- **NotificationChannel** - Android 8.0+通知渠道
- **PendingIntent** - 延迟意图，用于通知点击
- **FileProvider** - 安全文件URI生成

## ⚠️ 注意事项

### 兼容性考虑
- **Android版本** - 最低支持API 21 (Android 5.0)
- **WebView版本** - 需要支持JavaScript和DOM存储
- **存储权限** - Android 11+需要特殊处理

### 性能优化
- **内存管理** - 及时清理Blob缓存
- **异步处理** - 文件操作在后台线程执行
- **错误处理** - 完善的异常捕获和用户提示

### 安全考虑
- **文件验证** - 检查文件大小和类型
- **路径安全** - 防止路径遍历攻击
- **权限最小化** - 只请求必要的权限

## 🐛 常见问题

### 1. 下载失败
- 检查网络连接
- 确认存储权限已授予
- 查看Logcat错误信息

### 2. 文件名乱码
- 确保网页使用UTF-8编码
- 检查文件名清理逻辑

### 3. 文件无法打开
- 确认MIME类型识别正确
- 检查FileProvider配置
- 验证文件完整性

## 📈 扩展功能

### 可扩展特性
- **批量下载** - 支持多文件同时下载
- **下载队列** - 管理下载任务队列
- **断点续传** - 支持大文件断点续传
- **云存储集成** - 支持上传到云存储服务
- **下载历史** - 记录下载历史记录

## 📄 许可证

本项目采用MIT许可证，详情请查看LICENSE文件。

## 🤝 贡献指南

欢迎提交Issue和Pull Request来改进项目。

---

**开发者**: gudd  
**创建时间**: 2025年9月  
**最后更新**: 2025年1月