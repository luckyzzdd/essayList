package com.example.test.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.test.entity.TodoEntity;

import java.util.List;

/**
 * 这个注解标记这是数据访问对象
 * 用来增删改查数据表的
 */

@Dao
public interface TodoDao {
    /**
     * 可以批量新增，参数可以是List<TodoEntity>
     * @param todoEntity
     */
    @Insert
    void insert(TodoEntity todoEntity);

    //order by 。。排序
    //DESC，降序
    @Query("select * from todo_table order by id DESC")
    List<TodoEntity> getAllTodos();
    @Delete
    void delete(TodoEntity todoEntity);

    @Insert
    void insertAll(List<TodoEntity> todos);

    // 清空整个表（delete from 表名）
    @Query("DELETE FROM todo_table")
    void deleteAll();

    @Query("select * from todo_table where isSync = :isSync order by id DESC")
    List<TodoEntity> queryBySyncStatus(boolean isSync);
    @Update
    void update(TodoEntity todoEntity);
    @Query("select * from todo_table where id = :id")
    TodoEntity queryById(long id);
}
