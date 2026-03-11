package com.example.test.utils;

import com.example.test.api.TodoApiService;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit单例管理器（和OkHttpManager逻辑一致）
 */
public class RetrofitManager {
    // BaseUrl：注意必须以/结尾
    private static final String BASE_URL = "https://httpbin.org/";
    private static volatile Retrofit retrofit;

    public RetrofitManager() {
    }

    //获得Retrofit实例
    private static Retrofit getInstance() {
        if (retrofit == null) {
            synchronized (Retrofit.class) {
                if (retrofit == null) {
                    retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(OkHttpManager.getInstance()) // 复用OkHttp的配置（日志拦截器、超时）
                            .addConverterFactory(GsonConverterFactory.create()) // 自动JSON解析
                            .build();
                }
            }
        }
        return retrofit;
    }

    //获得API实例
    public static TodoApiService getTodoApi() {
        return getInstance().create(TodoApiService.class);
    }
}
