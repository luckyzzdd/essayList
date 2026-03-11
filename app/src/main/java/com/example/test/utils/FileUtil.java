package com.example.test.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.test.entity.TodoEntity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.TestOnly;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FileUtil {
    // 备份文件名（固定，恢复时要用到）
    private static final String BACKUP_FILE_NAME = "todo_backup.json";
    // Gson实例（全局一个就行，不用重复创建）
    private static final Gson gson = new Gson();

    /**
     * 备份数据
     *
     * @param context，，里面才能拿到文件位置
     * @param todos               要备份的待办列表
     * @return true=备份成功，false=失败
     */
    public static boolean backupTodos(Context context, List<TodoEntity> todos) {
        try {
            String jsonStr = gson.toJson(todos);
            LogUtil.d("gson转化java对象成json对象:" + jsonStr);
            //这个直接就是创建空文件+创建数据流，并且openFileOutput锁定了目录
            FileOutputStream fos = context.openFileOutput(BACKUP_FILE_NAME, Context.MODE_PRIVATE);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            byte[] bytes = jsonStr.getBytes(StandardCharsets.UTF_8);
            bos.write(bytes);
            bos.flush();
            //关闭流
            bos.close();
            fos.close();
            return true;
        } catch (Exception e) {
            // 捕获异常（比如IO错误），返回false
            LogUtil.e("备份待办失败：" + e.getMessage());
            return false;
        }
    }
    // FileUtil类里新增

    /**
     * 备份待办到外部存储的Download目录
     *
     * @param context 上下文
     * @param todos   待办列表
     * @return true=成功
     */
    public static boolean backupToExternalDownload(Context context, List<TodoEntity> todos) {
        // 1. 判空+检查权限
        if (context == null || todos == null || todos.isEmpty()) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Environment.isExternalStorageManager()) {
            return false;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        // 4. 转JSON字符串
        String jsonStr = gson.toJson(todos);
        // 2. 获取Download目录路径
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

//        外部存储的私有沙箱目录
//        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        // 3. 创建备份文件（todo_backup_external.json）
        File backupFile = new File(downloadDir, "todo_backup_external.json");
        try (// 5. 写入文件
             FileOutputStream fos = new FileOutputStream(backupFile);
             // 显式指定UTF-8编码，解决FileWriter乱码问题
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             // 加缓冲流提升写入效率
             BufferedWriter bw = new BufferedWriter(osw)) {
            bw.write(jsonStr);
            bw.flush();
            return true;
        } catch (Exception e) {
            LogUtil.e("外部备份失败：" + e.getMessage());
            return false;
        }
    }

    public static List<TodoEntity> restoreFromExternalDownload(Context context) {
        if (context == null) return null;
        if (!checkStoragePermissionForRestore(context)) {
            return null;
        }

        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File backupFile = new File(downloadDir, "todo_backup_external.json");

        try (
                FileInputStream fis = new FileInputStream(backupFile);
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader bufferedReader = new BufferedReader(isr);
        ) {
            String line;
            StringBuilder sb = new StringBuilder();

            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            //JSON转列表
            Type type = new TypeToken<List<TodoEntity>>() {
            }.getType();

            return gson.fromJson(sb.toString(), type);

        } catch (Exception e) {
            Log.e("文件流操作异常", e.getMessage());
            return null;
        }
    }

    /**
     * 恢复数据
     *
     * @param context
     * @return List<TodoEntity>
     */
    public static List<TodoEntity> restoreTodos(Context context) {
        if (context == null) return null;

        try (FileInputStream fis = context.openFileInput(BACKUP_FILE_NAME);
             // 3. 用InputStreamReader读取文件内容（避免乱码）
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            //    TypeToken解决Gson泛型识别问题
            Type type = new TypeToken<List<TodoEntity>>() {
            }.getType();
            List<TodoEntity> list = gson.fromJson(isr, type);
            return list;

        } catch (Exception e) {
            // 捕获异常（比如文件不存在、JSON格式错误）
            LogUtil.e("恢复待办失败：" + e.getMessage());
            return null;
        }
    }

    // 抽离独立的权限检查方法（和Activity中的hasStoragePermission逻辑对齐）
    private static boolean checkStoragePermissionForRestore(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android11+（API30及以上）：需要MANAGE_EXTERNAL_STORAGE（特殊权限）
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android10（API29）：仅需READ_EXTERNAL_STORAGE
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android9及以下：需要READ + WRITE双权限
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

}
