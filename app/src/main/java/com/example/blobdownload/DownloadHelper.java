package com.example.blobdownload;

import android.webkit.WebView;

/**
 * Created by gudd on 2025/8/18.
 */
public class DownloadHelper {

    public static DownloadHelper mInstance;

    public static synchronized DownloadHelper getInstance() {
        if (mInstance == null) {
            mInstance = new DownloadHelper();
        }
        return mInstance;
    }

    public void evaluateBlobUrl(WebView webView, String blobUrl) {
        String js = String.format(
                "(function() {" +
                        "  fetch('%s').then(res => res.blob())" +
                        "    .then(blob => {" +
                        "      const reader = new FileReader();" +
                        "      reader.onloadend = function() {" +
                        "        Android.onBlobData(reader.result);" + // 调用 Android 方法
                        "      };" +
                        "      reader.readAsDataURL(blob);" + // 转换为 Base64
                        "    });" +
                        "})();", blobUrl);

        webView.evaluateJavascript(js, null);
    }


}
