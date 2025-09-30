package com.example.blobdownload;

import static com.example.blobdownload.FileSaveHelper.REQUEST_STORAGE_PERMISSION;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;

public class MainActivityTest2OK extends AppCompatActivity {
    private static final String TAG = "MainActivity2";
    private static final String TARGET_URL = "https://i.kunlunjyk.com/?file=7aI1R25317000001056922006.zip";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_RESULT_CODE = 1;
    private static final int DOWNLOAD_PERMISSION_REQUEST_CODE = 2;
    private long downloadId;
    private BroadcastReceiver downloadReceiver;

    private JavaScriptInterface javascriptInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        setupWebView();

        // 加载你的测试网页
        webView.loadUrl(TARGET_URL);

        findViewById(R.id.btnGrantPermission).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileSaveHelper.getInstance().saveFileWithPermissionCheck(MainActivityTest2OK.this, null, "", "");
            }
        });
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);

        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        // 如果需要，允许跨域访问
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowFileAccessFromFileURLs(true);

        // 缓存和存储
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        // 其他优化设置
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // 设置WebChromeClient
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("WebViewConsole", consoleMessage.message() + " -- Line: "
                        + consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
                return true;
            }

            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                MainActivityTest2OK.this.filePathCallback = filePathCallback;
                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_RESULT_CODE);
                } catch (ActivityNotFoundException e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        // 设置WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            private WebResourceResponse handleCorsPreflightRequest() {
                try {
                    return new WebResourceResponse(
                            "text/plain",
                            "UTF-8",
                            200,
                            "OK",
                            Collections.singletonMap(
                                    "Access-Control-Allow-Origin",
                                    "*"
                            ),
                            new ByteArrayInputStream("".getBytes())
                    );
                } catch (Exception e) {
                    return null;
                }
            }

            private WebResourceResponse handleBlobUrlRequest(String blobUrl) {
                Log.d(TAG, "开始处理Blob URL: " + blobUrl);

                // 由于Blob URL在Java层面无法直接访问，我们需要通过JavaScript来获取数据
                // 这里返回一个特殊的响应，让JavaScript处理
                try {
                    // 创建一个特殊的响应，告诉JavaScript需要处理这个Blob URL
                    String jsCode = "javascript: (function() {" +
                            "console.log('Java拦截到Blob URL: " + blobUrl + "');" +
                            "if (typeof Android !== 'undefined' && typeof Android.handleBlobUrl === 'function') {" +
                            "    Android.handleBlobUrl('" + blobUrl + "');" +
                            "} else {" +
                            "    console.error('Android.handleBlobUrl方法未找到');" +
                            "}" +
                            "})();";

                    return new WebResourceResponse(
                            "text/html",
                            "UTF-8",
                            200,
                            "OK",
                            Collections.singletonMap("Content-Type", "text/html"),
                            new ByteArrayInputStream(jsCode.getBytes())
                    );
                } catch (Exception e) {
                    Log.e(TAG, "处理Blob URL时出错", e);
                    return null;
                }
            }


            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d(TAG, "拦截请求: " + url);

                // 处理CORS预检请求
                if ("OPTIONS".equals(request.getMethod())) {
                    return handleCorsPreflightRequest();
                }

                // 处理Blob URL请求
                if (url.startsWith("blob:")) {
                    Log.d(TAG, "检测到Blob URL请求: " + url);
                    return handleBlobUrlRequest(url);
                }

                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("blob:")) {
//                    downBlobUrl(url);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "页面加载结束，开始注入Blob缓存代码： " + url);

                // 延迟注入，确保页面完全加载
                new Handler().postDelayed(() -> {
                    injectBlobCacheCode();
                    // 注入文件下载工具函数
                    injectDownloadHelperFunctions();
                    // 注入文件名捕获代码
                    injectFileNameCaptureCode();
                    // 测试JavaScript注入是否成功
                    testJavaScriptInjection();
                    // 测试文件名捕获功能
                    testFileNameCapture();
                }, 1000);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Log.d(TAG, "HTTP错误: " + error.getErrorCode());
                }
            }
        });

        // 设置DownloadListener - 这是关键！
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

        webView.getSettings().setDefaultTextEncodingName("utf-8");
        // 添加JavaScript接口
        javascriptInterface = new JavaScriptInterface(this);
        webView.addJavascriptInterface(javascriptInterface, "Android");
    }

    private void injectBlobCacheCode() {
        // Blob缓存代码 + 文件名捕获
        String jsCode = "console.log('=== 开始注入Blob缓存代码 ===');" +
                "if (!window.blobCache) {" +
                "    window.blobCache = {};" +
                "    console.log('创建blobCache对象');" +
                "}" +
                "if (!window.blobFileNameMap) {" +
                "    window.blobFileNameMap = {};" +
                "    console.log('创建blobFileNameMap对象');" +
                "}" +
                "var originalCreateObjectURL = URL.createObjectURL;" +
                "URL.createObjectURL = function(blob) {" +
                "    console.log('createObjectURL被调用，Blob类型:', typeof blob);" +
                "    var url = originalCreateObjectURL.call(this, blob);" +
                "    console.log('生成的URL:', url);" +
                "    window.blobCache[url] = blob;" +
                "    console.log('缓存Blob URL:', url, 'Blob大小:', blob.size);" +
                "    return url;" +
                "};" +
                "console.log('Blob缓存代码注入完成');";

        Log.d(TAG, "注入Blob缓存代码");
        webView.evaluateJavascript(jsCode, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.d(TAG, "Blob缓存代码注入结果: " + value);
            }
        });

        // 文件名捕获代码已在onPageFinished中调用，这里不需要重复调用
    }

    private void testJavaScriptInjection() {
        String testCode = "console.log('=== JavaScript注入测试 ===');" +
                "console.log('blobCache存在?', typeof window.blobCache !== 'undefined');" +
                "console.log('blobFileNameMap存在?', typeof window.blobFileNameMap !== 'undefined');" +
                "console.log('createObjectURL被重写?', URL.createObjectURL.toString().includes('blobCache'));" +
                "if (window.blobCache) {" +
                "    console.log('当前缓存数量:', Object.keys(window.blobCache).length);" +
                "    console.log('缓存内容:', Object.keys(window.blobCache));" +
                "} else {" +
                "    console.log('blobCache不存在');" +
                "}";

        webView.evaluateJavascript(testCode, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.d(TAG, "JavaScript注入测试结果: " + value);
            }
        });

        // 延迟测试Blob创建
        new Handler().postDelayed(() -> {
            testBlobCreation();
        }, 3000);
    }

    private void testBlobCreation() {
        String testBlobCode = "console.log('=== 测试Blob创建 ===');" +
                "try {" +
                "    var testBlob = new Blob(['Hello World'], {type: 'text/plain'});" +
                "    console.log('创建测试Blob成功，大小:', testBlob.size);" +
                "    var testUrl = URL.createObjectURL(testBlob);" +
                "    console.log('创建测试URL:', testUrl);" +
                "    console.log('缓存中是否有测试URL?', window.blobCache && window.blobCache[testUrl] ? '是' : '否');" +
                "    if (window.blobCache && window.blobCache[testUrl]) {" +
                "        console.log('测试URL的Blob大小:', window.blobCache[testUrl].size);" +
                "    }" +
                "} catch(e) {" +
                "    console.error('测试Blob创建失败:', e.message);" +
                "}";

        Log.d(TAG, "测试Blob创建");
        webView.evaluateJavascript(testBlobCode, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.d(TAG, "Blob创建测试结果: " + value);
            }
        });
    }

    private void injectDownloadHelperFunctions() {
        String helperFunctions = "(function() {" +
                "console.log('=== 注入文件下载工具函数 ===');" +
                "try {" +
                "    // 创建全局下载工具函数" +
                "    window.downloadFile = function(blobUrl, fileType) {" +
                "        console.log('调用下载函数 - URL:', blobUrl, '类型:', fileType);" +
                "        if (typeof Android === 'undefined') {" +
                "            console.error('Android接口未找到');" +
                "            return;" +
                "        }" +
                "        " +
                "        // 根据文件类型调用不同的Android方法" +
                "        if (fileType === 'pdf') {" +
                "            downloadBlobAsPdf(blobUrl);" +
                "        } else if (fileType === 'ofd') {" +
                "            downloadBlobAsOfd(blobUrl);" +
                "        } else if (fileType === 'xml') {" +
                "            downloadBlobAsXml(blobUrl);" +
                "        } else if (fileType && fileType.startsWith('image/')) {" +
                "            var imageType = fileType.split('/')[1];" +
                "            downloadBlobAsImage(blobUrl, imageType);" +
                "        } else {" +
                "            downloadBlobGeneric(blobUrl, fileType);" +
                "        }" +
                "    };" +
                "    " +
                "    // PDF下载函数" +
                "    window.downloadPdf = function(blobUrl) {" +
                "        downloadFile(blobUrl, 'pdf');" +
                "    };" +
                "    " +
                "    // OFD下载函数" +
                "    window.downloadOfd = function(blobUrl) {" +
                "        downloadFile(blobUrl, 'ofd');" +
                "    };" +
                "    " +
                "    // XML下载函数" +
                "    window.downloadXml = function(blobUrl) {" +
                "        downloadFile(blobUrl, 'xml');" +
                "    };" +
                "    " +
                "    // 图片下载函数" +
                "    window.downloadImage = function(blobUrl, imageType) {" +
                "        downloadFile(blobUrl, 'image/' + imageType);" +
                "    };" +
                "    " +
                "    // 通用下载函数" +
                "    function downloadBlobGeneric(blobUrl, fileType) {" +
                "        console.log('通用下载 - URL:', blobUrl, '类型:', fileType);" +
                "        if (window.blobCache && window.blobCache[blobUrl]) {" +
                "            var blob = window.blobCache[blobUrl];" +
                "            var reader = new FileReader();" +
                "            reader.onloadend = function() {" +
                "                if (fileType) {" +
                "                    Android.getBase64FromBlobDataWithType(reader.result, fileType);" +
                "                } else {" +
                "                    Android.getBase64FromBlobData(reader.result);" +
                "                }" +
                "            };" +
                "            reader.readAsDataURL(blob);" +
                "        } else {" +
                "            console.error('Blob URL未在缓存中找到:', blobUrl);" +
                "        }" +
                "    }" +
                "    " +
                "    // PDF下载函数" +
                "    function downloadBlobAsPdf(blobUrl) {" +
                "        console.log('PDF下载 - URL:', blobUrl);" +
                "        if (window.blobCache && window.blobCache[blobUrl]) {" +
                "            var blob = window.blobCache[blobUrl];" +
                "            var reader = new FileReader();" +
                "            reader.onloadend = function() {" +
                "                Android.downloadPdf(reader.result);" +
                "            };" +
                "            reader.readAsDataURL(blob);" +
                "        } else {" +
                "            console.error('PDF Blob URL未在缓存中找到:', blobUrl);" +
                "        }" +
                "    }" +
                "    " +
                "    // OFD下载函数" +
                "    function downloadBlobAsOfd(blobUrl) {" +
                "        console.log('OFD下载 - URL:', blobUrl);" +
                "        if (window.blobCache && window.blobCache[blobUrl]) {" +
                "            var blob = window.blobCache[blobUrl];" +
                "            var reader = new FileReader();" +
                "            reader.onloadend = function() {" +
                "                Android.downloadOfd(reader.result);" +
                "            };" +
                "            reader.readAsDataURL(blob);" +
                "        } else {" +
                "            console.error('OFD Blob URL未在缓存中找到:', blobUrl);" +
                "        }" +
                "    }" +
                "    " +
                "    // XML下载函数" +
                "    function downloadBlobAsXml(blobUrl) {" +
                "        console.log('XML下载 - URL:', blobUrl);" +
                "        if (window.blobCache && window.blobCache[blobUrl]) {" +
                "            var blob = window.blobCache[blobUrl];" +
                "            var reader = new FileReader();" +
                "            reader.onloadend = function() {" +
                "                Android.downloadXml(reader.result);" +
                "            };" +
                "            reader.readAsDataURL(blob);" +
                "        } else {" +
                "            console.error('XML Blob URL未在缓存中找到:', blobUrl);" +
                "        }" +
                "    }" +
                "    " +
                "    // 图片下载函数" +
                "    function downloadBlobAsImage(blobUrl, imageType) {" +
                "        console.log('图片下载 - URL:', blobUrl, '类型:', imageType);" +
                "        if (window.blobCache && window.blobCache[blobUrl]) {" +
                "            var blob = window.blobCache[blobUrl];" +
                "            var reader = new FileReader();" +
                "            reader.onloadend = function() {" +
                "                Android.downloadImage(reader.result, imageType);" +
                "            };" +
                "            reader.readAsDataURL(blob);" +
                "        } else {" +
                "            console.error('图片 Blob URL未在缓存中找到:', blobUrl);" +
                "        }" +
                "    }" +
                "    " +
                "    console.log('✅ 文件下载工具函数注入完成');" +
                "    console.log('可用函数: downloadFile(blobUrl, fileType), downloadPdf(blobUrl), downloadOfd(blobUrl), downloadXml(blobUrl), downloadImage(blobUrl, imageType)');" +
                "} catch(e) {" +
                "    console.error('注入文件下载工具函数时出错:', e.message);" +
                "}" +
                "})();";

        Log.d(TAG, "注入文件下载工具函数");
        webView.evaluateJavascript(helperFunctions, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.d(TAG, "文件下载工具函数注入结果: " + value);
            }
        });
    }

    private void injectFileNameCaptureCode() {
        // 正确的文件名捕获代码 - 拦截click事件
        String fileNameCaptureCode = "console.log('=== 注入文件名捕获代码 ===');" +
                "console.log('开始注入文件名捕获代码');" +
                "try {" +
                "    var originalCreateElement = document.createElement;" +
                "    document.createElement = function(tagName) {" +
                "        var element = originalCreateElement.call(this, tagName);" +
                "        if (tagName.toLowerCase() === 'a') {" +
                "            console.log('创建了<a>标签');" +
                "            var originalClick = element.click;" +
                "            element.click = function() {" +
                "                var href = this.getAttribute('href');" +
                "                var download = this.getAttribute('download');" +
                "                console.log('点击<a>标签下载:', href, '文件名:', download);" +
                "                if (href && href.startsWith('blob:') && download) {" +
                "                    window.blobFileNameMap[href] = download;" +
                "                    console.log('映射文件名:', href, '->', download);" +
                "                }" +
                "                return originalClick.call(this);" +
                "            };" +
                "        }" +
                "        return element;" +
                "    };" +
                "    console.log('✅ 文件名捕获代码注入完成');" +
                "} catch(e) {" +
                "    console.error('文件名捕获代码注入失败:', e.message);" +
                "}";
        
        Log.d(TAG, "注入文件名捕获代码");
        webView.evaluateJavascript(fileNameCaptureCode, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.d(TAG, "文件名捕获代码注入结果: " + value);
            }
        });
    }
    
    private void testFileNameCapture() {
        // 测试文件名捕获功能
        String testCode = "console.log('=== 测试文件名捕获功能 ===');" +
                "console.log('blobFileNameMap存在?', typeof window.blobFileNameMap !== 'undefined');" +
                "if (window.blobFileNameMap) {" +
                "    console.log('文件名映射数量:', Object.keys(window.blobFileNameMap).length);" +
                "    console.log('文件名映射内容:', window.blobFileNameMap);" +
                "} else {" +
                "    console.log('blobFileNameMap不存在');" +
                "}" +
                "console.log('=== 文件名捕获测试完成 ===');";
        
        Log.d(TAG, "测试文件名捕获功能");
        webView.evaluateJavascript(testCode, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.d(TAG, "文件名捕获测试结果: " + value);
            }
        });
    }

    private void downBlobUrl(String url) {
        if (url.startsWith("blob")) {
            Log.d(TAG, "download start url: " + url);

            // 简化的下载处理 - 先确保基本功能正常
            String downloadCode = "console.log('=== 开始下载处理 ===');" +
                    "var targetUrl = '" + url + "';" +
                    "console.log('目标URL:', targetUrl);" +
                    "if (window.blobCache && window.blobCache[targetUrl]) {" +
                    "    console.log('从缓存中找到Blob');" +
                    "    var blob = window.blobCache[targetUrl];" +
                    "    var reader = new FileReader();" +
                    "    reader.onloadend = function() {" +
                    "        console.log('读取完成，数据长度:', reader.result.length);" +
                    "        var fileName = window.blobFileNameMap && window.blobFileNameMap[targetUrl];" +
                    "        console.log('捕获到的文件名:', fileName);" +
                    "        if (typeof Android !== 'undefined') {" +
                    "            if (fileName) {" +
                    "                Android.downloadFileWithName(reader.result, fileName);" +
                    "                console.log('✅ 使用文件名下载:', fileName);" +
                    "            } else {" +
                    "                Android.getBase64FromBlobData(reader.result);" +
                    "                console.log('✅ 使用通用方法下载');" +
                    "            }" +
                    "        }" +
                    "    };" +
                    "    reader.readAsDataURL(blob);" +
                    "} else {" +
                    "    console.log('缓存中未找到目标Blob');" +
                    "}";

            webView.evaluateJavascript(downloadCode, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    Log.d(TAG, "下载处理结果: " + value);
                }
            });
        }
    }

    // JavaScript接口
    public class JavaScriptInterface {
        private String fileMimeType;
        private final Context context;

        public JavaScriptInterface(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public void getBase64FromBlobData(String base64Data) throws IOException {
            Log.d(TAG, "=== 接收到Blob Base64数据 ===");
            Log.d(TAG, "数据长度: " + (base64Data != null ? base64Data.length() : 0));
            Log.d(TAG, "=====>>> " + base64Data);

            // 从base64Data中提取MIME类型
            if (base64Data != null && base64Data.startsWith("data:")) {
                int semicolonIndex = base64Data.indexOf(';');
                if (semicolonIndex > 5) {
                    fileMimeType = base64Data.substring(5, semicolonIndex);
                    Log.d(TAG, "从Base64数据中提取MIME类型: " + fileMimeType);
                }
            }

            // 如果无法从Base64数据中提取MIME类型，设置默认值
            if (fileMimeType == null || fileMimeType.isEmpty()) {
                fileMimeType = "application/octet-stream"; // 默认二进制文件类型
                Log.d(TAG, "使用默认MIME类型: " + fileMimeType);
            }

            convertBase64StringToFileAndStoreIt(base64Data);
        }

        @JavascriptInterface
        public void getBase64FromBlobDataWithType(String base64Data, String fileType) throws IOException {
            Log.d(TAG, "=== 接收到Blob Base64数据（带文件类型） ===");
            Log.d(TAG, "数据长度: " + (base64Data != null ? base64Data.length() : 0));
            Log.d(TAG, "指定文件类型: " + fileType);
            Log.d(TAG, "=====>>> " + base64Data);

            // 根据指定的文件类型设置MIME类型
            fileMimeType = getMimeTypeFromFileType(fileType);
            Log.d(TAG, "映射的MIME类型: " + fileMimeType);

            convertBase64StringToFileAndStoreIt(base64Data);
        }

        @JavascriptInterface
        public void downloadPdf(String base64Data) throws IOException {
            Log.d(TAG, "=== 下载PDF文件 ===");
            fileMimeType = "application/pdf";
            convertBase64StringToFileAndStoreIt(base64Data);
        }

        @JavascriptInterface
        public void downloadOfd(String base64Data) throws IOException {
            Log.d(TAG, "=== 下载OFD文件 ===");
            fileMimeType = "application/ofd";
            convertBase64StringToFileAndStoreIt(base64Data);
        }

        @JavascriptInterface
        public void downloadXml(String base64Data) throws IOException {
            Log.d(TAG, "=== 下载XML文件 ===");
            fileMimeType = "application/xml";
            convertBase64StringToFileAndStoreIt(base64Data);
        }

        @JavascriptInterface
        public void downloadImage(String base64Data, String imageType) throws IOException {
            Log.d(TAG, "=== 下载图片文件 ===");
            Log.d(TAG, "图片类型: " + imageType);

            // 根据图片类型设置MIME类型
            switch (imageType.toLowerCase()) {
                case "png":
                    fileMimeType = "image/png";
                    break;
                case "jpg":
                case "jpeg":
                    fileMimeType = "image/jpeg";
                    break;
                case "gif":
                    fileMimeType = "image/gif";
                    break;
                case "webp":
                    fileMimeType = "image/webp";
                    break;
                case "svg":
                    fileMimeType = "image/svg+xml";
                    break;
                default:
                    fileMimeType = "image/png";
                    break;
            }

            convertBase64StringToFileAndStoreIt(base64Data);
        }

        @JavascriptInterface
        public void downloadFileWithName(String base64Data, String fileName) throws IOException {
            Log.d(TAG, "=== 下载文件（带文件名） ===");
            Log.d(TAG, "文件名: " + fileName);
            Log.d(TAG, "数据长度: " + (base64Data != null ? base64Data.length() : 0));
            Log.d(TAG, "=====>>> " + base64Data);

            // 从文件名中提取扩展名来确定MIME类型
            if (fileName != null && !fileName.isEmpty()) {
                String extension = getFileExtension(fileName);
                fileMimeType = getMimeTypeFromExtension(extension);
                Log.d(TAG, "从文件名提取的扩展名: " + extension);
                Log.d(TAG, "映射的MIME类型: " + fileMimeType);
            } else {
                // 如果无法从文件名提取，尝试从Base64数据中提取
                if (base64Data != null && base64Data.startsWith("data:")) {
                    int semicolonIndex = base64Data.indexOf(';');
                    if (semicolonIndex > 5) {
                        fileMimeType = base64Data.substring(5, semicolonIndex);
                        Log.d(TAG, "从Base64数据中提取MIME类型: " + fileMimeType);
                    }
                }

                if (fileMimeType == null || fileMimeType.isEmpty()) {
                    fileMimeType = "application/octet-stream";
                    Log.d(TAG, "使用默认MIME类型: " + fileMimeType);
                }
            }

            convertBase64StringToFileAndStoreItWithName(base64Data, fileName);
        }

        private String getFileExtension(String fileName) {
            if (fileName == null || fileName.isEmpty()) {
                return "";
            }
            int lastDotIndex = fileName.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
                return fileName.substring(lastDotIndex + 1).toLowerCase();
            }
            return "";
        }

        private String getMimeTypeFromExtension(String extension) {
            if (extension == null || extension.isEmpty()) {
                return "application/octet-stream";
            }

            switch (extension.toLowerCase()) {
                case "pdf":
                    return "application/pdf";
                case "ofd":
                    return "application/ofd";
                case "xml":
                    return "application/xml";
                case "doc":
                    return "application/msword";
                case "docx":
                    return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                case "xls":
                    return "application/vnd.ms-excel";
                case "xlsx":
                    return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                case "ppt":
                    return "application/vnd.ms-powerpoint";
                case "pptx":
                    return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                case "txt":
                    return "text/plain";
                case "html":
                    return "text/html";
                case "css":
                    return "text/css";
                case "js":
                    return "application/javascript";
                case "json":
                    return "application/json";
                case "zip":
                    return "application/zip";
                case "rar":
                    return "application/x-rar-compressed";
                case "7z":
                    return "application/x-7z-compressed";
                case "png":
                    return "image/png";
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "gif":
                    return "image/gif";
                case "webp":
                    return "image/webp";
                case "svg":
                    return "image/svg+xml";
                case "mp4":
                    return "video/mp4";
                case "avi":
                    return "video/x-msvideo";
                case "mov":
                    return "video/quicktime";
                case "mp3":
                    return "audio/mpeg";
                case "wav":
                    return "audio/wav";
                case "ogg":
                    return "audio/ogg";
                default:
                    return "application/octet-stream";
            }
        }

        private String getMimeTypeFromFileType(String fileType) {
            if (fileType == null || fileType.isEmpty()) {
                return "application/octet-stream";
            }

            switch (fileType.toLowerCase()) {
                case "pdf":
                    return "application/pdf";
                case "ofd":
                    return "application/ofd";
                case "xml":
                    return "application/xml";
                case "doc":
                    return "application/msword";
                case "docx":
                    return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                case "xls":
                    return "application/vnd.ms-excel";
                case "xlsx":
                    return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                case "ppt":
                    return "application/vnd.ms-powerpoint";
                case "pptx":
                    return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                case "txt":
                    return "text/plain";
                case "html":
                    return "text/html";
                case "css":
                    return "text/css";
                case "js":
                    return "application/javascript";
                case "json":
                    return "application/json";
                case "zip":
                    return "application/zip";
                case "rar":
                    return "application/x-rar-compressed";
                case "7z":
                    return "application/x-7z-compressed";
                case "png":
                    return "image/png";
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "gif":
                    return "image/gif";
                case "webp":
                    return "image/webp";
                case "svg":
                    return "image/svg+xml";
                case "mp4":
                    return "video/mp4";
                case "avi":
                    return "video/x-msvideo";
                case "mov":
                    return "video/quicktime";
                case "mp3":
                    return "audio/mpeg";
                case "wav":
                    return "audio/wav";
                case "ogg":
                    return "audio/ogg";
                default:
                    return "application/octet-stream";
            }
        }

        @JavascriptInterface
        public void handleBlobUrl(String blobUrl) {
            Log.d(TAG, "=== 处理Blob URL ===");
            Log.d(TAG, "Blob URL: " + blobUrl);

            // 使用改进的JavaScript代码来处理Blob URL
            String jsCode = getImprovedBlobUrlHandler(blobUrl);

            webView.post(() -> {
                webView.evaluateJavascript(jsCode, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        Log.d(TAG, "Blob URL处理结果: " + value);
                    }
                });
            });
        }

        public String getBase64StringFromBlobUrl(String blobUrl, String mimeType) {
            if (blobUrl.startsWith("blob")) {
                fileMimeType = mimeType;
                // 改进的Blob URL处理方案
                return "javascript: (function() {\n" +
                        "    console.log('开始处理Blob URL:', '" + blobUrl + "');\n" +
                        "    \n" +
                        "    // 检查Blob URL是否有效\n" +
                        "    if (!'" + blobUrl + "' || !'" + blobUrl + "'.startsWith('blob:')) {\n" +
                        "        console.error('无效的Blob URL:', '" + blobUrl + "');\n" +
                        "        return;\n" +
                        "    }\n" +
                        "    \n" +
                        "    // 检查HTTPS/HTTP混合内容问题\n" +
                        "    if ('" + blobUrl + "'.startsWith('blob:http://') && window.location.protocol === 'https:') {\n" +
                        "        console.error('混合内容错误：HTTPS页面不能访问HTTP的Blob URL');\n" +
                        "        return;\n" +
                        "    }\n" +
                        "    \n" +
                        "    console.log('URL协议检查通过 - Blob协议: ' + '" + blobUrl + "'.split(':')[0] + ', 页面协议: ' + window.location.protocol);\n" +
                        "    \n" +
                        "    // 使用XMLHttpRequest替代fetch，提高兼容性\n" +
                        "    function tryXHR() {\n" +
                        "        return new Promise((resolve, reject) => {\n" +
                        "            var xhr = new XMLHttpRequest();\n" +
                        "            xhr.open('GET', '" + blobUrl + "', true);\n" +
                        "            xhr.responseType = 'blob';\n" +
                        "            xhr.timeout = 15000; // 15秒超时\n" +
                        "            \n" +
                        "            xhr.onload = function() {\n" +
                        "                console.log('XHR响应状态 - 状态码: ' + xhr.status + ', 状态文本: ' + xhr.statusText);\n" +
                        "                \n" +
                        "                if (xhr.status === 200) {\n" +
                        "                    var blob = xhr.response;\n" +
                        "                    console.log('成功获取Blob数据 - 大小: ' + blob.size + ' bytes, 类型: ' + blob.type);\n" +
                        "                    \n" +
                        "                    if (blob.size === 0) {\n" +
                        "                        reject(new Error('Blob数据为空，可能URL已失效'));\n" +
                        "                        return;\n" +
                        "                    }\n" +
                        "                    \n" +
                        "                    var reader = new FileReader();\n" +
                        "                    reader.onloadend = function() {\n" +
                        "                        console.log('FileReader读取完成，数据长度: ' + reader.result.length);\n" +
                        "                        resolve(reader.result);\n" +
                        "                    };\n" +
                        "                    reader.onerror = function() {\n" +
                        "                        reject(new Error('FileReader读取失败'));\n" +
                        "                    };\n" +
                        "                    reader.readAsDataURL(blob);\n" +
                        "                } else {\n" +
                        "                    reject(new Error('XHR请求失败，状态码: ' + xhr.status));\n" +
                        "                }\n" +
                        "            };\n" +
                        "            \n" +
                        "            xhr.onerror = function() {\n" +
                        "                console.error('XHR请求发生错误');\n" +
                        "                reject(new Error('XHR请求失败'));\n" +
                        "            };\n" +
                        "            \n" +
                        "            xhr.ontimeout = function() {\n" +
                        "                console.error('XHR请求超时');\n" +
                        "                reject(new Error('请求超时'));\n" +
                        "            };\n" +
                        "            \n" +
                        "            console.log('开始发送XHR请求...');\n" +
                        "            xhr.send();\n" +
                        "        });\n" +
                        "    }\n" +
                        "    \n" +
                        "    // 尝试fetch作为备选方案\n" +
                        "    function tryFetch() {\n" +
                        "        return fetch('" + blobUrl + "', {\n" +
                        "            mode: 'cors',\n" +
                        "            credentials: 'same-origin',\n" +
                        "            cache: 'no-cache'\n" +
                        "        })\n" +
                        "        .then(response => {\n" +
                        "            console.log('Fetch响应状态 - 状态码: ' + response.status + ', 状态文本: ' + response.statusText);\n" +
                        "            \n" +
                        "            if (!response.ok) {\n" +
                        "                throw new Error('HTTP错误! 状态: ' + response.status + ' ' + response.statusText);\n" +
                        "            }\n" +
                        "            \n" +
                        "            return response.blob();\n" +
                        "        })\n" +
                        "        .then(blob => {\n" +
                        "            console.log('成功获取Blob数据 - 大小: ' + blob.size + ' bytes, 类型: ' + blob.type);\n" +
                        "            \n" +
                        "            if (blob.size === 0) {\n" +
                        "                throw new Error('Blob数据为空，可能URL已失效');\n" +
                        "            }\n" +
                        "            \n" +
                        "            return new Promise((resolve, reject) => {\n" +
                        "                var reader = new FileReader();\n" +
                        "                reader.onloadend = function() {\n" +
                        "                    console.log('FileReader读取完成，数据长度: ' + reader.result.length);\n" +
                        "                    resolve(reader.result);\n" +
                        "                };\n" +
                        "                reader.onerror = function() {\n" +
                        "                    reject(new Error('FileReader读取失败'));\n" +
                        "                };\n" +
                        "                reader.readAsDataURL(blob);\n" +
                        "            });\n" +
                        "        });\n" +
                        "    }\n" +
                        "    \n" +
                        "    // 先尝试XHR，失败后尝试fetch\n" +
                        "    tryXHR()\n" +
                        "    .then(base64data => {\n" +
                        "        console.log('Base64数据生成成功，长度: ' + base64data.length);\n" +
                        "        \n" +
                        "        if (typeof Android !== 'undefined' && typeof Android.getBase64FromBlobData === 'function') {\n" +
                        "            Android.getBase64FromBlobData(base64data);\n" +
                        "            console.log('数据已发送到Android端');\n" +
                        "        } else {\n" +
                        "            console.error('Android接口未找到');\n" +
                        "        }\n" +
                        "    })\n" +
                        "    .catch(error => {\n" +
                        "        console.log('XHR失败，尝试fetch方案: ' + error.message);\n" +
                        "        \n" +
                        "        tryFetch()\n" +
                        "        .then(base64data => {\n" +
                        "            console.log('Fetch成功，Base64数据长度: ' + base64data.length);\n" +
                        "            \n" +
                        "            if (typeof Android !== 'undefined' && typeof Android.getBase64FromBlobData === 'function') {\n" +
                        "                Android.getBase64FromBlobData(base64data);\n" +
                        "                console.log('数据已发送到Android端');\n" +
                        "            } else {\n" +
                        "                console.error('Android接口未找到');\n" +
                        "            }\n" +
                        "        })\n" +
                        "        .catch(fetchError => {\n" +
                        "            console.error('所有方案都失败了');\n" +
                        "            console.error('XHR错误: ' + error.message);\n" +
                        "            console.error('Fetch错误: ' + fetchError.message);\n" +
                        "            console.error('错误名称: ' + fetchError.name + ', 错误信息: ' + fetchError.message + ', Blob URL: ' + '" + blobUrl + "' + ', 时间: ' + new Date().toISOString());\n" +
                        "            \n" +
                        "            if (fetchError.name === 'TypeError') {\n" +
                        "                console.error('可能的网络问题 - CORS: ' + fetchError.message.includes('CORS') + ', 网络: ' + fetchError.message.includes('network') + ', Fetch: ' + fetchError.message.includes('fetch'));\n" +
                        "            }\n" +
                        "        });\n" +
                        "    })\n" +
                        "    .finally(() => {\n" +
                        "        console.log('Blob URL处理流程结束');\n" +
                        "    });\n" +
                        "})();";
            }
            return "javascript: console.log('It is not a Blob URL');";
        }

        private String getImprovedBlobUrlHandler(String blobUrl) {
            return "javascript: (function() {\n" +
                    "    console.log('=== 改进的Blob URL处理器 ===');\n" +
                    "    console.log('目标Blob URL:', '" + blobUrl + "');\n" +
                    "    \n" +
                    "    // 检查Blob URL是否仍然有效\n" +
                    "    function checkBlobUrlValidity(url) {\n" +
                    "        try {\n" +
                    "            var urlObj = new URL(url);\n" +
                    "            return urlObj.protocol === 'blob:';\n" +
                    "        } catch (e) {\n" +
                    "            return false;\n" +
                    "        }\n" +
                    "    }\n" +
                    "    \n" +
                    "    if (!checkBlobUrlValidity('" + blobUrl + "')) {\n" +
                    "        console.error('Blob URL无效或已过期:', '" + blobUrl + "');\n" +
                    "        return;\n" +
                    "    }\n" +
                    "    \n" +
                    "    // 尝试多种方法获取Blob数据\n" +
                    "    function tryMultipleMethods() {\n" +
                    "        var methods = [\n" +
                    "            function() { return tryDirectAccess(); },\n" +
                    "            function() { return tryXHRMethod(); },\n" +
                    "            function() { return tryFetchMethod(); },\n" +
                    "            function() { return tryCreateObjectURL(); }\n" +
                    "        ];\n" +
                    "        \n" +
                    "        var currentMethod = 0;\n" +
                    "        \n" +
                    "        function tryNext() {\n" +
                    "            if (currentMethod >= methods.length) {\n" +
                    "                console.error('所有方法都失败了');\n" +
                    "                return;\n" +
                    "            }\n" +
                    "            \n" +
                    "            console.log('尝试方法 ' + (currentMethod + 1) + '/' + methods.length);\n" +
                    "            \n" +
                    "            methods[currentMethod]()\n" +
                    "            .then(function(result) {\n" +
                    "                console.log('方法 ' + (currentMethod + 1) + ' 成功');\n" +
                    "                processBlobData(result);\n" +
                    "            })\n" +
                    "            .catch(function(error) {\n" +
                    "                console.log('方法 ' + (currentMethod + 1) + ' 失败:', error.message);\n" +
                    "                currentMethod++;\n" +
                    "                tryNext();\n" +
                    "            });\n" +
                    "        }\n" +
                    "        \n" +
                    "        tryNext();\n" +
                    "    }\n" +
                    "    \n" +
                    "    // 方法1: 直接访问（如果Blob对象仍然存在）\n" +
                    "    function tryDirectAccess() {\n" +
                    "        return new Promise(function(resolve, reject) {\n" +
                    "            try {\n" +
                    "                // 尝试从window对象中查找Blob\n" +
                    "                var blob = window.blobCache && window.blobCache['" + blobUrl + "'];\n" +
                    "                if (blob) {\n" +
                    "                    console.log('找到缓存的Blob对象');\n" +
                    "                    resolve(blob);\n" +
                    "                } else {\n" +
                    "                    reject(new Error('Blob对象未找到'));\n" +
                    "                }\n" +
                    "            } catch (e) {\n" +
                    "                reject(e);\n" +
                    "            }\n" +
                    "        });\n" +
                    "    }\n" +
                    "    \n" +
                    "    // 方法2: XMLHttpRequest\n" +
                    "    function tryXHRMethod() {\n" +
                    "        return new Promise(function(resolve, reject) {\n" +
                    "            var xhr = new XMLHttpRequest();\n" +
                    "            xhr.open('GET', '" + blobUrl + "', true);\n" +
                    "            xhr.responseType = 'blob';\n" +
                    "            xhr.timeout = 10000;\n" +
                    "            \n" +
                    "            xhr.onload = function() {\n" +
                    "                if (xhr.status === 200) {\n" +
                    "                    resolve(xhr.response);\n" +
                    "                } else {\n" +
                    "                    reject(new Error('XHR失败: ' + xhr.status));\n" +
                    "                }\n" +
                    "            };\n" +
                    "            \n" +
                    "            xhr.onerror = function() {\n" +
                    "                reject(new Error('XHR网络错误'));\n" +
                    "            };\n" +
                    "            \n" +
                    "            xhr.ontimeout = function() {\n" +
                    "                reject(new Error('XHR超时'));\n" +
                    "            };\n" +
                    "            \n" +
                    "            xhr.send();\n" +
                    "        });\n" +
                    "    }\n" +
                    "    \n" +
                    "    // 方法3: Fetch API\n" +
                    "    function tryFetchMethod() {\n" +
                    "        return fetch('" + blobUrl + "', {\n" +
                    "            mode: 'cors',\n" +
                    "            credentials: 'same-origin',\n" +
                    "            cache: 'no-cache'\n" +
                    "        })\n" +
                    "        .then(function(response) {\n" +
                    "            if (!response.ok) {\n" +
                    "                throw new Error('Fetch失败: ' + response.status);\n" +
                    "            }\n" +
                    "            return response.blob();\n" +
                    "        });\n" +
                    "    }\n" +
                    "    \n" +
                    "    // 方法4: 尝试重新创建Blob URL\n" +
                    "    function tryCreateObjectURL() {\n" +
                    "        return new Promise(function(resolve, reject) {\n" +
                    "            // 这个方法通常不会成功，因为原始Blob可能已经不存在\n" +
                    "            reject(new Error('无法重新创建Blob URL'));\n" +
                    "        });\n" +
                    "    }\n" +
                    "    \n" +
                    "    // 处理获取到的Blob数据\n" +
                    "    function processBlobData(blob) {\n" +
                    "        console.log('成功获取Blob数据 - 大小:', blob.size, 'bytes, 类型:', blob.type);\n" +
                    "        \n" +
                    "        if (blob.size === 0) {\n" +
                    "            console.error('Blob数据为空');\n" +
                    "            return;\n" +
                    "        }\n" +
                    "        \n" +
                    "        var reader = new FileReader();\n" +
                    "        reader.onloadend = function() {\n" +
                    "            console.log('FileReader读取完成，数据长度:', reader.result.length);\n" +
                    "            \n" +
                    "            if (typeof Android !== 'undefined' && typeof Android.getBase64FromBlobData === 'function') {\n" +
                    "                Android.getBase64FromBlobData(reader.result);\n" +
                    "                console.log('数据已发送到Android端');\n" +
                    "            } else {\n" +
                    "                console.error('Android接口未找到');\n" +
                    "            }\n" +
                    "        };\n" +
                    "        \n" +
                    "        reader.onerror = function() {\n" +
                    "            console.error('FileReader读取失败');\n" +
                    "        };\n" +
                    "        \n" +
                    "        reader.readAsDataURL(blob);\n" +
                    "    }\n" +
                    "    \n" +
                    "    // 开始尝试多种方法\n" +
                    "    tryMultipleMethods();\n" +
                    "})();";
        }

        private void convertBase64StringToFileAndStoreIt(String base64PDf) throws IOException {
            final int notificationId = 1;
            String currentDateTime = DateFormat.getDateTimeInstance().format(new Date());
            String newTime = currentDateTime.replaceFirst(", ", "_").replaceAll(" ", "_").replaceAll(":", "-");

            // 确保fileMimeType不为null
            if (fileMimeType == null || fileMimeType.isEmpty()) {
                fileMimeType = "application/octet-stream";
            }

            Log.d("fileMimeType ====> ", fileMimeType);
            MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
            String extension = mimeTypeMap.getExtensionFromMimeType(fileMimeType);

            // 如果无法获取扩展名，使用默认扩展名
            if (extension == null || extension.isEmpty()) {
                extension = "bin";
            }

            final File dwldsPath = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS) + "/" + newTime + "_." + extension);
            String regex = "^data:" + fileMimeType + ";base64,";
            byte[] pdfAsBytes = Base64.decode(base64PDf.replaceFirst(regex, ""), 0);
            try {
                FileOutputStream os = new FileOutputStream(dwldsPath);
                os.write(pdfAsBytes);
                os.flush();
                os.close();
            } catch (Exception e) {
                Toast.makeText(context, "FAILED TO DOWNLOAD THE FILE!", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
            if (dwldsPath.exists()) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                Uri apkURI = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", dwldsPath);
                intent.setDataAndType(apkURI, MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                String CHANNEL_ID = "MYCHANNEL";
                final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel notificationChannel = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    notificationChannel = new NotificationChannel(CHANNEL_ID, "name", NotificationManager.IMPORTANCE_LOW);
                    Notification notification = new Notification.Builder(context, CHANNEL_ID)
                            .setContentText("You have got something new!")
                            .setContentTitle("File downloaded")
                            .setContentIntent(pendingIntent)
                            .setChannelId(CHANNEL_ID)
                            .setSmallIcon(android.R.drawable.stat_sys_download_done)
                            .build();
                    if (notificationManager != null) {
                        notificationManager.createNotificationChannel(notificationChannel);
                        notificationManager.notify(notificationId, notification);
                    }
                }
            }
            Toast.makeText(context, "FILE DOWNLOADED!", Toast.LENGTH_SHORT).show();
        }

        private void convertBase64StringToFileAndStoreItWithName(String base64PDf, String fileName) throws IOException {
            final int notificationId = 1;

            // 确保fileMimeType不为null
            if (fileMimeType == null || fileMimeType.isEmpty()) {
                fileMimeType = "application/octet-stream";
            }

            Log.d("fileMimeType ====> ", fileMimeType);
            Log.d("fileName ====> ", fileName);

            // 使用指定的文件名，如果没有则使用时间戳
            String finalFileName;
            if (fileName != null && !fileName.isEmpty()) {
                // 清理文件名，移除非法字符
                finalFileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
                Log.d(TAG, "使用指定文件名: " + finalFileName);
            } else {
                // 使用时间戳生成文件名
                String currentDateTime = DateFormat.getDateTimeInstance().format(new Date());
                String newTime = currentDateTime.replaceFirst(", ", "_").replaceAll(" ", "_").replaceAll(":", "-");
                MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
                String extension = mimeTypeMap.getExtensionFromMimeType(fileMimeType);
                if (extension == null || extension.isEmpty()) {
                    extension = "bin";
                }
                finalFileName = newTime + "_." + extension;
                Log.d(TAG, "使用生成的文件名: " + finalFileName);
            }

            final File dwldsPath = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS) + "/" + finalFileName);

            // 更安全的方式处理Base64数据
            String base64Data = base64PDf;
            if (base64Data.startsWith("data:")) {
                int commaIndex = base64Data.indexOf(',');
                if (commaIndex > 0) {
                    base64Data = base64Data.substring(commaIndex + 1);
                }
            }
            
            Log.d(TAG, "处理后的Base64数据长度: " + base64Data.length());
            byte[] pdfAsBytes = Base64.decode(base64Data, 0);

            try {
                FileOutputStream os = new FileOutputStream(dwldsPath);
                os.write(pdfAsBytes);
                os.flush();
                os.close();
                Log.d(TAG, "文件保存成功: " + dwldsPath.getAbsolutePath());
            } catch (Exception e) {
                Log.e(TAG, "文件保存失败", e);
                Toast.makeText(context, "FAILED TO DOWNLOAD THE FILE!", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

            if (dwldsPath.exists()) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                Uri apkURI = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", dwldsPath);

                // 根据文件扩展名设置正确的MIME类型
                String fileExtension = getFileExtension(finalFileName);
                String mimeType = getMimeTypeFromExtension(fileExtension);

                intent.setDataAndType(apkURI, mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                String CHANNEL_ID = "MYCHANNEL";
                final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel notificationChannel = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    notificationChannel = new NotificationChannel(CHANNEL_ID, "name", NotificationManager.IMPORTANCE_LOW);
                    Notification notification = new Notification.Builder(context, CHANNEL_ID)
                            .setContentText("文件已下载: " + finalFileName)
                            .setContentTitle("下载完成")
                            .setContentIntent(pendingIntent)
                            .setChannelId(CHANNEL_ID)
                            .setSmallIcon(android.R.drawable.stat_sys_download_done)
                            .build();
                    if (notificationManager != null) {
                        notificationManager.createNotificationChannel(notificationChannel);
                        notificationManager.notify(notificationId, notification);
                    }
                }
            }
            Toast.makeText(context, "FILE DOWNLOADED: " + finalFileName, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_RESULT_CODE && filePathCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限 granted，重新执行保存
                Log.e("FileSave", "存储权限被授予");
//                FileSaveHelper.getInstance().saveFileWithPermissionCheck(this,null,"","");
            } else {
                Log.e("FileSave", "存储权限被拒绝");
            }
        }
    }


}
