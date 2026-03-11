package com.example.test.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * 一个简单的把时间戳变成可读格式的工具
 */
public class TimeUtil {
    public static final String getTime(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
        //设置时区
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        Date date = new Date(System.currentTimeMillis());
        String string = simpleDateFormat.format(date);
        return string;
    }

}
