package com.example.test.utils;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * 这种单例模式，就是防止多并发导致的问题
 */
public class OkHttpManager {
    private static volatile OkHttpClient okHttpClient;
    //超时时间
    private static final int TIMEOUT = 15;

    // 私有化构造方法，禁止外部new
    private OkHttpManager() {
    }

    /**
     * 本质就是不用直接new OkHttpClient这样，因为要配置日志拦截器和超时时间
     * @return
     */
    public static OkHttpClient getInstance() {
        if (okHttpClient == null) {
            synchronized (OkHttpClient.class) {
                if (okHttpClient == null) {
                    //配置日志拦截器
                    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                    //配置日志级别，BODY = 打印请求+响应的完整内容（包括JSON）
                    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                    //构建OKHttpClient
                    okHttpClient = new OkHttpClient.Builder()
                            .addInterceptor(loggingInterceptor)
                            .connectTimeout(TIMEOUT, TimeUnit.SECONDS) // 连接超时
                            .readTimeout(TIMEOUT, TimeUnit.SECONDS)   // 读取超时
                            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)  // 写入超时
                            .build();
                }
            }

        }
        return okHttpClient;
    }

}
