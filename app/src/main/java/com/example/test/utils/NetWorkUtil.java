package com.example.test.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.test.entity.HitokotoBean;
import com.example.test.entity.TodoPostBean;
import com.example.test.entity.TodoResponseBean;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 网络工具类：封装GET请求，处理网络状态检查、线程切换
 */
public class NetWorkUtil {
    // 每日一句API地址
    private static final String HITOKOTO_URL = "https://v1.hitokoto.cn/?c=d&c=i";
    // Gson实例（全局单例，避免重复创建）
    private static final Gson gson = new Gson();
    // 主线程Handler（用于更新UI）
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    // 新增：POST请求的API地址
    private static final String TODO_POST_URL = "https://httpbin.org/post";
    // 新增：JSON媒体类型（POST请求体的Content-Type）
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    /**
     * 回调接口：给UI层返回请求结果，就是toast来提示一下
     */
    public interface HitokotoCallback {
        void onSuccess(HitokotoBean bean); // 请求成功

        void onFail(String errorMsg);      // 请求失败
    }

    //响应
    public interface TodoCallback {
        void onSuccess(TodoResponseBean bean); // 请求成功

        void onFail(String errorMsg);      // 请求失败
    }

    /**
     * 每日一句文案（异步GET请求）
     *
     * @param context 上下文，检查网络
     * @return 回调（返回结果）
     */
    public static void getHitokoto(Context context, HitokotoCallback callback) {
        //先检查网络状态
        if (!isNetworkAvailable(context)) {
            callback.onFail("无网络连接，请检查网络");
            return;
        }
        //构建GET请求
        Request request = new Request.Builder()
                .url(HITOKOTO_URL)
                .get() // GET请求（默认就是GET，可省略）
                .build();
        //enqueue这里开始切子线程了，所以要调用handler
        OkHttpManager.getInstance().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                //请求失败，（比如超时、服务器错误）
                String errorMsg = "请求失败：" + e.getMessage();
                Log.e("NetworkUtil", errorMsg);
                // 切换到主线程回调
                mainHandler.post(() -> callback.onFail(errorMsg));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                //处理响应
                if (response.isSuccessful() && response.body() != null) {
                    //读取响应体（JSON字符串）
                    String jsonStr = response.body().string();
                    Log.d("NetworkUtil", "API返回：" + jsonStr);
                    HitokotoBean bean = gson.fromJson(jsonStr, HitokotoBean.class);
                    mainHandler.post(() -> callback.onSuccess(bean));
                }
            }
        });
    }

    public static void postTodo(Context context, TodoPostBean postBean, TodoCallback callback) {
        // 1. 检查网络
        if (!isNetworkAvailable(context)) {
            callback.onFail("无网络连接");
            return;
        }
        String jsonStr = gson.toJson(postBean);
        Log.d("NetworkUtil", "POST请求体：" + jsonStr);

        //构建请求体
        RequestBody requestBody = RequestBody.create(jsonStr, JSON_TYPE);
        //构建post请求
        Request request = new Request.Builder()
                .url(TODO_POST_URL)
                .post(requestBody)
                .build();

        //异步发起请求
        OkHttpManager.getInstance().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                String errorMsg = "POST请求失败：" + e.getMessage();
                Log.e("NetworkUtil", errorMsg);
                mainHandler.post(() -> callback.onFail(errorMsg));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    // 解析响应JSON
                    String responseJson = response.body().string();
                    TodoResponseBean bean = gson.fromJson(responseJson, TodoResponseBean.class);
                    mainHandler.post(() -> callback.onSuccess(bean));
                } else {
                    String errorMsg = "响应失败：" + response.code();
                    Log.e("NetworkUtil", errorMsg);
                    mainHandler.post(() -> callback.onFail(errorMsg));
                }
            }
        });

    }

    //检查网络状态
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;
        //拿网络管理员对象
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        // Android 10 及以上使用新 API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            if (cm == null) return false;
            //当前活跃的网络对象
            Network activeNetwork = cm.getActiveNetwork();
            //网络的能力对象
            NetworkCapabilities networkCapabilities = cm.getNetworkCapabilities(activeNetwork);
            if (networkCapabilities == null) return false;
            //检查是否有网络连接
            return ( networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    ||  networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                    &&  networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else {
            try {
                android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }


    }
}
