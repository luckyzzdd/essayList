package com.example.test.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.test.R;
import com.example.test.adapter.TodoAdapter;
import com.example.test.api.TodoApiService;
import com.example.test.broadcast.LocalSyncReceiver;
import com.example.test.entity.TodoEntity;
import com.example.test.entity.TodoPostBean;
import com.example.test.entity.TodoResponseBean;
import com.example.test.utils.DbUtil;
import com.example.test.utils.NetWorkUtil;
import com.example.test.utils.NotificationUtil;
import com.example.test.utils.RetrofitManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 随笔同步前台服务（找工作项目核心功能）
 * 核心：带通知栏，保活率高，用于自动同步随笔到云端
 */
public class SyncService extends Service {
    private static final String TAG = "SyncService";
    // 关键：添加标记，防止重复启动定时任务
    private boolean isSyncTaskRunning = false; // 初始为false
    private Handler syncHandler;
    //接口实例，
    private TodoApiService todoApiService;
    // 同步间隔：10分钟（测试时可改成30秒，方便验证）
    private static final long SYNC_INTERVAL = 60 * 1000; // 10分钟=600000毫秒
    private int successCount = 0;
    private int failCount = 0;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "服务创建：onCreate");
        // 关键：启动前台服务（绑定通知栏，避免被系统杀死）
        // 适配Android 12+启动前台服务
        Notification notification = NotificationUtil.getSyncServiceNotification(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startForeground(NotificationUtil.SERVICE_NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NotificationUtil.SERVICE_NOTIFICATION_ID, notification);
        }
        // 2. 初始化Handler和Retrofit
        syncHandler = new Handler(Looper.getMainLooper());
        todoApiService = RetrofitManager.getTodoApi();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "服务启动：onStartCommand");
        // 面试考点：返回START_STICKY → 服务被系统杀死后，系统会尝试重启（保活关键）
        //启动定时任务
        if (!isSyncTaskRunning) {
            startSyncTask();
            isSyncTaskRunning = true;
        }
        return START_STICKY;
    }

    /**
     * 核心：定时同步任务（每隔SYNC_INTERVAL执行一次）
     */
    private void startSyncTask() {
        syncHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 执行同步逻辑
                syncUnUploadedTodos();
                // 循环执行：执行完一次后，延迟SYNC_INTERVAL再执行
                syncHandler.postDelayed(this, SYNC_INTERVAL);
            }
        }, SYNC_INTERVAL);
    }

    /**
     * 核心逻辑：同步未上传的随笔到云端
     */
    private void syncUnUploadedTodos() {
        Log.d(TAG, "开始同步未上传的随笔...");
        // 1. 查询本地未同步的随笔（子线程操作数据库，避免ANR）
        new Thread(() -> {
            List<TodoEntity> unSyncList = DbUtil.getUnSyncTodos(SyncService.this);
            if (unSyncList.isEmpty()) {
                Log.d(TAG, "暂无未同步的随笔");
                return;
            }
            Log.d(TAG, "即将调用Retrofit请求");
            //计数器
            CountDownLatch latch = new CountDownLatch(unSyncList.size());
            successCount=0;
            failCount= 0;
            List<String> failTitles = new ArrayList<>();
            for (TodoEntity todo : unSyncList) {
                // 把TodoEntity转成PostBean（适配Retrofit的请求体）
                TodoPostBean postBean = new TodoPostBean(
                        todo.getTitle(),
                        todo.getContent(),
                        todo.getCreateTime()
                );

                //调用Retrofit的post请求
                todoApiService.postTodo(postBean).enqueue(new Callback<TodoResponseBean>() {
                    @Override
                    public void onResponse(Call<TodoResponseBean> call, Response<TodoResponseBean> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            //同步成功，更新数据库同步状态
                            new Thread(() -> {
                                Log.d(TAG, "进入子线程");
                                try {
                                    DbUtil.updateTodoSyncStatus(SyncService.this, todo.getId(), true);

                                    Log.d(TAG, "随笔同步成功：" + todo.getTitle());
                                    successCount++;
                                } catch (Exception e) {
                                    Log.e(TAG, "错误" + e.getMessage());
                                    failCount++;
                                }
                                finally {
                                    // 不管成功/失败，计数器-1
                                    latch.countDown();
                                }
                            }).start();
                        }else {
                            failCount++;
                            latch.countDown();
                        }
                    }

                    @Override
                    public void onFailure(Call<TodoResponseBean> call, Throwable t) {
                        Log.e(TAG, "随笔同步失败：" + todo.getTitle() + "，原因：" + t.getMessage());
                        failCount++;
                        latch.countDown();
                    }
                });
            }
            // 2. 等待所有请求完成（计数器归0）
            try {
                latch.await(); // 阻塞，直到所有请求完成
            } catch (InterruptedException e) {
                Log.e(TAG, "等we待同步完成被中断：" + e.getMessage());
                Thread.currentThread().interrupt();
            }

            // 批量同步，发送一次广播
        new Handler(Looper.getMainLooper()).postDelayed(()->{
            String msg = "同步完成：成功" + successCount + "条，失败" + failCount + "条";
            sendSyncResultBroadcast(msg);
            updateNotification(msg);
        },1000);
        }).start();
    }

    private void updateNotification(String content) {
        Notification build = new NotificationCompat.Builder(this, NotificationUtil.CHANNEL_ID)
                .setContentTitle("随笔同步服务")
                .setContentText(content)
                .setSmallIcon(R.drawable.start_label_icon)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        //更新通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NotificationUtil.SERVICE_NOTIFICATION_ID, build,ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        }else {
            startForeground(NotificationUtil.SERVICE_NOTIFICATION_ID, build);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "服务销毁：onDestroy");

        // 停止定时任务（避免内存泄漏）
        syncHandler.removeCallbacksAndMessages(null);
        // 停止前台服务，移除通知
        stopForeground(true);
    }

    //这个是绑定式的service要用的
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    // 同步成功时发广播
    private void sendSyncResultBroadcast(String msg) {
        //传入广播标识
        Intent intent = new Intent(LocalSyncReceiver.ACTION_SYNC_RESULT);
        intent.putExtra(LocalSyncReceiver.KEY_SYNC_MSG, msg);
        //发送本地广播
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
