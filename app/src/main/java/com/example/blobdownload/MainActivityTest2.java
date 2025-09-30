package com.example.blobdownload;

import static com.example.blobdownload.FileSaveHelper.REQUEST_STORAGE_PERMISSION;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class MainActivityTest2 extends AppCompatActivity {
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
    
    // 下载确认相关变量
    private String pendingDownloadUrl;
    private String pendingFileName;
    private String pendingFileSize;
    private String pendingMimeType;

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
                FileSaveHelper.getInstance().saveFileWithPermissionCheck(MainActivityTest2.this, null, "", "");
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
                MainActivityTest2.this.filePathCallback = filePathCallback;
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

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d(TAG, "拦截请求: " + url);

                // 处理CORS预检请求
                if ("OPTIONS".equals(request.getMethod())) {
                    return BlobDownloadHelper.getInstance().getHandleCorsPreflightRequest();
                }

                // 处理Blob URL请求
                if (url.startsWith("blob:")) {
                    Log.d(TAG, "检测到Blob URL请求: " + url);
                    return BlobDownloadHelper.getInstance().getHandleBlobUrlRequest(url);
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
                    injectFileNameCaptureCode();
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

    // ==================== JavaScript注入方法 ====================
    
    /**
     * 注入Blob缓存代码，拦截URL.createObjectURL调用
     */
    private void injectBlobCacheCode() {
        // Blob缓存代码 + 文件名捕获
        String jsCode = BlobDownloadHelper.getInstance().getBlobUrlCacheJS();

        Log.d(TAG, "注入Blob缓存代码");
        webView.evaluateJavascript(jsCode, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.d(TAG, "Blob缓存代码注入结果: " + value);
            }
        });

        // 文件名捕获代码已在onPageFinished中调用，这里不需要重复调用
    }

    /**
     * 注入文件名捕获代码，拦截<a>标签的创建和点击事件
     */
    private void injectFileNameCaptureCode() {
        // 正确的文件名捕获代码 - 拦截click事件
        String fileNameCaptureCode = BlobDownloadHelper.getInstance().getFileNameCaptureJS();
        
        Log.d(TAG, "注入文件名捕获代码");
        webView.evaluateJavascript(fileNameCaptureCode, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.d(TAG, "文件名捕获代码注入结果: " + value);
            }
        });
    }
    

    // ==================== 下载处理方法 ====================
    
    /**
     * 处理Blob URL下载，支持文件名捕获和确认下载
     */
    private void downBlobUrl(String url) {
        if (url.startsWith("blob")) {
            Log.d(TAG, "download start url: " + url);

            // 修改后的下载处理 - 添加确认下载功能
            String downloadCode = BlobDownloadHelper.getInstance().getActualDownloadStepOne(url);

            webView.evaluateJavascript(downloadCode, value -> Log.d(TAG, "下载处理结果: " + value));
        }
    }

    // ==================== JavaScript接口 ====================
    public class JavaScriptInterface {
        private String fileMimeType;
        private final Context context;

        public JavaScriptInterface(Context context) {
            this.context = context;
        }

        // ==================== 核心下载方法 ====================
        
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

            FileSaveHelper.getInstance().convertBase64StringToFileAndStoreIt(context,base64Data,fileMimeType);
        }


        @JavascriptInterface
        public void downloadFileWithName(String base64Data, String fileName) throws IOException {
            Log.d(TAG, "=== 下载文件（带文件名） ===");
            Log.d(TAG, "文件名: " + fileName);
            Log.d(TAG, "数据长度: " + (base64Data != null ? base64Data.length() : 0));
            Log.d(TAG, "=====>>> " + base64Data);

            // 从文件名中提取扩展名来确定MIME类型
            if (fileName != null && !fileName.isEmpty()) {
                String extension = FileSaveHelper.getInstance().getFileExtension(fileName);
                fileMimeType = MimeTypeHelper.getMimeTypeFromExtension(extension);
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

            FileSaveHelper.getInstance().convertBase64StringToFileAndStoreItWithName(context,base64Data, fileName,fileMimeType);
        }


        @JavascriptInterface
        public void handleBlobUrl(String blobUrl) {
            Log.d(TAG, "=== 处理Blob URL ===");
            Log.d(TAG, "Blob URL: " + blobUrl);

            // 使用改进的JavaScript代码来处理Blob URL
            String jsCode = BlobDownloadHelper.getInstance().getBlobUrlHandler(blobUrl);

            webView.post(() -> {
                webView.evaluateJavascript(jsCode, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        Log.d(TAG, "Blob URL处理结果: " + value);
                    }
                });
            });
        }

        /**
         * 显示下载确认对话框
         */
        @JavascriptInterface
        public void showDownloadConfirmDialog(String url, String fileName, long fileSize, String mimeType) {
            Log.d(TAG, "=== 显示下载确认对话框 ===");
            Log.d(TAG, "URL: " + url);
            Log.d(TAG, "文件名: " + fileName);
            Log.d(TAG, "文件大小: " + fileSize);
            Log.d(TAG, "MIME类型: " + mimeType);

            // 在主线程中显示对话框
            runOnUiThread(() -> {
                showConfirmDownloadDialog(url, fileName, fileSize, mimeType);
            });
        }

        /**
         * 确认下载后开始实际下载
         */
        @JavascriptInterface
        public void startActualDownload(String url) {
            Log.d(TAG, "=== 开始实际下载 ===");
            Log.d(TAG, "URL: " + url);

            // 在主线程中执行下载
            runOnUiThread(() -> {
                performActualDownload(url);
            });
        }


    }

    /**
     * 显示确认下载对话框
     */
    private void showConfirmDownloadDialog(String url, String fileName, long fileSize, String mimeType) {
        // 保存待下载信息
        pendingDownloadUrl = url;
        pendingFileName = fileName;
        pendingFileSize = formatFileSize(fileSize);
        pendingMimeType = mimeType;

        // 创建对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_confirm_download, null);
        builder.setView(dialogView);

        // 设置文件信息
        TextView tvFileName = dialogView.findViewById(R.id.tvConfirmFileName);
        TextView tvFileSize = dialogView.findViewById(R.id.tvConfirmFileSize);
        TextView tvFileType = dialogView.findViewById(R.id.tvConfirmFileType);

        tvFileName.setText("文件名: " + fileName);
        tvFileSize.setText("文件大小: " + pendingFileSize);
        tvFileType.setText("文件类型: " + mimeType);

        // 创建对话框
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        // 设置按钮点击事件
        Button btnCancel = dialogView.findViewById(R.id.btnCancelConfirm);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirmDownload);

        btnCancel.setOnClickListener(v -> {
            Log.d(TAG, "用户取消下载");
            dialog.dismiss();
            // 清空待下载信息
            clearPendingDownload();
        });

        btnConfirm.setOnClickListener(v -> {
            Log.d(TAG, "用户确认下载");
            dialog.dismiss();
            // 开始实际下载
            performActualDownload(url);
        });

        dialog.show();
    }

    /**
     * 执行实际下载
     */
    private void performActualDownload(String url) {
        Log.d(TAG, "=== 执行实际下载 ===");
        Log.d(TAG, "URL: " + url);

        String downloadCode = BlobDownloadHelper.getInstance().getActualDownloadStepTwo(url);

        webView.evaluateJavascript(downloadCode, value -> {
            Log.d(TAG, "实际下载处理结果: " + value);
            // 清空待下载信息
            clearPendingDownload();
        });
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 清空待下载信息
     */
    private void clearPendingDownload() {
        pendingDownloadUrl = null;
        pendingFileName = null;
        pendingFileSize = null;
        pendingMimeType = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearPendingDownload();
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
