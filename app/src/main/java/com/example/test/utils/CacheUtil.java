package com.example.test.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class CacheUtil {


    private static final String SP_NAME = "hitokoto_cache";
    private static final String KEY_HITOKOTO = "hitokoto_text";
    private static final String KEY_FROM = "hitokoto_from";

    // 存缓存：把新的句子和来源存起来（自动覆盖旧的）
    public static void saveHitokotoCache(Context context, String text, String from) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit()
                .putString(KEY_HITOKOTO, text)
                .putString(KEY_FROM, from)
                .apply();
    }

    // 取缓存：获取上次存的句子（返回null表示无缓存）
    public static String getHitokotoText(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_HITOKOTO, null);
    }

    // 取缓存：获取上次存的来源
    public static String getHitokotoFrom(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_FROM, null);
    }
}

