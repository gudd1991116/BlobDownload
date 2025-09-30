package com.example.blobdownload;

import android.util.Log;
import android.webkit.WebResourceResponse;

import java.io.ByteArrayInputStream;
import java.util.Collections;

/**
 * Created by gudd on 2025/9/26.
 */
public class BlobDownloadHelper {
    private static final String TAG = "BlobDownloadHelper";

    private static BlobDownloadHelper instance;

    private BlobDownloadHelper() {
    }

    public static BlobDownloadHelper getInstance() {
        if (instance == null) {
            instance = new BlobDownloadHelper();
        }
        return instance;
    }

    public WebResourceResponse getHandleCorsPreflightRequest() {
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


    public WebResourceResponse getHandleBlobUrlRequest(String blobUrl) {
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

    /**
     * 获取BlobUrl 缓存的JavaScript代码，初始化缓存对象和文件名映射表
     * @return
     */
    public String getBlobUrlCacheJS() {
        return "console.log('=== 开始注入Blob缓存代码 ===');" +
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
    }
    /**
     * 获取文件名捕获的JavaScript代码，通过拦截<a>标签的创建和点击事件
     * @return
     */
    public String getFileNameCaptureJS() {
        return "console.log('=== 注入文件名捕获代码 ===');" +
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
    }

    /**
     * 创建处理blob数据的javascript
     * @param blobUrl blobUrl
     * @return javascript code
     */
    public String getBlobUrlHandler(String blobUrl) {
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

    /**
     * 获取真实下载带确认弹窗的JS脚本--下载第一步（必须）
     * @param url
     * @return
     */
    public String getActualDownloadStepOne(String url) {
        return "console.log('=== 开始下载处理（带确认） ===');" +
                "var targetUrl = '" + url + "';" +
                "console.log('目标URL:', targetUrl);" +
                "if (window.blobCache && window.blobCache[targetUrl]) {" +
                "    console.log('从缓存中找到Blob');" +
                "    var blob = window.blobCache[targetUrl];" +
                "    var fileName = window.blobFileNameMap && window.blobFileNameMap[targetUrl];" +
                "    console.log('捕获到的文件名:', fileName);" +
                "    console.log('Blob大小:', blob.size);" +
                "    console.log('Blob类型:', blob.type);" +
                "    " +
                "    /* 调用Android确认下载方法 */" +
                "    if (typeof Android !== 'undefined') {" +
                "        Android.showDownloadConfirmDialog(targetUrl, fileName || '未知文件', blob.size, blob.type);" +
                "        console.log('✅ 已调用确认下载对话框');" +
                "    } else {" +
                "        console.error('Android接口不可用');" +
                "    }" +
                "} else {" +
                "    console.log('缓存中未找到目标Blob');" +
                "}";
    }

    /**
     * 获取真实的下载 JS--下载第二步（必须）
     * @param url
     * @return
     */
    public String getActualDownloadStepTwo(String url){
        return "console.log('=== 开始实际下载 ===');" +
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
    }

}
