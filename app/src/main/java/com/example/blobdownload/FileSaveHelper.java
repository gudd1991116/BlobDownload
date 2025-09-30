package com.example.blobdownload;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Created by gudd on 2025/9/25.
 */
public class FileSaveHelper {
    private static final String TAG = "FileSaveHelper";
    public static final int REQUEST_STORAGE_PERMISSION = 1001;

    private static FileSaveHelper instance;

    private FileSaveHelper() {
    }

    public static FileSaveHelper getInstance() {
        if (instance == null) {
            instance = new FileSaveHelper();
        }
        return instance;
    }

    public void saveFileWithPermissionCheck(Activity activity, byte[] fileData, String fileName, String mimeType) {
        // 检查权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                activity.requestPermissions(
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION
                );
                return;
            }
        }

        // 执行保存
//        saveFileCompatible(activity, fileData, fileName,mimeType);
    }


    public void convertBase64StringToFileAndStoreIt(Context context, String base64PDf, String fileMimeType) throws IOException {
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
            intent.setAction(android.content.Intent.ACTION_VIEW);
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

    public void convertBase64StringToFileAndStoreItWithName(Context context, String base64PDf, String fileName, String fileMimeType) {
        try {
            final int notificationId = new Random().nextInt(1000)+1;

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
                intent.setAction(android.content.Intent.ACTION_VIEW);
                Uri apkURI = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".fileprovider", dwldsPath);

                // 根据文件扩展名设置正确的MIME类型
                String fileExtension = getFileExtension(finalFileName);
                String mimeType = MimeTypeHelper.getMimeTypeFromExtension(fileExtension);

                intent.setDataAndType(apkURI, mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                String CHANNEL_ID = "MYCHANNEL";
                final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel notificationChannel = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    notificationChannel = new NotificationChannel(CHANNEL_ID, "Blob文件下载通知", NotificationManager.IMPORTANCE_LOW);
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
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "FILE DOWNLOADED FAILED: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }
}
