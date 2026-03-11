package com.example.test.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.test.R;

/**
 * 通知工具类：创建前台服务的通知渠道+通知（找工作项目必备）
 */
public class NotificationUtil {
    // 通知渠道ID（自定义，唯一即可）
    public static final String CHANNEL_ID = "sync_service_channel";
    // 前台服务通知ID（自定义，>0即可）
    public static final int SERVICE_NOTIFICATION_ID = 1001;

    /**
     * 创建通知渠道（Android 8.0+必须）
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //安卓8以上
            NotificationChannel notificationChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "随笔同步服务",
                    NotificationManager.IMPORTANCE_LOW// 低优先级，不弹窗，只在通知栏显示
            );
            //注册渠道
            NotificationManager manager =  context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(notificationChannel);
        }
    }

    /**
     * 获取前台服务的通知对象
     */
    public static Notification getSyncServiceNotification(Context context) {
        createNotificationChannel(context);
        //构建通知
        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("随笔同步服务")
                .setContentText("正在后台自动同步你的随笔，每一分钟一次...")
                .setSmallIcon(R.drawable.start_label_icon) // 替换为你的APP图标（必须是应用内资源）
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}
