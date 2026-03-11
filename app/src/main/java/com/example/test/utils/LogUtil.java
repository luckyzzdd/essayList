package com.example.test.utils;

import android.util.Log;

/**
 * 封装的日志工具类（新手友好版）
 * 核心：统一控制日志开关，简化打印代码
 */
public class LogUtil {
    // 日志开关：true=打印日志（开发时），false=关闭日志（上线时）
    private static final boolean IS_DEBUG = true;
    // 统一日志标签：方便在Logcat里筛选日志
    private static final String TAG = "TodoListLog";

    /**
     * 打印调试日志（最常用）
     * @param msg 要打印的日志内容
     */
    public static void d(String msg) {
        if (IS_DEBUG) {
            Log.d(TAG, msg);
        }
    }

    /**
     * 重载方法：支持自定义标签（可选）
     * @param tag 自定义标签
     * @param msg 日志内容
     */
    public static void d(String tag, String msg) {
        if (IS_DEBUG) {
            Log.d(tag, msg);
        }
    }

    // 可选：如果需要其他级别日志（信息、警告、错误），可以加这几个方法
    public static void i(String msg) {
        if (IS_DEBUG) {
            Log.i(TAG, msg);
        }
    }

    public static void w(String msg) {
        if (IS_DEBUG) {
            Log.w(TAG, msg);
        }
    }

    public static void e(String msg) {
        if (IS_DEBUG) {
            Log.e(TAG, msg);
        }
    }
}