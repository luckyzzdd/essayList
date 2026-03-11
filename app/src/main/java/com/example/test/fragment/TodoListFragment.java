package com.example.test.fragment;

import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.test.MainActivity;
import com.example.test.adapter.TodoAdapter;
import com.example.test.broadcast.LocalSyncReceiver;
import com.example.test.dao.TodoDao;
import com.example.test.database.TodoDatabase;
import com.example.test.databinding.FragmentTodoListBinding;
import com.example.test.entity.TodoEntity;
import com.example.test.utils.FileUtil;
import com.example.test.utils.LogUtil;

import java.util.List;

public class TodoListFragment extends Fragment {
    private FragmentTodoListBinding binding;
    // 新手：SharedPreferences的文件名常量（避免硬编码）
    private static final String SP_NAME = "TodoList";
    private static final String KEY_LAST_TODO = "last_todo";
    private TodoAdapter todoAdapter;
    // 新增1：广播接收器实例
    private LocalSyncReceiver syncReceiver;
    // 新增2：防重复注册标记
    private boolean isReceiverRegistered = false;
    private boolean flag = false;
    private String TAG ="Todo_ListFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTodoListBinding.inflate(inflater, container, false);
        initView();
        Log.d(TAG,"进入onCreateView，并加载数据");
        return binding.getRoot();
    }


    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden){
            loadTodoList();
            Log.d(TAG,"进入onHiddenChanged,并加载数据");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isReceiverRegistered){
            initSyncReceiver();
        }
        //查询数据库的记录，给到todoAdapter的list
        loadTodoList();
        Log.d(TAG,"进入onResume,并加载数据");
    }

    @Override
    public void onPause() {
        super.onPause();
        // Fragment不可见时注销，避免接收无用广播
        unregisterSyncReceiver();
    }

    /**
     * 初始化广播接收器
     */
    private void initSyncReceiver() {
        if (getActivity()==null || isReceiverRegistered) return;
        //
        syncReceiver = new LocalSyncReceiver(() -> {
            //同步完成，刷新列表
            loadTodoList();
            Toast.makeText(getContext(), "同步完成，列表已更新", Toast.LENGTH_SHORT).show();
        });
        // 注册广播
        IntentFilter filter = new IntentFilter(LocalSyncReceiver.ACTION_SYNC_RESULT);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(syncReceiver, filter);
        // 标记为已注册
        isReceiverRegistered = true;
    }


    // 注销广播接收器（抽成独立方法）
    private void unregisterSyncReceiver() {
        if (getActivity() == null || !isReceiverRegistered || syncReceiver == null) return;

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(syncReceiver);
        // 重置标记
        isReceiverRegistered = false;
        syncReceiver = null;
    }

    private void initView() {
        //创建Adapter
        todoAdapter = new TodoAdapter(getContext(),this::handlerTodoDelete);
        //设置Adapter
        binding.RV.setAdapter(todoAdapter);
        //设置布局管理器
        binding.RV.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.RV.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        binding.btnBackup.setOnClickListener(v -> {
            // 点击按钮时，执行备份逻辑
            backupTodoList();
        });
        binding.btnRestore.setOnClickListener(v->{
            //点击按钮时，执行恢复逻辑
            restoreTodoList();
        });
        // 新增：外部备份按钮
        binding.btnBackupExternal.setOnClickListener(v -> {
            backupToExternalDownload();
        });

        // 新增：外部恢复按钮
        binding.btnRestoreExternal.setOnClickListener(v -> {
            restoreFromExternalDownload();
        });



    }
    /**
     * 备份到外部Download目录
     */
    private void backupToExternalDownload() {
        if (getActivity() == null || getContext() == null) {
            Toast.makeText(getContext(), "上下文异常", Toast.LENGTH_SHORT).show();
            return;
        }
        //点击先授权
        if (!((MainActivity)getActivity()).hasStoragePermission()){
            //没有授权
            Toast.makeText(getContext(), "请先授权外部存储权限,成功后再次点击", Toast.LENGTH_SHORT).show();
            //发起授权
            ((MainActivity) getActivity()).checkAndRequestStoragePermission();
            return;
        }
        //开始备份
        new Thread(()->{
            try {
                //先拿到数据
                TodoDao todoDao = TodoDatabase.getINSTANCE(getContext()).todoDao();
                List<TodoEntity> allTodos = todoDao.getAllTodos();
                //调取工具类备份
                boolean isSuccess = FileUtil.backupToExternalDownload(getContext(),allTodos);
                getActivity().runOnUiThread(()->{
                    if (isSuccess) {
                        Toast.makeText(getContext(), "备份成功！文件存在外部存储", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "备份失败（列表为空/出错）", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                LogUtil.e("外部备份待办异常：" + e.getMessage());
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "外部备份失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * 从外部Download目录回复
     */
    private void restoreFromExternalDownload(){
        if (getActivity() == null || getContext() == null) {
            Toast.makeText(getContext(), "上下文异常", Toast.LENGTH_SHORT).show();
            return;
        }
        // 权限检查
        if (!((MainActivity)getActivity()).hasStoragePermission()) {
            Toast.makeText(getContext(), "请先授权外部存储权限", Toast.LENGTH_SHORT).show();
            ((MainActivity)getActivity()).checkAndRequestStoragePermission();
            return;
        }
        new Thread(() -> {
            try {
                List<TodoEntity> restoreTodos = FileUtil.restoreFromExternalDownload(getContext());
                if (restoreTodos == null || restoreTodos.isEmpty()) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "外部恢复失败：无备份文件", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // 清空+插入
                TodoDao todoDao = TodoDatabase.getINSTANCE(getContext()).todoDao();
                todoDao.deleteAll();
                todoDao.insertAll(restoreTodos);

                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "外部恢复成功！共恢复" + restoreTodos.size() + "条", Toast.LENGTH_SHORT).show();
                    loadTodoList();
                });
            } catch (Exception e) {
                LogUtil.e("外部恢复异常：" + e.getMessage());
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "外部恢复失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();

    }
    private void restoreTodoList() {
        if (getActivity() == null || getContext() == null) {
            Toast.makeText(getContext(), "上下文异常，恢复失败", Toast.LENGTH_SHORT).show();
            return;
        }
        //子线程进行回复操作
        new Thread(()->{
            try {
                List<TodoEntity> restoreTodos = FileUtil.restoreTodos(getContext());
                if (restoreTodos == null || restoreTodos.isEmpty()) {
                    // 恢复失败（文件不存在/列表为空）
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "恢复失败：无备份文件/备份内容为空", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                //操作数据，先清空，后删除
                TodoDao todoDao = TodoDatabase.getINSTANCE(getContext()).todoDao();
                todoDao.deleteAll();
                todoDao.insertAll(restoreTodos);
                // 步骤3：主线程提示+刷新列表
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "恢复成功！共恢复" + restoreTodos.size() + "条待办", Toast.LENGTH_SHORT).show();
                    // 刷新RecyclerView列表
                    loadTodoList();
                });
            } catch (Exception e) {
                LogUtil.e("恢复列表异常：" + e.getMessage());
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "恢复失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void backupTodoList() {
        if (getActivity() == null || getContext() == null) {
            Toast.makeText(getContext(), "上下文异常，备份失败", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(()->{
            try {
                TodoDao todoDao = TodoDatabase.getINSTANCE(getContext()).todoDao();
                List<TodoEntity> allTodos = todoDao.getAllTodos();
                //调取工具类备份
                boolean isSuccess = FileUtil.backupTodos(getContext(), allTodos);
                getActivity().runOnUiThread(()->{
                    if (isSuccess) {
                        Toast.makeText(getContext(), "备份成功！文件存在内部存储", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "备份失败（列表为空/出错）", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                LogUtil.e("备份记录列表异常：" + e.getMessage());
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "备份失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();

    }

    /**
     * 处理删除事件，子线程执行数据库操作
     */
    public void handlerTodoDelete(TodoEntity todoEntity, int position){
        if (getActivity()==null) return;
        new Thread(()->{
            try {
                TodoDao todoDao = TodoDatabase.getINSTANCE(getContext()).todoDao();
                todoDao.delete(todoEntity);
                getActivity().runOnUiThread(() -> {
                    loadTodoList();
                    Toast.makeText(getContext(), "删除成功", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                LogUtil.e("删除记录失败：" + e.getMessage());
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "删除失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * 子线程加载数据
     */
    public void loadTodoList(){
        if (getActivity()==null) return;
        new Thread(()->{
            try {
                TodoDao todoDao = TodoDatabase.getINSTANCE(getContext()).todoDao();
                List<TodoEntity> allTodos = todoDao.getAllTodos();
                // 优化：无论数据是否为空，都刷新UI
                getActivity().runOnUiThread(() -> {
                    todoAdapter.update(allTodos);
                    if (!allTodos.isEmpty()) {
                        binding.RV.post(()->{
                            // 进阶：平滑滚动（体验更好）
                            binding.RV.smoothScrollToPosition(0);
                        });
                    }
                    // 空数据提示（新手友好）
                    if (allTodos.isEmpty()) {
                        Toast.makeText(getContext(), "暂无记录，点击右下角添加", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                LogUtil.e("加载记录列表失败：" + e.getMessage());
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "加载失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterSyncReceiver();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // 避免内存泄漏
    }
}