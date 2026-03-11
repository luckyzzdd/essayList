package com.example.test.api;

import com.example.test.entity.TodoPostBean;
import com.example.test.entity.TodoResponseBean;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface TodoApiService {
    // 注解说明：
    // @POST("tod o/api/add") → POST请求，路径拼接在BaseUrl后
    // @Body → 请求体（自动转JSON）
    //这个是call类，代替了okhttp,,,newCall(request)
    @POST("post")
    Call<TodoResponseBean> postTodo(@Body TodoPostBean postBean);
}
