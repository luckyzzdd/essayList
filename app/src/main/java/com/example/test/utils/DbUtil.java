package com.example.test.utils;

import android.content.Context;

import com.example.test.dao.TodoDao;
import com.example.test.database.TodoDatabase;
import com.example.test.entity.TodoEntity;

import java.util.List;

public class DbUtil {
    private  TodoDatabase todoDatabase;
   private static TodoDao initTodoDao(Context context){
       TodoDao todoDao = TodoDatabase.getINSTANCE(context).todoDao();
       return todoDao;
   }

   public static List<TodoEntity> getUnSyncTodos(Context context){
       TodoDao todoDao = initTodoDao(context);
       return todoDao.queryBySyncStatus(false);
   }

    // 同步成功后更新状态（新增方法）
    public static void updateTodoSyncStatus(Context context, long todoId, boolean isSync) {
        TodoDao todoDao = initTodoDao(context);
        TodoEntity todo = todoDao.queryById(todoId);
        if (todo != null) {
            todo.setSync(isSync);
            todoDao.update(todo);
        }
    }
}
