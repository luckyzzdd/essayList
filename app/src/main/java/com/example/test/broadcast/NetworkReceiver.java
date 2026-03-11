package com.example.test.broadcast;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.example.test.service.SyncService;
import com.example.test.utils.NetWorkUtil;

/**
 * 网络状态广播接收器（系统广播）
 * 监听网络变化，断网提示，联网触发同步
 */
public class NetworkReceiver extends BroadcastReceiver {
    //记录上次的网络记录
    private boolean lastNetworkState = true;
    public static final String TAG = "Broad_cast";
    /**
     *
     * @param context 系统调用这个接收器时给的context
     * @param intent The Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        //. 获取当前网络状态
        boolean  currentConnected  = NetWorkUtil.isNetworkAvailable(context);
        Log.d(TAG,"进入onReceive");
        Log.d(TAG,"lastNetworkState:"+String.valueOf(lastNetworkState));
        Log.d(TAG,"currentConnected:"+String.valueOf(currentConnected));
        if (currentConnected && !lastNetworkState){
            Log.d(TAG,"进入网络已恢复");
            Toast.makeText(context, "网络已恢复，开始自动同步随笔", Toast.LENGTH_SHORT).show();
            // 联网后启动同步服务（触发一次同步）
            startService(context);
        }
        if (!currentConnected && lastNetworkState){
            Log.d(TAG,"进入网络已断开");
            Toast.makeText(context, "网络已断开，暂停随笔同步", Toast.LENGTH_SHORT).show();
            stopService(context);
        }
        lastNetworkState = currentConnected;
    }
    /**
     * 关闭同步服务（直接调用，系统静默处理已停止的情况）
     */
    private void stopService(Context context) {
        Intent serviceIntent = new Intent(context, SyncService.class);
        context.stopService(serviceIntent);
        Log.d(TAG, "暂停同步服务");
    }
    private  void startService(Context context) {
        Intent serviceIntent = new Intent(context, SyncService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
