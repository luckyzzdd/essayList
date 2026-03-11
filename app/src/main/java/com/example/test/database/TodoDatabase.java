package com.example.test.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import com.example.test.dao.TodoDao;
import com.example.test.entity.TodoEntity;

/**
 * Data类单例管理，保证只有一个连接
 *
 * 这里用抽象类，是因为Room要帮我自动生成实现类
 */
@Database(entities = {TodoEntity.class},version = 1)
public abstract class TodoDatabase extends RoomDatabase {
    //定义DAO的抽象方法
   public abstract TodoDao todoDao();
    // 2. 单例模式：避免重复创建数据库连接（新手必学）
    private static volatile TodoDatabase INSTANCE;
    //获取数据实例的方法
    //两重检查锁
    public static TodoDatabase getINSTANCE(Context context) {
        if (INSTANCE==null){
            synchronized (TodoDatabase.class){
                if (INSTANCE==null){
                    //创建Database实例
                    /** volatile 的作用：禁止指令重排
                     * JVM
                     * 1. 分配内存空间（给TodoDatabase实例）；
                     * 2. 初始化实例（执行Room的build逻辑：创建数据库连接、初始化表结构、生成DAO实现等）；
                     * 3. 把INSTANCE指向分配的内存空间（此时INSTANCE从null变为非null）。
                     * 但是JVM 为了优化性能，可能会把步骤 2 和 3 重排（指令重排），变成：
                     *分配内存空间；
                     * 2. INSTANCE指向内存空间（INSTANCE≠null）；
                     * 3. 初始化实例（还在执行Room的初始化逻辑）。
                     * 线程a到2，3之间，线程b来看到Instance！=null,就拿到实例，但此时还没有初始化
                     */
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    TodoDatabase.class,
                                    "todo_database"
                            )
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
