# 测试说明

## 项目测试步骤

### 1. 环境准备
- 确保已安装Android Studio
- 确保有Android设备或模拟器（API 21+）

### 2. 项目导入
1. 打开Android Studio
2. 选择"Open an existing Android Studio project"
3. 选择项目根目录
4. 等待Gradle同步完成

### 3. 权限配置
- 首次运行时会请求存储权限
- 点击"授予权限"按钮授予必要权限

### 4. 功能测试
1. 点击"打开网页"按钮
2. 等待网页加载完成
3. 在网页中查找下载按钮
4. 点击下载按钮测试下载功能
5. 观察下载进度显示
6. 检查文件是否下载到指定目录

### 5. 预期结果
- 网页能正常加载
- 下载按钮点击能被正确拦截
- 显示下载进度和文件名
- 文件成功下载到Downloads/BlobDownloads/目录

### 6. 故障排除
- 如果网页无法加载，检查网络连接
- 如果下载失败，检查存储权限
- 如果进度不显示，检查JavaScript注入是否成功

## 日志查看
使用Android Studio的Logcat查看详细日志：
- 过滤标签：MainActivity, DownloadManager, WebAppInterface
- 查看WebView加载和JavaScript执行日志
- 查看下载进度和状态变化
