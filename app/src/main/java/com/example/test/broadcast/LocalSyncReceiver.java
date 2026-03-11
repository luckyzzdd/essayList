package com.example.test.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * 本地广播接收器：接收Service的同步结果
 * SyncService 同步完成后，用本地广播通知 MainActivity 更新 UI（比如显示同步成功提示）
 */
public class LocalSyncReceiver extends BroadcastReceiver {
    // 广播Action（自定义，唯一即可）
    public static final String ACTION_SYNC_RESULT = "com.example.test.action.SYNC_RESULT";
    // 同步结果的Key
    public static final String KEY_SYNC_MSG = "sync_msg";
    private OnSyncResultListener listener;
    // 定义回调接口，用于通知页面刷新列表
    public interface OnSyncResultListener {
        void onSyncCompleted(); // 同步完成，需要刷新列表
    }
    public LocalSyncReceiver(OnSyncResultListener listener) {
        this.listener = listener;
    }
    // 提供无参构造（给MainActivity用）和有参构造（给Fragment用）
    public LocalSyncReceiver() {
        this.listener = null;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String msg = intent.getStringExtra(KEY_SYNC_MSG);
        // 通知页面刷新列表
        if (listener != null) {
            listener.onSyncCompleted();
        }
    }
}
