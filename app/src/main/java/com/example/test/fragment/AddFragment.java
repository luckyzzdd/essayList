package com.example.test.fragment;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.test.MainActivity;
import com.example.test.R;
import com.example.test.api.TodoApiService;
import com.example.test.database.TodoDatabase;
import com.example.test.databinding.FragmentAddBinding;
import com.example.test.entity.TodoEntity;
import com.example.test.entity.TodoPostBean;
import com.example.test.entity.TodoResponseBean;
import com.example.test.utils.LogUtil;
import com.example.test.utils.NetWorkUtil;
import com.example.test.utils.RetrofitManager;
import com.example.test.utils.TimeUtil;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddFragment extends Fragment {
    private FragmentAddBinding binding;
    private String TAG = "ADD_FRAGMENT";
    // 新增：保存当前待办数据，用于对比更新
    private TodoEntity currentTodo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddBinding.inflate(inflater, container, false);
        // 1. 抽离参数解析到独立方法
        updateTodoData();
        initClickEvents();
        return binding.getRoot();
    }

    // 新增：独立的参数解析+UI刷新方法（核心修复）
    public void updateTodoData() {
        if (getArguments() == null || binding == null) return;

        TodoEntity newTodo = getArguments().getParcelable("todo_data");
        // 避免重复刷新（数据没变就不更新）
        if (newTodo == null || newTodo.equals(currentTodo)) return;

        currentTodo = newTodo;
        // 更新UI
        binding.etTodoContent.setText(currentTodo.getContent());
        binding.tvFragmentTitle.setText(currentTodo.getTitle());
        Log.d(TAG, "AddFragment参数已更新：" + currentTodo.getTitle());
    }

    // 新增：抽离点击事件，让代码更清晰
    private void initClickEvents() {
        // Retrofit提交按钮
        binding.btnRequestSubmitTodo.setOnClickListener(v -> {
            String todoContent = binding.etTodoContent.getText().toString().trim();
            String title = binding.tvFragmentTitle.getText().toString().trim();
            if (todoContent.isEmpty() && title.isEmpty()) {
                Toast.makeText(getContext(), "请输入待办内容", Toast.LENGTH_SHORT).show();
                return;
            }
            if (getContext() == null) return;
            TodoPostBean todoPostBean = new TodoPostBean(title, todoContent, TimeUtil.getTime());
            retrofitRequest(todoPostBean);
        });

        // 本地提交按钮
        binding.btnSubmitTodo.setOnClickListener(v -> {
            String todoContent = binding.etTodoContent.getText().toString().trim();
            String title = binding.tvFragmentTitle.getText().toString().trim();
            if (todoContent.isEmpty() && title.isEmpty()) {
                Toast.makeText(getContext(), "请输入待办内容", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                try {
                    if (getActivity() != null) {
                        TodoEntity todoEntity = new TodoEntity(todoContent, TimeUtil.getTime(), title);
                        todoEntity.setSync(false);
                        TodoDatabase.getINSTANCE(getActivity()).todoDao().insert(todoEntity);

                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "待办已保存到数据库", Toast.LENGTH_SHORT).show();
                            // 保存到SP
                            SharedPreferences todoSP = getContext().getSharedPreferences("todoSP", MODE_PRIVATE);
                            SharedPreferences.Editor edit = todoSP.edit();
                            edit.putString("last_todo_content", todoContent);
                            edit.apply();
                            // 切换回列表页
                            switchTodoListFragment();
                            // 清空输入框
                            binding.etTodoContent.setText("");
                            binding.tvFragmentTitle.setText("");
                            // 重置当前待办数据
                            currentTodo = null;
                            getArguments().clear(); // 清空参数，避免下次显示旧数据
                        });
                    }
                } catch (Exception e) {
                    LogUtil.e("添加待办失败：" + e.getMessage());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "添加失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }).start();
        });

        // 清空按钮
        binding.btnClearTodo.setOnClickListener(v -> {
            binding.etTodoContent.setText("");
            binding.tvFragmentTitle.setText("");
            currentTodo = null;
            getArguments().clear();
        });
    }

    // 关键修复：Fragment显示/隐藏时刷新参数
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) { // 当Fragment从隐藏变为显示时
            updateTodoData(); // 重新解析参数并刷新UI
            Log.d(TAG, "AddFragment显示，执行参数刷新");
        }
    }

    // 兜底：Resume时也刷新一次
    @Override
    public void onResume() {
        super.onResume();
        updateTodoData();
    }

    private void retrofitRequest(TodoPostBean todoPostBean) {
        TodoApiService todoApiService = RetrofitManager.getTodoApi();
        todoApiService.postTodo(todoPostBean).enqueue(new Callback<TodoResponseBean>() {
            @Override
            public void onResponse(Call<TodoResponseBean> call, Response<TodoResponseBean> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(getContext(), "Retrofit提交成功：" + response.body().getJson(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Retrofit响应失败：" + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TodoResponseBean> call, Throwable t) {
                Toast.makeText(getContext(), "Retrofit请求失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void switchTodoListFragment() {
        if (getActivity() != null) {
            try {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                MainActivity mainActivity = (MainActivity) getActivity();
                Fragment currentFragment = mainActivity.mCurrentShowFragment;
                if (currentFragment != null) {
                    ft.hide(currentFragment);
                }
                String simpleName = TodoListFragment.class.getSimpleName();
                TodoListFragment todoListFragment = (TodoListFragment) fm.findFragmentByTag(simpleName);
                ft.show(todoListFragment);
                ft.commitAllowingStateLoss();
            //拿到当前的
        } catch (Exception e) {
                LogUtil.e("Fragment切换失败：" + e.getMessage());
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        currentTodo = null; // 清空数据，避免内存泄漏
    }
}