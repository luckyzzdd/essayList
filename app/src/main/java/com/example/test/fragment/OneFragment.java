package com.example.test.fragment;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.test.database.TodoDatabase;
import com.example.test.databinding.FragmentOneBinding;
import com.example.test.entity.HitokotoBean;
import com.example.test.entity.TodoEntity;
import com.example.test.utils.CacheUtil;
import com.example.test.utils.LogUtil;
import com.example.test.utils.NetWorkUtil;
import com.example.test.utils.TimeUtil;


public class OneFragment extends Fragment {
    private FragmentOneBinding binding;

    // 增加最大重试次数，避免无限循环
    private static final int MAX_RETRY_COUNT = 3;
    // 记录当前重试次数
    private int currentRetryCount = 0;

    private Handler mHandler;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOneBinding.inflate(inflater, container, false);
        mHandler = new Handler(Looper.getMainLooper());
        loadData();
        initView();
        return binding.getRoot();
    }

    private void initView() {
        binding.next.setOnClickListener(v -> {
            loadData();
        });
        binding.add.setOnClickListener(v -> {
            // 1. 获取输入内容，trim()去除前后空格（避免空内容）
            String oneHitokoto = binding.oneHitokoto.getText().toString().trim();
            String oneFrom = binding.oneFrom.getText().toString().trim();
            // 2. 判空，避免无效提交
            if (oneHitokoto.isEmpty() && oneFrom.isEmpty()) {
                return; // 直接返回，不执行后续逻辑
            }
            new Thread(() -> {
                try {
                    //保存到Room数据库里
                    if (getActivity() != null) {

                        TodoEntity todoEntity = new TodoEntity(oneHitokoto, TimeUtil.getTime(), oneFrom);
                        //获取数据库实例，调用DAO的新增方法
                        //Fragment是依托在mainActivity的类，getActivity就是拿到mainActivity的实例
                        TodoDatabase.getINSTANCE(getActivity()).todoDao().insert(todoEntity);

                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "待办已保存到数据库", Toast.LENGTH_SHORT).show();

                            //新增最后的代办到sp里
                            SharedPreferences todoSP = getContext().getSharedPreferences("todoSP", MODE_PRIVATE);
                            SharedPreferences.Editor edit = todoSP.edit();
                            edit.putString("last_todo_content", oneHitokoto);
                            edit.apply();//异步提交
                        });

                    }
                } catch (Exception e) {
                    // 优化2：异常捕获+日志打印（实习必备）
                    LogUtil.e("添加失败：" + e.getMessage());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "添加失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }).start();

        });
    }

    //加载数据
    private void loadData() {
        if (getContext() == null || getActivity() == null) return;
        // 第一步：先显示缓存（解决“请求慢”的问题，瞬间有内容）
        String cacheText = CacheUtil.getHitokotoText(getContext());
        String cacheFrom = CacheUtil.getHitokotoFrom(getContext());
        if (cacheText != null) {
            binding.oneHitokoto.setText(cacheText);
            binding.oneFrom.setText(cacheFrom);
        } else {
            // 无缓存时才显示“加载中”
            binding.oneHitokoto.setText("加载中...");
            binding.oneFrom.setText("");
        }
        binding.next.setVisibility(View.GONE);
        binding.add.setVisibility(View.GONE);
        binding.loadingBar.setVisibility(View.VISIBLE);
        requestHitokoto();
    }

    private void requestHitokoto() {
        //发起请求
        NetWorkUtil.getHitokoto(getContext(), new NetWorkUtil.HitokotoCallback() {
            @Override
            public void onSuccess(HitokotoBean bean) {
                // 获取当前显示的文案
                String currentText = binding.oneHitokoto.getText().toString().trim();
//               对比新内容和当前内容
                if ( currentText.equals(bean.getHitokoto())) {
                    //内容重复，且未到达最大重试次数
                    currentRetryCount++;
                    if (currentRetryCount < MAX_RETRY_COUNT) {
                        Log.d("OneFragment", "内容重复，重新请求（第" + currentRetryCount + "次）");
                        mHandler.postDelayed(()->{ requestHitokoto(); },1000);

                        return; // 不执行后续逻辑
                    } else {
                        // 达到最大重试次数，不再请求，直接使用当前内容
                        Log.d("OneFragment", "已重试" + MAX_RETRY_COUNT + "次，仍重复，停止请求");
                    }
                }
                binding.oneHitokoto.setText(bean.getHitokoto());
                binding.oneFrom.setText(bean.getFrom());
                binding.loadingBar.setVisibility(View.GONE);
                binding.next.setVisibility(View.VISIBLE);
                binding.add.setVisibility(View.VISIBLE);
                CacheUtil.saveHitokotoCache(getContext(), bean.getHitokoto(), bean.getFrom());
                // 重置重试次数
                currentRetryCount = 0;
            }

            @Override
            public void onFail(String errorMsg) {
                Toast.makeText(getContext(), "请求失败", Toast.LENGTH_SHORT).show();
                binding.loadingBar.setVisibility(View.GONE);
                binding.next.setVisibility(View.VISIBLE);
                binding.add.setVisibility(View.VISIBLE);
                Log.e("TodoListLog", errorMsg);
                // 失败时也重置重试次数
                currentRetryCount = 0;
            }
        });
    }
}
