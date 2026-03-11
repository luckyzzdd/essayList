package com.example.test;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.test.broadcast.NetworkReceiver;
import com.example.test.databinding.ActivityMainBinding;
import com.example.test.fragment.AddFragment;
import com.example.test.fragment.TodoListFragment;
import com.example.test.fragment.OneFragment;
import com.example.test.service.SyncService;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    public FragmentManager fragmentManager;

    // 权限申请启动器
    private ActivityResultLauncher<Intent> storageSettingLauncher;
    private ActivityResultLauncher<String[]> storagePermissionLauncher;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    // Fragment列表
    private List<Fragment> list = new ArrayList<>();
    // 网络广播接收器
    private NetworkReceiver networkReceiver;
    // 防抖变量（可根据需要调整间隔，比如500ms）
    private long lastNavClickTime = 0;
    private static final long NAV_CLICK_INTERVAL = 500;

    // 当前显示的Fragment（手动维护，核心）
    public Fragment mCurrentShowFragment;
    // Fragment事务锁
    private boolean isFragmentTransactionExecuting = false;

    public static final String TAG = "MAIN_ACTIVITY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 设置沉浸式状态栏内边距
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // 初始化背景、权限、底部导航等
        setBackGroud();
        Log.d("TodoListLog", "此设备的sdk" + String.valueOf(Build.VERSION.SDK_INT));
        initPermissionLaunchers();
        initBottomNav();
        checkNotificationPermissionAndStartService();
        registerNetworkReceiver();
        readLastTodoFromSP();

        // 停止服务按钮点击事件
        Intent serviceIntent = new Intent(this, SyncService.class);
        binding.btnStopService.setOnClickListener(v -> {
            stopService(serviceIntent);
            Toast.makeText(this, "同步服务已停止", Toast.LENGTH_SHORT).show();
        });
    }

    // ===================== 对外暴露的方法 =====================
    public boolean isFragmentTransactionExecuting() {
        return isFragmentTransactionExecuting;
    }

    public void setFragmentTransactionExecuting(boolean fragmentTransactionExecuting) {
        isFragmentTransactionExecuting = fragmentTransactionExecuting;
    }

    public ActivityMainBinding getBinding() {
        return binding;
    }

    // 对外暴露：跳转到AddFragment并传递参数（给Adapter调用）
    public void switchToAddFragment(Bundle args) {
        Fragment addFragment = list.get(2);
        showFragment(addFragment, args);
    }

    /**
     * 注册网络状态广播接收器
     */
    private void registerNetworkReceiver() {
        networkReceiver = new NetworkReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, filter);
    }
    /**
     * 初始化底部导航栏
     */
    private void initBottomNav() {
        // 初始化Fragment列表
        list.add(new OneFragment());
        list.add(new TodoListFragment());
        list.add(new AddFragment());
        mCurrentShowFragment = null;

        // 默认显示TodoListFragment
        showFragment(list.get(1));
        binding.bottomNav.setSelectedItemId(R.id.nav_todo_list);

        // 底部导航点击事件
        binding.bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                // 防抖处理
                long currentTime = System.currentTimeMillis();
                // 只有“同一按钮短时间重复点击”才拦截
                if (currentTime - lastNavClickTime < NAV_CLICK_INTERVAL &&
                        menuItem.getItemId() == binding.bottomNav.getSelectedItemId()) {
                    Log.d(TAG, "短时间重复点击同一导航，拦截");
                    return true;
                }
//                if (currentTime - lastNavClickTime < NAV_CLICK_INTERVAL) {
//                    return true;
//                }
                lastNavClickTime = currentTime;
                int itemId = menuItem.getItemId();
                if (itemId == R.id.nav_one) {
                    showFragment(list.get(0));
                } else if (itemId == R.id.nav_todo_list) {
                    showFragment(list.get(1));
                } else if (itemId == R.id.nav_add_todo) {
                    showFragment(list.get(2));
                }
                return true;
            }
        });
    }

    /**
     * 设置背景
     */
    private void setBackGroud() {
        Drawable background = getResources().getDrawable(R.drawable.flower, getTheme());
        background.setAlpha(120);
        binding.main.setBackground(background);
    }

    /**
     * 初始化权限申请启动器
     */
    private void initPermissionLaunchers() {
        // 1. 跳系统设置申请MANAGE_EXTERNAL_STORAGE权限
        storageSettingLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (hasStoragePermission()) {
                        Toast.makeText(MainActivity.this, "所有文件访问权限已授予", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "未授予所有文件访问权限，备份功能受限", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // 2. 动态申请普通存储权限
        storagePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean allGranted = true;
                    for (Boolean granted : permissions.values()) {
                        if (granted == null || !granted) {
                            allGranted = false;
                            break;
                        }
                    }
                    if (allGranted) {
                        Toast.makeText(MainActivity.this, "存储权限申请成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "权限申请失败，备份/读写外部存储功能无法使用", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // 3. 动态申请通知权限
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Toast.makeText(this, "通知权限已授予，启动同步服务", Toast.LENGTH_SHORT).show();
                        startSyncService();
                    } else {
                        Toast.makeText(this, "通知权限未授予，前台服务无法正常运行", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * 读取最后一个待办事项（从SP）
     */
    private void readLastTodoFromSP() {
        SharedPreferences todoSP = this.getSharedPreferences("todoSP", Context.MODE_PRIVATE);
        String lastTodo = todoSP.getString("last_todo_content", "暂无");
        Toast.makeText(this, "上次添加的记录：" + lastTodo, Toast.LENGTH_SHORT).show();
    }

    // ===================== Fragment切换核心逻辑 =====================

    /**
     * 无参数的Fragment切换（默认）
     */
    private void showFragment(Fragment targetFragment) {
        showFragment(targetFragment, null);
    }

    /**
     * 带参数的Fragment切换（核心修复）
     */
    private void showFragment(Fragment targetFragment, Bundle args) {
        // 1. 基础安全校验（放宽锁的拦截，只拦截真·并发）
        if (targetFragment == null || isFinishing() || isDestroyed()) {
            return;
        }
        // 事务锁只拦截“正在执行的事务”，不拦截重复点击（重复点击靠下面的状态判断）
        if (isFragmentTransactionExecuting) {
            Log.d(TAG, "事务正在执行，跳过本次切换");
            return;
        }
        // 2. 避免重复切换同一个Fragment
        if (mCurrentShowFragment == targetFragment) {
            // 如果是AddFragment且有新参数，强制更新参数和UI
            if (targetFragment instanceof AddFragment && args != null) {
                targetFragment.setArguments(args);
                ((AddFragment) targetFragment).updateTodoData();
            }
            // 强制刷新：如果Fragment应该显示但实际隐藏了，手动show
            if (targetFragment.isAdded() && targetFragment.isHidden()) {
                getSupportFragmentManager().beginTransaction()
                        .show(targetFragment)
                        .commitNowAllowingStateLoss(); // 同步提交，立即生效
            }
            // 更新底部导航（兜底）
            updateBottomNavSelection(targetFragment);
            return;
        }

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        isFragmentTransactionExecuting = true; // 加锁

        try {
            // 3. 隐藏当前显示的Fragment
            if (mCurrentShowFragment != null && mCurrentShowFragment.isAdded()) {
                ft.hide(mCurrentShowFragment);
            }

            // 4. 处理目标Fragment
            String fragmentTag = targetFragment.getClass().getSimpleName();
            Fragment existingFragment = findAddedFragmentByType(fm, targetFragment.getClass());

            if (existingFragment == null) {
                // 首次添加：设置参数（如果有）
                if (args != null) {
                    targetFragment.setArguments(args);
                }
                ft.add(R.id.fragment_container, targetFragment, fragmentTag);
                Log.d(TAG, "首次添加Fragment：" + fragmentTag);
            } else {
                // 复用已有Fragment：更新参数+刷新UI
                if (args != null) {
                    existingFragment.setArguments(args);
                    // 如果是AddFragment，主动调用刷新方法
                    if (existingFragment instanceof AddFragment) {
                        ((AddFragment) existingFragment).updateTodoData();
                    }
                }
                ft.show(existingFragment);
                // 替换为已存在的实例（关键，保证状态一致）
                targetFragment = existingFragment;
                Log.d(TAG, "复用Fragment：" + fragmentTag);
            }


            if (!fm.isStateSaved()) {
                ft.commitNowAllowingStateLoss(); // 同步提交，UI立即刷新
            } else {
                ft.commitAllowingStateLoss(); // 仅当状态已保存时用异步
            }
            // 6. 更新当前显示的Fragment标记
            mCurrentShowFragment = targetFragment;

            // 7. 更新底部导航选中状态
            updateBottomNavSelection(targetFragment);

        } catch (Exception e) {
            Log.e("MainActivity", "Fragment切换失败：" + e.getMessage());
        } finally {
            // 解锁（必须放在finally，确保无论是否异常都能解锁）
            isFragmentTransactionExecuting = false;
        }
    }

    /**
     * 根据Fragment类型更新底部导航选中状态
     */
    private void updateBottomNavSelection(Fragment fragment) {
        if (fragment instanceof OneFragment) {
            binding.bottomNav.setSelectedItemId(R.id.nav_one);
        } else if (fragment instanceof TodoListFragment) {
            binding.bottomNav.setSelectedItemId(R.id.nav_todo_list);
        } else if (fragment instanceof AddFragment) {
            binding.bottomNav.setSelectedItemId(R.id.nav_add_todo);
        }
    }

    /**
     * 查找已添加的同类型Fragment
     */
    private Fragment findAddedFragmentByType(FragmentManager fm, Class<? extends Fragment> fragmentClass) {
        for (Fragment f : fm.getFragments()) {
            if (f != null && f.isAdded() && fragmentClass.isInstance(f)) {
                return f;
            }
        }
        return null;
    }

    // ===================== 权限和服务相关 =====================

    /**
     * 检查存储权限（适配全版本）
     */
    public boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * 检查通知权限并启动同步服务
     */
    public void checkNotificationPermissionAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                startSyncService();
            }
        } else {
            startSyncService();
        }
    }

    /**
     * 启动同步服务（适配8.0+前台服务）
     */
    private void startSyncService() {
        Intent intent = new Intent(this, SyncService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    /**
     * 申请存储权限（对外调用）
     */
    public void checkAndRequestStoragePermission() {
        if (!hasStoragePermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName().trim()));
                if (storageSettingLauncher != null) {
                    storageSettingLauncher.launch(intent);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
                storagePermissionLauncher.launch(permissions);
            } else {
                String[] permissions = {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                };
                storagePermissionLauncher.launch(permissions);
            }
        } else {
            Toast.makeText(this, "存储权限已授权", Toast.LENGTH_SHORT).show();
        }
    }

    // ===================== 生命周期 =====================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 解绑广播接收器
        if (networkReceiver != null) {
            unregisterReceiver(networkReceiver);
        }
        // 置空Binding，避免内存泄漏
        binding = null;
    }


}