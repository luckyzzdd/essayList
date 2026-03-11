# ListAPP

## 前置知识

### 生命周期

#### activity生命周期

```
onCreate() → onStart() → onResume() → [用户交互] → onPause() → onStop() → onDestroy()
```

**启动 Activity**：`onCreate() → onStart() → onResume()`

**按 Home 键切后台**：`onPause() → onStop()`

**从后台切回前台**：`onRestart() → onStart() → onResume()`

**按返回键退出**：`onPause() → onStop() → onDestroy()`

#### fragment生命周期

onAttach()  与 Activity 建立关联

onCreate() 实例初始化

onCreateView()  创建 B 的视图（加载布局，返回 View 对象）；

onViewCreated()  View 创建完成,可操作本控件，比如 findViewById

onStart() 可见

onResume() 可交互

onPause()  不可交互

onStop()  不可见

onDestroyView()  销毁这个视图

onDestroy() 销毁这个组件

onDetach()

特殊的生命周期

onHiddenChanged()来处理隐藏和可见

#### service生命周期

启动式Service

```
onCreate() → onStartCommand() [每次调用 startService() 时]→ [后台运行] → onDestroy()
```

特点：和启动者（如 Activity）解耦，即使启动者销毁，Service 仍会后台运行，需手动调用 `stopService()`/`stopSelf()` 停止。

关键：`onStartCommand()` 可多次调用（每次 `startService()` 都会触发），但 `onCreate()` 仅首次创建时执行 1 次。

绑定式Service

```
onCreate() → onBind()[调用 bindService() 时] → [和绑定者通信] → onUnbind()[绑定者解除绑定（或销毁）时] → onDestroy()
```

特点：和绑定者（如 Activity）生命周期绑定，绑定者销毁时自动触发 `onUnbind()`，Service 若无其他绑定者则销毁。

关键：`onBind()` 返回 `IBinder` 对象，用于绑定者和 Service 通信。

重要提醒

Service 默认运行在**主线程**！若执行耗时操作（如网络请求、文件读写），必须手动创建子线程，否则会导致 ANR。

后台 Service 受 Android 版本限制（Android 8.0+ 后台 Service 会被系统限制），耗时后台任务建议用 `WorkManager` 替代。



#### BroadcastReceiver生命周期

BroadcastReceiver 是「消息接收器」，生命周期**极短**（仅处理接收到的广播），没有长期存活的状态。

```
onReceive() → [执行完毕] → 销毁（被系统回收）
```

触发时机：当接收到匹配的广播（如系统广播、自定义广播）时，系统会创建 BroadcastReceiver 实例，调用 `onReceive()`。

生命周期长度：`onReceive()` 执行时间**不能超过 10 秒**，否则系统会判定为 ANR 并杀死进程。

重要提醒

`onReceive()` 运行在主线程，**不能执行耗时操作**！若需耗时处理，应启动 Service

动态注册的 BroadcastReceiver 需在 `onDestroy()` 中注销（如 Activity 销毁时），否则会导致内存泄漏。

### Context类和核心组件的关系

#### **最初父类**

Context（抽象基类）定义了核心能力：访问资源（`getResources()`）、启动组件（`startActivity()`/`startService()`）、获取系统服务（`getSystemService()`）、访问 SharedPreferences 等。

#### **膝下二子**

**1.ContextImpl**（Context的真正的实现类）所有 Context 的核心方法（如 `getResources()`、`getPackageManager()`）最终都由 `ContextImpl` 实现。普通开发者几乎不会直接接触它，Android 框架内部通过「装饰模式」封装在 `ContextWrapper` 中。

2.**ContextWrapper** （包装类）               

字面意思是「上下文包装器」，持有一个 `ContextImpl` 实例，所有方法都委托给 `ContextImpl` 执行。

它的核心作用是**让子类（Activity/Service/Application）可以方便地扩展 Context 能力**，而无需重写所有抽象方法。

ContextThemeWrapper（带主题的包装类），Service、Application 直接继承自 `ContextWrapper`





**ContextThemeWrapper（带主题的包装类）**

- 继承自 `ContextWrapper`，额外增加了「主题（Theme）」相关的能力（如 `setTheme()`、`getTheme()`）。
- Activity 继承自 `ContextThemeWrapper`（因为 Activity 有界面，需要主题控制样式），而 Service/Application 不需要主题，所以直接继承 `ContextWrapper`。



虽然 Activity、Service、Application 都属于 Context，但它们的「生命周期」「能力范围」「使用场景」差异极大，用错会导致崩溃或内存泄漏。



|       特性        |      Application Context      |      Service Context       |     Activity Context     |
| :---------------: | :---------------------------: | :------------------------: | :----------------------: |
|     生命周期      |    与应用进程一致（最长）     |  与 Service 生命周期绑定   | 与 Activity 生命周期绑定 |
|   主题（Theme）   |    ❌ 无（无法设置 / 使用）    |            ❌ 无            |     ✅ 有（核心能力）     |
|   启动 Activity   | ⚠️ 需加 FLAG_ACTIVITY_NEW_TASK |           ⚠️ 同左           |        ✅ 直接启动        |
|  加载布局 / 资源  |    ✅ 可加载（无主题限制）     |           ✅ 同左           |    ✅ 可加载（带主题）    |
| 弹出 Dialog/Toast |   ❌ 无法弹 Dialog（需窗口）   |      ❌ 无法弹 Dialog       |      ✅ 可弹所有弹窗      |
|   内存泄漏风险    |  ⚠️ 低（但静态引用仍会泄漏）   | ⚠️ 中（Service 销毁前引用） |     ⚠️ 高（最易泄漏）     |

#### 经典错误

用 Application Context 启动 Activity 不加 FLAG

```
// 错误写法：崩溃（android.util.AndroidRuntimeException: Calling startActivity() from outside of an Activity context requires the FLAG_ACTIVITY_NEW_TASK flag）
getApplicationContext().startActivity(new Intent(this, SecondActivity.class));

// 正确写法：加 FLAG
Intent intent = new Intent(this, SecondActivity.class);
intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
getApplicationContext().startActivity(intent);
```

用 Service Context 弹出 Dialog

```
// 错误写法：
AlertDialog.Builder builder = new AlertDialog.Builder(this); // this 是 Service
builder.setTitle("提示").show();

// 正确替代：用 Notification 代替 Dialog（Service 无窗口，无法弹 Dialog）
NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("提示")
        .setContentText("Service 运行中")
        .setSmallIcon(R.mipmap.ic_launcher)
        .build();
manager.notify(1, notification);
```

#### 不同Context获取方式

##### **1. Activity 中获取 Context**

```
// 方式1：直接用 this（Activity 本身就是 Context）
Context context = this;

// 方式2：获取 Application Context（全局上下文）
Context appContext = getApplicationContext();

// 方式3：获取 Base Context（ContextWrapper 持有的基础 Context）
Context baseContext = getBaseContext(); // 一般不用，除非特殊扩展场景
```

##### **2. Service 中获取 Context**

```
// 方式1：直接用 this（Service 本身就是 Context）
Context context = this;

// 方式2：获取 Application Context
Context appContext = getApplicationContext();
```

##### **3. Fragment 中获取 Context**

```
// 方式1：onAttach 回调中获取（最安全，避免空指针）
@Override
public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    this.mContext = context;
}

// 方式2：getActivity()（需判空，因为 Fragment 可能脱离 Activity）
Context context = getActivity();
if (context == null) return;
```

4. ##### **BroadcastReceiver 中获取 Context**

```
// onReceive 方法的第一个参数就是 Context（Receiver 上下文）
@Override
public void onReceive(Context context, Intent intent) {
    // 注意：这个 Context 是临时的，Receiver 执行完就销毁，不要长期持有
    Toast.makeText(context, "广播接收成功", Toast.LENGTH_SHORT).show();
}
```

### 内存泄漏

错误写法

```
 // 静态变量：生命周期和App进程一样长（直到App被杀死）
    private static Context mContext;
```

正确写法（弱引用避免泄漏）

// 弱引用：不会阻止系统回收对象    private WeakReference<Context> mContextRef;



**内存泄漏 = 程序中不再使用的对象，被错误地持有引用，导致系统无法回收它占用的内存**。

#### 为什么会泄漏？

1. Activity 的生命周期：你退出 MainActivity（按返回键），Activity 本应该被系统回收（储物间拿走）；
2. 但静态变量`mContext`还死死 “抓着” 这个 Activity 的 Context 不放；
3. 系统想回收 Activity，却发现还有引用指向它，只能让它留在内存里 —— 这就是内存泄漏。
4. 如果你反复进入 / 退出这个 Activity，内存里会堆一堆 “死” 的 Activity，最终 OOM（内存溢出）。

#### 看不见的引用

非静态内部类/匿名内部类，会默认持有外部类的「强引用」。。因为在这些内部类里我们可以直接访问外部类的成员变量，这是编译器自动加的引用。。

```
 // 1. 非静态内部类 MyTask → 编译器自动加了指向 MainActivity 的强引用
    private class MyTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            // 模拟耗时10秒：此时即使你退出Activity（按返回键），
            // MyTask 还在后台运行，它持有的 Activity 引用就会让 Activity 无法被回收！
            try { Thread.sleep(10000); } catch (Exception e) {}
            return null;
        }
    }
    //启动
    启动异步任务 → MyTask 实例被创建，持有 Activity 强引用
        new MyTask().execute();
```



```
// 1. Handler 是匿名内部类 → 编译器自动加了指向 MainActivity 的强引用
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // 空实现，但引用还在！
        }
    };
    
    
    / 2. 发送延迟10秒的消息 → 消息会被放到 Looper 的消息队列里，
        // 而消息持有 Handler 的引用，Handler 又持有 Activity 的引用！
        mHandler.postDelayed(() -> {}, 10000);
```

非静态内部类持有的外部类引用是外部类实例的引用，它是需要调用外部类的方法，成员变量一类的

为什么静态内部类不持有外部类引用？

因为它设计出来就是作为一个独立类存在的，仅挂在外部类名下。

还有非静态变量和静态变量？？

|  变量类型  |    归属对象     |                    生命周期                    |
| :--------: | :-------------: | :--------------------------------------------: |
| 非静态变量 |  Activity 实例  | 和 Activity 实例一致（退出 Activity 就被回收） |
|  静态变量  | MainActivity 类 |    和 App 进程一致（进程不死，变量不消失）     |

静态内部类本身是安全的（不会泄漏）；

静态变量本身不是罪魁祸首 ——**罪魁祸首是你把 Activity 实例赋值给静态变量**（`mContext = this`），导致 Activity 被静态变量「粘」在内存里。





### 下载调试的包

单个MainActivity

Gradle的打的包，需要手动构建完然后可以拿，只是运行拿不到的

下载的地址D:\pro\android_pro\TodoList\app\build\outputs\apk\debug\





## 一模块，底部导航栏

### 模块描述

通过点击底部不同的标签来切换fragment

### 1底部导航栏控件

````
    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottom_nav"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@android:color/transparent"
        app:itemActiveIndicatorStyle="@null"
        app:itemIconTint="@color/bottom_nav_color"
        app:itemRippleColor="@android:color/transparent"
        app:itemTextColor="@color/bottom_nav_color"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:menu="@menu/bottom_nav_menu" />
````

绑定下面的menu.xml



遇到的属性

1app:itemIconTint="@color/bottom_nav_color" ，字体颜色

ReSource里拿东西是按资源类型来拿

bottom_nav_color.xml

```
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="@color/grey_dark" android:state_checked="false"/>
    <item android:color="@color/pink" android:state_checked="true"/>
</selector>
```

注意：

所有 Selector 和 Shape 文件都放在 `res/drawable` 目录下

最终通过 `android:background` 或 `android:src` 属性应用到控件上。

shaper是静态样式，用来定义圆角、渐变、描边等几何形状，核心属性：`solid`（填充）、`corners`（圆角）、`stroke`（描边）、`gradient`（渐变）。

selector是状态选择器，根据控件的状态来切换样式，核心规则：状态项从上到下匹配，默认状态放最后



app:itemActiveIndicatorStyle="@null"



### 2.一个menu标签为根标签的xml

```
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- 待办列表项 -->
    <item
        android:id="@+id/nav_one"
        android:title="一句"
        android:icon="@mipmap/juzi" />
    <item
        android:id="@+id/nav_todo_list"
        android:title="清单"
        android:icon="@mipmap/list" />
    <!-- 新增待办项 -->
    <item
        android:id="@+id/nav_add_todo"
        android:title="新增"
        android:icon="@mipmap/add" />
</menu>
```



### 3.导航的切换（重点）

放到activityonCreate生命周期里，在它初始化的时候开始切换。。。

```
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

    
  
```

两个重要方法

  binding.bottomNav.setSelectedItemId(R.id.nav_todo_list);**

  **binding.bottomNav.setOnItemSelectedListener**

## 二模块，style.xml

当默认的按钮属性我不喜欢，就可以在Resource里自定义样式

```
<?xml version="1.0" encoding="utf-8"?>
<resources>

    <!-- 核心：可复用的黑白简约按钮样式 -->
    <style name="Widget.Button.MinimalBlackWhite" parent="Widget.Material3.Button">
        <!-- 黑白简约核心属性 -->
        <item name="backgroundTint">@color/black</item>          <!-- 黑色背景 -->
        <item name="android:textColor">@color/white</item>       <!-- 白色文字 -->
        <item name="cornerRadius">4dp</item>                     <!-- 4dp小圆角 -->
        <item name="strokeWidth">0dp</item>                      <!-- 无边框 -->
        <item name="rippleColor">@color/gray_200</item>          <!-- 浅灰水波纹（极简反馈） -->
        <item name="elevation">0dp</item>                        <!-- 去掉阴影 -->
        <item name="android:textSize">14sp</item>                <!-- 统一文字大小 -->
        <item name="android:paddingHorizontal">20dp</item>       <!-- 统一水平内边距 -->
        <item name="android:paddingVertical">10dp</item>         <!-- 统一垂直内边距 -->
    </style>

    <!-- 可选：边框版黑白简约按钮（透明背景+黑边框） -->
    <style name="Widget.Button.MinimalBlackWhite.Outlined" parent="Widget.Material3.Button.OutlinedButton">
        <item name="backgroundTint">@android:color/transparent</item> <!-- 透明背景 -->
        <item name="strokeWidth">1dp</item>                           <!-- 1dp黑边框 -->
        <item name="strokeColor">@color/black</item>                   <!-- 边框色 -->
        <item name="android:textColor">@color/black</item>             <!-- 黑色文字 -->
        <item name="cornerRadius">4dp</item>                           <!-- 小圆角 -->
        <item name="rippleColor">@color/gray_200</item>                <!-- 浅灰水波纹 -->
        <item name="android:textSize">14sp</item>                      <!-- 统一文字大小 -->
    </style>
    
</resources>
```



然后在在控件里用，style="@style/Widget.Button.MinimalBlackWhite.Outlined"这个属性



## 三模块 fragment切换（涉及模块1）

#### 基础架构（Activity 托管 Fragment）



```
MainActivity（容器）→ FragmentManager/FragmentTransaction → 多个(FragmentOne/TodoList/Add)
```

**容器**：Activity 中用`FrameLayout`（id: fragment_container）承载 Fragment；

**管理方式**：用`hide/show`复用 Fragment（优于`replace`，避免重建 Fragment 导致数据丢失 / 性能损耗）；

**状态维护**：手动维护`mCurrentShowFragment`（当前显示的 Fragment 实例），不依赖`FragmentManager`的滞后状态（实习核心考点）。

#### 核心步骤1，防抖校验，仅拦截同一按钮重复点击，在监听导航栏setOnItemSelectedListener这里

```
			 // 防抖处理
                long currentTime = System.currentTimeMillis();
                // 只有“同一按钮短时间重复点击”才拦截
                if (currentTime - lastNavClickTime < NAV_CLICK_INTERVAL &&
                        menuItem.getItemId() == binding.bottomNav.getSelectedItemId()) {
                    Log.d(TAG, "短时间重复点击同一导航，拦截");
                    return true;
                }
              //这个是所有按钮  短时间点击，包含切换。。。但这个是完全错误的，因为会导致导航栏的标签被选择，但因为太快，而return，fragment并不展示
           if (currentTime - lastNavClickTime < NAV_CLICK_INTERVAL) {
                  return true;
               }
                lastNavClickTime = currentTime;
```

#### 核心步骤2校验

安全验证，（Activity非空，非销毁），非事务中



核心步骤3事务锁和mCurrentShowFragment



```
  在模块1中有监听导航栏的切换，每次换id，然后执行那个item的需要showFragment
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
            //通过类型来找是否有同类型的Fragment在这个栈里
            Fragment existingFragment = findAddedFragmentByType(fm, targetFragment.getClass());

///把那个方法加入笔记里，算是伪代码
   /**
     * 查找已添加的同类型Fragment
     */
     // Class<? extends Fragment>  ?是通配符，extends Fragment 必须是 Fragment 本身，或 Fragment 的子类
    private Fragment findAddedFragmentByType(FragmentManager fm, Class<? extends Fragment> fragmentClass) {
        for (Fragment f : fm.getFragments()) {
            if (f != null && f.isAdded() && fragmentClass.isInstance(f)) {
                return f;
            }
        }
        return null;
    }
///


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

			//判断系统有没有保存activity的状态，避免“保存的状态” 和 “实际的 UI 状态”不 一致
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
```



### 遇到的问题

1**防抖逻辑过度拦截**。只需要拦截同一按钮短时间重复点击，所有按钮  短时间点击，包含切换。。。但这个是完全错误的，因为会导致导航栏的标签被选择，但因为太快，而return，fragment并不展示

2**复用 Fragment 参数不更新**，，之前是在addFragment的onCreateView生命周期里实现拿参数，现在还要在onResume里加上这个 updateTodoData() 

```
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
```

3细节上的问题，FragmentManager.findFragmentById(R.id.Container）`findFragmentById` 不是拿「最后 add 入栈的 Fragment」，而是拿「当前绑定在指定 container_id 上的 Fragment 实例」..FragmentManager 处理 `hide/show/add` 这些事务是**异步的**,,可能系统还没处理好状态。。

4,之前在todoadapter上总是自己去new一个新的addFragment,为了保证单例，就可以之前去复用mainActivity里的这个。

5Fragment 切换用 hide/show 还是 replace

优先 hide/show（复用实例，减少重建开销），replace 会销毁 Fragment，导致数据丢失 / 性能差（本次项目就是用 hide/show 解决重叠问题）；

6手动维护锁是为了防止并发，为啥不用系统的 synchronized?对于 Fragment 切换这个场景，用布尔值标记就够了（轻量、简单）只需要 “拦截并发请求”，不是 “等待并发请求”

7手动维护mCurrentShowFragment原因

```
// 你执行：显示FragmentB
ft.show(fragmentB).hide(fragmentA).commit(); 
// 此时调用FragmentManager的方法查“当前显示的Fragment”，大概率拿到的还是A（旧状态）
Fragment current =getSupportFragmentManager().findFragmentById(R.id.container); 
// 但你手动维护的mCurrentShowFragment = fragmentB（和你想要的状态一致）
```

FragmentManager 对 Fragment 状态的管理是 **“异步更新”** 的, 调用 `commit()` 后拿到的可能是**旧状态**，FragmentManager 不会立刻更新状态，而 `mCurrentShowFragment` 是手动维护的内存变量，执行 `hide/show` 后立刻更新，能保证 “内存状态和 UI 实际显示的 Fragment 一致”。

关于FragmentManager本身的异步和同步，于Fragment状态的同步性

```
// 1. 异步提交add事务
ft.add(targetFragment).commit(); 
// 2. 此时调用isAdded()，返回false（因为commit是异步，事务还没执行，mAdded没更新）
Log.d("test", targetFragment.isAdded() + ""); // false
// 3. 强制执行异步事务
getSupportFragmentManager().executePendingTransactions();
// 4. 再调用isAdded()，返回true（事务执行完，mAdded已更新）
Log.d("test", targetFragment.isAdded() + ""); // true
```

### 知识点

#### 实例获取类（最常用，但易滞后，实习必考）

|              方法               |                     用途                      |                       坑点 / 实习考点                        |
| :-----------------------------: | :-------------------------------------------: | :----------------------------------------------------------: |
|   `findFragmentById(int id)`    | 根据「容器 ID/Fragment ID」查询 Fragment 实例 | ① 滞后性：异步事务没执行完时，拿到旧实例；② 实习考点：容器 ID 是`R.id.fragment_container`，Fragment ID 是 add 时设置的 id |
| `findFragmentByTag(String tag)` |        根据 add 时设置的 tag 查询实例         | ① 滞后性和上面一致；② 实习考点：tag 必须唯一，否则查错；③ 比 findFragmentById 更推荐（ID 易冲突） |
|        `getFragments()`         | 返回 FragmentManager 管理的所有 Fragment 列表 | ① 滞后 + 耗时：遍历列表慢，且列表是 “记录的状态” 而非实时 UI 状态；② 坑点：新手常遍历这个列表找 “当前显示的 Fragment”，导致切换慢 |

#### 2. 状态查询类（易误当 “锁”，导致慢，实习高频坑点）

表格

|              方法              |                       用途                       |                       坑点 / 实习考点                        |
| :----------------------------: | :----------------------------------------------: | :----------------------------------------------------------: |
|        `isStateSaved()`        | 判断 Activity 状态是否已保存（如切后台、横竖屏） | ① 坑点：新手误用来拦截切换（当成 “锁”），导致合法切换被拦截；② 实习考点：仅在「生命周期中切换 Fragment」时用（避免崩溃），日常点击切换不用 |
| `executePendingTransactions()` |           强制执行所有待处理的异步事务           | ① 坑点：同步阻塞主线程，强制等事务执行完，导致切换慢；② 实习考点：仅在 Activity 销毁前用（确保事务执行完），日常不用 |
|   `getBackStackEntryCount()`   |           获取回退栈中 Fragment 的数量           |       实习考点：用于判断 “是否能按返回键回退 Fragment”       |

#### 3. 事务控制类（核心，异步执行，实习必考）

|               方法                |                        用途                         |                   坑点 / 实习考点                    |
| :-------------------------------: | :-------------------------------------------------: | :--------------------------------------------------: |
|       `beginTransaction()`        | 创建 FragmentTransaction 对象（用于 hide/show/add） | 实习考点：所有 Fragment 切换都要通过这个方法创建事务 |
|         `popBackStack()`          |              异步回退 Fragment 回退栈               |     实习考点：和`commit()`一样异步，不会立刻生效     |
|     `popBackStackImmediate()`     |                   同步回退回退栈                    |       坑点：同步阻塞，导致切换慢，仅特殊场景用       |
| `addOnBackStackChangedListener()` |                   监听回退栈变化                    |   实习考点：处理 “返回键切换 Fragment” 的核心监听    |

#### 

### 其他知识点

#### Activty的状态保存

activity 的 “保存状态”：是 UI 组件的可恢复数据（View 文字、Fragment 显示状态、自定义业务数据），不是 “前台 / 后台” 这种运行状态；

触发保存的场景：切后台、横竖屏切换、分屏、系统回收内存；

核心影响：状态保存后（`isStateSaved()=true`），不能提交 Fragment 事务（会崩溃），只能在状态未保存时（前台正常运行）操作。为的是保证 “保存的状态” 和 “实际的 UI 状态” 一致，快照和实际状态不一致，恢复时 UI 错乱，所以安卓禁止并抛崩溃

## 四模块，RecyclerView

1 TodoAdapter

2 ViewHolder

3 ItemDecoration

4 ItemAnimator

5 LayoutManager



适配器和ViewHolder

### 核心思路

1布局文件

宿主布局（activity_main.xml），添加RecyclerView控件，item 布局（item_todo.xml）

2 你需要列表展示的实体类，（TodoEntity.java）

3Adapter + ViewHolder（核心）

onCreateViewHolder-----创建 ViewHolder（加载 Item 布局）

onBindViewHolder------绑定数据到 ViewHolder

getItemCount()--------返回数据总数 

4更新数据

notifyDataSetChanged();全量刷新

还有增量刷新，此项目采用增量刷新

5.在activity或者Fragment初始化 RecyclerView

```
	 //创建Adapter
        todoAdapter = new TodoAdapter(getContext(),this::handlerTodoDelete);
        //设置Adapter
        binding.RV.setAdapter(todoAdapter);
        //设置布局管理器
        binding.RV.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.RV.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
```



```
public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.VH> {
    private List<TodoEntity> list = new ArrayList<>();
    private Context context;
    private DeleteData deleteData;
    private String TAG = "Todo_Adapter";
	//构造函数，传入上下文和回调函数
    public TodoAdapter(Context context, DeleteData deleteData) {
        this.context = context;
        this.deleteData = deleteData;
    }

    @NonNull
    @Override
    public TodoAdapter.VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ItemListBinding binding = ItemListBinding.inflate(layoutInflater);
        return new VH(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        int realPosition = holder.getAdapterPosition();
        if (realPosition == RecyclerView.NO_POSITION) return;
        TodoEntity todo = list.get(realPosition);

        // 绑定数据
        holder.binding.Tv0.setText(todo.getTitle());
        holder.binding.Tv1.setText(todo.getContent());
        holder.binding.Tv2.setText(todo.getCreateTime());
        holder.binding.Tv3.setText(todo.isSync() ? "已同步" : "未同步");
        holder.binding.Im0.setImageResource(todo.isSync() ? R.mipmap.circle_selected : R.mipmap.circle_unselected);

        // 点击事件：调用MainActivity的统一方法
        holder.itemView.setOnClickListener(v -> {
            try {
                // 1. 上下文校验
                if (!(context instanceof MainActivity) || ((MainActivity) context).isFinishing() || ((MainActivity) context).isDestroyed()) {
                    Toast.makeText(context, "上下文异常", Toast.LENGTH_SHORT).show();
                    return;
                }

                MainActivity mainActivity = (MainActivity) context;
                // 2. 事务锁校验
                if (mainActivity.isFragmentTransactionExecuting()) {
                    Log.d(TAG, "事务正在执行，跳过点击");
                    return;
                }

                // 3. 封装参数
                Bundle bundle = new Bundle();
                bundle.putParcelable("todo_data", todo);

                // 4. 调用MainActivity的切换方法（核心！统一逻辑）
                mainActivity.switchToAddFragment(bundle);

                Log.d(TAG, "请求切换到AddFragment，参数：" + todo.getTitle());
            } catch (Exception e) {
                LogUtil.e("点击待办跳转失败：" + e.getMessage());
                Toast.makeText(context, "跳转失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // 长按删除事件
        holder.itemView.setOnLongClickListener(v -> {
            alertDialog(todo, realPosition); // 用realPosition避免错位
            return true;
        });
    }

    // 其余代码保持不变（alertDialog、update、VH、TodoDiffCallback、DeleteData接口等）
    private void alertDialog(TodoEntity todoEntity, int position) {
        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle("温馨删除")
                .setMessage("确定要删除这条待办事项吗")
                .setNegativeButton("取消", (dialog, which) -> Toast.makeText(context, "取消成功", Toast.LENGTH_SHORT).show())
                .setPositiveButton("确定", (dialog, which) -> {
                    if (deleteData != null) {
                        deleteData.delete(todoEntity, position);
                    }
                })
                .create();
        alertDialog.show();
    }

    public void update(List<TodoEntity> listTodo) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new TodoDiffCallback(list, listTodo));
        list.clear();
        list.addAll(listTodo);
        result.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class VH extends RecyclerView.ViewHolder {
        ItemListBinding binding;

        public VH(@NonNull ItemListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static class TodoDiffCallback extends DiffUtil.Callback {
        private List<TodoEntity> oldList;
        private List<TodoEntity> newList;

        public TodoDiffCallback(List<TodoEntity> oldList, List<TodoEntity> newList) {
            this.newList = newList;
            this.oldList = oldList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            TodoEntity oldTodo = oldList.get(oldItemPosition);
            TodoEntity newTodo = newList.get(newItemPosition);
            return oldTodo.getTitle().equals(newTodo.getTitle())
                    && oldTodo.getContent().equals(newTodo.getContent())
                    && oldTodo.getCreateTime().equals(newTodo.getCreateTime())
                    && oldTodo.isSync() == newTodo.isSync();
        }
    }

    public interface DeleteData {
        void delete(TodoEntity todoEntity, int position);
    }

    public List<TodoEntity> getList() {
        return list;
    }
}
```

### 知识点

#### Fragment传参

1准备可序列化的实体类， implements Parcelable，一般写好实体类这些东西都有

2封装参数，在 “发送方”（如 Activity 或另一个 Fragment）中，创建 Bundle 并存入数据：

```
   				// 3. 封装参数
                Bundle bundle = new Bundle();
                bundle.putParcelable("todo_data", todo);
		//设置参数，然后提交事务
		  targetFragment.setArguments(args);
		  ft.add(R.id.fragment_container, targetFragment, fragmentTag);
```

3接收参数

```
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
```

#### 长按弹窗

```
 // 其余代码保持不变（alertDialog、update、VH、TodoDiffCallback、DeleteData接口等）
    private void alertDialog(TodoEntity todoEntity, int position) {
        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle("温馨删除")
                .setMessage("确定要删除这条待办事项吗")
                .setNegativeButton("取消", (dialog, which) -> Toast.makeText(context, "取消成功", Toast.LENGTH_SHORT).show())
                .setPositiveButton("确定", (dialog, which) -> {
                    if (deleteData != null) {
                        deleteData.delete(todoEntity, position);
                    }
                })
                .create();
        alertDialog.show();
    }
```

#### 增量刷新

**增量刷新** 是 RecyclerView 只更新「有变化的条目」，而不是把整个列表全部重新刷新（全量刷新）。

全量刷新：比如调用 `notifyDataSetChanged()`，不管列表里 100 条数据只有 1 条变了，都会重新绘制所有条目，效率低、可能有闪烁。

增量刷新：精准找到「新增 / 删除 / 修改 / 移动」的条目，只刷新这一条 / 几条，性能更好、体验更流畅。



```
public void update(List<TodoEntity> listTodo) {
    // 1. 计算新旧列表的差异
    DiffUtil.DiffResult result = DiffUtil.calculateDiff(new TodoDiffCallback(list, listTodo));
    // 2. 更新Adapter的数据源
    list.clear();
    list.addAll(listTodo);
    // 3. 把差异结果分发给RecyclerView，只刷新变化的条目
    result.dispatchUpdatesTo(this);
}
```

`TodoDiffCallback` 是你自定义的「对比规则类」,必须实现 4 个方法，DiffUtil 靠这 4 个方法判断数据有没有变化

```
private static class TodoDiffCallback extends DiffUtil.Callback {
    private List<TodoEntity> oldList; // 旧数据（刷新前的列表）
    private List<TodoEntity> newList; // 新数据（要刷新的列表）

    public TodoDiffCallback(List<TodoEntity> oldList, List<TodoEntity> newList) {
        this.newList = newList;
        this.oldList = oldList;
    }

    // 方法1：返回旧列表的长度
    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    // 方法2：返回新列表的长度
    @Override
    public int getNewListSize() {
        return newList.size();
    }

    // 方法3：判断「是不是同一个条目」（核心！新手重点理解）
    // 比如：旧列表的第2条和新列表的第2条，是不是同一个待办？
    // 规则：用唯一标识（id）判断，而不是内容
    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        // 这里用id判断，因为每个待办的id是唯一的，哪怕内容改了，id还是同一个
        return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
    }

    // 方法4：判断「同一个条目的内容有没有变」
    // 只有 areItemsTheSame 返回 true 时，才会调用这个方法
    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        TodoEntity oldTodo = oldList.get(oldItemPosition);
        TodoEntity newTodo = newList.get(newItemPosition);
        // 对比所有关键字段：标题、内容、创建时间、同步状态，只要有一个不一样，就返回false（表示内容变了）
        return oldTodo.getTitle().equals(newTodo.getTitle())
                && oldTodo.getContent().equals(newTodo.getContent())
                && oldTodo.getCreateTime().equals(newTodo.getCreateTime())
                && oldTodo.isSync() == newTodo.isSync();
    }
}
```

调用 `DiffUtil.calculateDiff(new TodoDiffCallback(list, listTodo))` 时，DiffUtil 会：

1. 遍历新旧两个列表，用你写的 `areItemsTheSame` 和 `areContentsTheSame` 对比每一条数据；
2. 生成一个 `DiffResult` 对象，里面记录了「哪些条目新增了、哪些删除了、哪些内容改了、哪些位置动了」。

#### 更新数据源 + 分发差异

```
list.clear(); // 清空旧数据
list.addAll(listTodo); // 替换成新数据
result.dispatchUpdatesTo(this); // 把差异发给Adapter，Adapter会自动调用对应的刷新方法（比如notifyItemChanged()）
```

#### 平滚滑动

```
   binding.RV.post(()->{
                            // 进阶：平滑滚动（体验更好）
                            binding.RV.smoothScrollToPosition(0);
                        });
```

##### `post(Runnable)` 的核心原理

- **线程特性**：`post()` 保证传入的 Runnable 一定在 **UI 线程** 执行（即使你在子线程调用 `post()`，内部也会切换到 UI 线程）；

- **执行时机**：不会立即执行，而是加入 View 关联的 `MessageQueue`，等当前的布局测量（measure/layout）、绘制（draw）等任务执行完毕后，再执行这个 Runnable；

- 为什么 RecyclerView 要这么用

  RecyclerView 的滚动依赖自身的布局完成（比如宽高、Item 视图都已渲染），如果在布局完成前调用 

  ```
  smoothScrollToPosition
  ```

  ，滚动目标位置可能还不存在，导致滚动失效。

  ```
  post()
  ```

   能规避这个问题。

### 其他知识点

#### 回调函数的写法

因为那个单一职责的规范，adapter最好不做数据库的操作，所以定义一个接口回调

```
  public interface DeleteData {
        void delete(TodoEntity todoEntity, int position);
    }
```

让调用它的Listfragment传入

```
todoAdapter = new TodoAdapter(getContext(),this::handlerTodoDelete);
```

::是一种语法糖，方法引用运算符，用更短的写法替代 Lambda 表达式。因为此时，Lambda 里只调一个方法，就直接把这个方法传过去就行。函数式接口专属



## 五模块，本地化存储

### sharedpreferences

#### 是什么？

超轻量级数据存储工具，主要用于保存应用的**简单配置信息**（如用户登录状态、字体大小、开关设置等），不适合存储大量 / 复杂数据（如图片、大文本）。

存储格式：XML 文件，默认保存在应用的私有目录（`/data/data/包名/shared_prefs/`），其他应用无法访问；

支持的数据类型：`boolean`、`int`、`float`、`long`、`String`、`Set<String>`；

操作方式：通过 `Editor` 编辑数据，需提交（`commit()`/`apply()`）才会生效。

#### 怎么用？（伪代码）

增和改// // MODE_PRIVATE：私有模式（默认），只有当前应用可访问

```
 SharedPreferences sp = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
   // Step2: 获取编辑器Editor
        SharedPreferences.Editor editor = sp.edit();

        // Step3: 存入键值对（支持多种基础类型）
        editor.putBoolean(KEY_IS_LOGIN, true); // 布尔值：是否登录
        editor.putString(KEY_USER_NAME, "张三"); // 字符串：用户名
        editor.putInt(KEY_AGE, 20); // 整型：年龄

        // 存储Set<String>（注意：不能存null，且元素需为String）
        Set<String> hobbies = new HashSet<>();
        hobbies.add("看书");
        hobbies.add("跑步");
        editor.putStringSet(KEY_HOBBIES, hobbies);

        // Step4: 提交保存（二选一）
        // commit()：同步提交，返回boolean表示是否成功，主线程中不建议大量使用
        // editor.commit();
        // apply()：异步提交，无返回值，效率更高，推荐使用
        editor.apply();
```

删

```
SharedPreferences sp = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(KEY_AGE); // 删除“年龄”键
        editor.apply();
```

查

```
 SharedPreferences sp = getSharedPreferences(SP_NAME,Context.MODE_PRIVATE);
 boolean isLogin = sp.getBoolean(KEY_IS_LOGIN, false); // 默认未登录
 String userName = sp.getString(KEY_USER_NAME, "未知用户"); // 默认未知
```



本项目来记录最后一个添加的记录

```
 SharedPreferences todoSP = getContext().getSharedPreferences("todoSP", MODE_PRIVATE);
   SharedPreferences.Editor edit = todoSP.edit();
   edit.putString("last_todo_content", todoContent);
   edit.apply();
```



### 数据库存储(ROOM数据库)

#### 是什么？

数据库存储主要用于**大量 / 结构化数据**的持久化，**SQLite**：Android 内置的轻量级关系型数据库，**原生 SQLite 操作**：直接通过 `SQLiteOpenHelper` 实现，但需手写 SQL 语句。

原生SQLlite

核心工具类：SQLiteOpenHelper

```
// 步骤1：创建SQLiteOpenHelper子类
public class MyDbHelper extends SQLiteOpenHelper {
    // 数据库名
    private static final String DB_NAME = "user_db";
    // 数据库版本（升级时需递增）
    private static final int DB_VERSION = 1;
    // 用户表建表语句
    private static final String CREATE_USER_TABLE = "CREATE TABLE user (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," + // 主键自增
            "name TEXT NOT NULL," + // 姓名（非空）
            "age INTEGER)"; // 年龄

    public MyDbHelper(Context context) {
        // 参数：上下文、数据库名、游标工厂（null为默认）、版本号
        super(context, DB_NAME, null, DB_VERSION);
    }

    // 首次创建数据库时调用（仅执行一次）
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USER_TABLE); // 执行建表SQL
        Log.d("MyDbHelper", "用户表创建成功");
    }

    // 数据库版本升级时调用（版本号递增触发）
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 示例：删除旧表重新创建（实际开发需考虑数据迁移）
        db.execSQL("DROP TABLE IF EXISTS user");
        onCreate(db);
    }
}
```

//封装的数据库管理类,使用MyDbHelper操作数据库（增删改查）

```
public class DbManager {
private final MyDbHelper dbHelper;

    public DbManager(Context context) {
        dbHelper = new MyDbHelper(context);
    }

    // 1. 插入数据
    public void addUser(String name, int age) {
        // 获取可写数据库（无则创建）
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // ContentValues：封装键值对（键=列名，值=数据）
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("age", age);
        // 插入数据：参数（表名，空值列名，ContentValues）
        long id = db.insert("user", null, values);
        db.close(); // 关闭数据库，避免资源泄漏
        Log.d("DbManager", "插入用户成功，id：" + id);
    }

    // 2. 查询数据（查询所有用户）
    public void queryAllUsers() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // 查询：参数（表名，列数组，where条件，where参数，分组，排序，分页）
        Cursor cursor = db.query(
                "user", // 表名
                null,   // 查询所有列
                null,   // where条件（如 "age > ?"）
                null,   // where参数（如 new String[]{"18"}）
                null,   // 分组
                null,   // 筛选
                "id DESC" // 按id降序排列
        );

        // 遍历游标获取数据
        if (cursor.moveToFirst()) {
            do {
                // 通过列名获取索引，再获取值（避免硬编码索引）
                int id = cursor.getInt(cursor.getColumnIndex("id"));
                String name = cursor.getString(cursor.getColumnIndex("name"));
                int age = cursor.getInt(cursor.getColumnIndex("age"));
                Log.d("DbManager", "用户：id=" + id + ", name=" + name + ", age=" + age);
            } while (cursor.moveToNext());
        }
        cursor.close(); // 关闭游标
        db.close();
    }

    // 3. 更新数据（根据id更新年龄）
    public void updateUserAge(int id, int newAge) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("age", newAge);
        // 更新：参数（表名，ContentValues，where条件，where参数）
        int rows = db.update("user", values, "id=?", new String[]{String.valueOf(id)});
        db.close();
        Log.d("DbManager", "更新" + rows + "行数据");
    }

    // 4. 删除数据（根据id删除）
    public void deleteUser(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // 删除：参数（表名，where条件，where参数）
        int rows = db.delete("user", "id=?", new String[]{String.valueOf(id)});
        db.close();
        Log.d("DbManager", "删除" + rows + "行数据");
    }
    
    //用sql语句来做增删改查
     public void insert(Notepad notepad){
        SQLiteDatabase db = getWritableDatabase();
        String sql = "insert into notepad(content,time) values(?,?)";
        db.execSQL(sql,new Object[]{notepad.getContent(),notepad.getTime()});
        db.close(); // 必须添加
    }
    public List<Notepad> findAll(){
        SQLiteDatabase db = getReadableDatabase();
        String sql = "select * from notepad";
        Cursor cursor = db.rawQuery(sql, null);
        List<Notepad> list = new ArrayList<>();
        //可以把cursor去理解为这一行的指针，它的下个指针是下一行
        while (cursor.moveToNext()){
            int id = cursor.getInt(cursor.getColumnIndex("id"));
            String content = cursor.getString(cursor.getColumnIndex("content"));
            String time = cursor.getString(cursor.getColumnIndex("time"));
            Notepad notepad = new Notepad();
            notepad.setId(id);
            notepad.setContent(content);
            notepad.setTime(time);
            list.add(notepad);
        }
        cursor.close();
        db.close();
        return  list;
    }
    删改。。。
}
```

原生的缺点：麻烦，易出错，无编译期 SQL 校验

#### Room 框架

Room 是 ORM（对象关系映射）框架，将 Java/Kotlin 对象与数据库表映射

核心由 3 部分组成：

**Entity**：实体类，对应数据库表（每字段对应列）；

**Dao**：数据访问接口，定义增删改查方法（无需实现）；

**Database**：数据库类，继承 `RoomDatabase`，管理数据库实例、版本等。

单例管理数据库，保证连接的唯一性

##### Entity

```
@Entity(tableName = "todo_table") // 显式指定表名（默认是类名TodoEntity）
public class TodoEntity  implements Parcelable {
 @PrimaryKey(autoGenerate = true) // 主键 + 自增（id由数据库自动分配）
    private long id; 
    ...
}
```

`@Entity(tableName = "xxx")`：自定义表名;

`@PrimaryKey(autoGenerate = true)`：主键自增

##### Dao

Dao 是 “数据访问对象”，只定义方法，Room 自动生成实现类

```
@Dao // 标记这是Dao接口，Room会自动生成TodoDao_Impl实现类
public interface TodoDao {
    // 1. 单条插入
    @Insert
    void insert(TodoEntity todoEntity);

    // 2. 批量插入（适配批量添加待办的场景）
    @Insert
    void insertAll(List<TodoEntity> todos);

    // 3. 查询所有待办：按id降序（最新添加的排在前面，符合待办使用习惯）
    @Query("select * from todo_table order by id DESC")
    List<TodoEntity> getAllTodos();

    // 4. 按ID查询单条待办（编辑待办时用）
    @Query("select * from todo_table where id = :id")
    TodoEntity queryById(long id);

    // 5. 按同步状态查询（同步到服务器的核心逻辑）
    @Query("select * from todo_table where isSync = :isSync order by id DESC")
    List<TodoEntity> queryBySyncStatus(boolean isSync);

    // 6. 单条删除（删除指定待办）
    @Delete
    void delete(TodoEntity todoEntity);

    // 7. 清空整张表（批量删除/重置待办时用）
    @Query("DELETE FROM todo_table")
    void deleteAll();

    // 8. 更新待办（编辑待办内容/同步状态时用）
    @Update
    void update(TodoEntity todoEntity);
}
```

**关键要点**：

- 基础操作（增 / 删 / 改）：直接用`@Insert`/`@Delete`/`@Update`注解，无需写 SQL；
- 查询操作：必须用`@Query`+SQL 语句，支持参数（用`:参数名`引用，比如`:id`/`:isSync`）；
- SQL 语法：Room 会**编译期校验 SQL**（比如表名 / 列名写错，编译时就报错，而非运行时崩溃）；
- 待办场景优化：`order by id DESC`是待办列表的常用排序方式，符合用户习惯。



##### Database类

```
@Database(entities = {TodoEntity.class},version = 1) // 关联Entity，版本号1
public abstract class TodoDatabase extends RoomDatabase { // 必须继承RoomDatabase，且是抽象类
    // 1. 声明Dao的抽象方法（Room自动实现，返回TodoDao_Impl实例）
    public abstract TodoDao todoDao();

    // 2. 单例模式（双重检查锁+volatile）：新手必掌握的Room最佳实践
    private static volatile TodoDatabase INSTANCE;

    public static TodoDatabase getINSTANCE(Context context) {
        if (INSTANCE==null){ // 第一次检查（无锁，提高效率）
            synchronized (TodoDatabase.class){ // 加锁，保证线程安全
                if (INSTANCE==null){ // 第二次检查（避免多线程重复创建）
                    // 创建数据库实例
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(), // 必须用Application Context（避免内存泄漏）
                            TodoDatabase.class, // 数据库类
                            "todo_database" // 数据库文件名（保存在/data/data/包名/databases/下）
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
```

数据库工具类（部分接口），还有一部分没有使用

```
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
```

**关键要点**：

`@Database`注解：`entities`指定所有关联的 Entity，`version`是数据库版本（升级时要递增）；

必须是抽象类，且定义 Dao 的抽象方法（Room 自动实现）；

单例模式：

- `volatile`：禁止 JVM 指令重排（你注释里的解释非常准确，新手要理解这个坑）；
- `context.getApplicationContext()`：避免 Activity 销毁导致的内存泄漏；
- 双重检查锁：兼顾线程安全和性能。



可优化的点：

1.Dao方法全是同步的，可以改成异步，`Coroutine`（Kotlin）或`RxJava`（Java）

2.数据观察（LiveData）：待办列表更新时自动刷新 UI，建议 Dao 方法返回



#### 问题

**1双重检查锁的用处**

多线程并发

为什么不直接用 “简单单例”？

```
public static TodoDatabase getINSTANCE(Context context) {
    if (INSTANCE == null) {
        INSTANCE = Room.databaseBuilder(...).build();
    }
    return INSTANCE;
}
```

如果线程 A 和线程 B 同时进入 `if (INSTANCE == null)`，会各自创建一个 `TodoDatabase` 实例，导致数据库多连接（耗资源、数据可能不一致）；

为什么不直接加锁？

```
// 低效示范：每次调用都加锁，性能差
public static synchronized TodoDatabase getINSTANCE(Context context) {
    if (INSTANCE == null) {
        INSTANCE = Room.databaseBuilder(...).build();
    }
    return INSTANCE;
}
```

加锁是重量级操作，后续每次调用 `getINSTANCE()` 都要等锁，而实例创建后其实不需要锁，浪费性能；

双重检查锁的核心逻辑

```
public static TodoDatabase getINSTANCE(Context context) {
    // 第一次检查：无锁，实例已创建时直接返回，不影响性能
    if (INSTANCE == null) { 
        // 加锁：只在实例未创建时加锁，保证线程安全
        synchronized (TodoDatabase.class) { 
            // 第二次检查：防止多线程等待锁后重复创建
            if (INSTANCE == null) { 
                INSTANCE = Room.databaseBuilder(...).build();
            }
        }
    }
    return INSTANCE;
}
```



**2volatile的用处**

解决指令重排，`volatile` 是给 `INSTANCE` 变量加的 “保险”，专门解决 JVM 底层的「指令重排」问题

创建 TodoDatabase 实例的 3 个步骤（JVM 层面）

```
INSTANCE = Room.databaseBuilder(...).build();
```

1给 `INSTANCE` 分配内存空间（此时 `INSTANCE` 从 `null` 变成 “非 null”，但还没初始化）；

2执行 `build()` 方法，初始化 `TodoDatabase` 实例（连接数据库、创建表结构等）；

3把初始化好的实例赋值给 `INSTANCE`；

指令重排的坑

为了优化性能，JVM 可能把步骤改成：1 → 3 → 2：

- 线程 A 执行到 “步骤 3” 时，`INSTANCE` 已经不是 `null`，但实例还没初始化（步骤 2 没完成）；
- 此时线程 B 进来，第一次检查 `INSTANCE != null`，直接返回这个 “未初始化的实例”；
- 线程 B 调用 `todoDao()` 时，会因为实例没初始化而崩溃；

**3为啥必须是抽象类**

Room 的工作原理，`TodoDatabase` 是 “模板”，Room 在编译时会自动生成一个**继承自你的抽象类**的实现类。

抽象类不能被直接实例化（`new TodoDatabase()` 会报错），强制你通过 `getINSTANCE()` 单例方法获取实例，符合数据库 “唯一连接” 的设计原则；

你只需要声明 `public abstract TodoDao todoDao();` 这个抽象方法，Room 会在生成的 `TodoDatabase_Impl` 中自动实现这个方法，返回 `TodoDao` 的真实实现类（`TodoDao_Impl`）；

### 内部存储

封装了一个文件工具类来实现内部存储

**内部备份**

```
调用
TodoDao todoDao = TodoDatabase.getINSTANCE(getContext()).todoDao();
List<TodoEntity> allTodos = todoDao.getAllTodos();
boolean isSuccess = FileUtil.backupTodos(getContext(), allTodos);
....             
```

FileUtil里

文件操作！！！

```
 public static boolean backupTodos(Context context, List<TodoEntity> todos) {
        try {
            String jsonStr = gson.toJson(todos);
            LogUtil.d("gson转化java对象成json对象:" + jsonStr);
            //这个直接就是创建空文件+创建数据流，并且openFileOutput锁定了目录
            FileOutputStream fos = context.openFileOutput(BACKUP_FILE_NAME, Context.MODE_PRIVATE);
            //字节流
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            //缓存字节流
            //显示指定UTF-8编码
            byte[] bytes = jsonStr.getBytes(StandardCharsets.UTF_8);
            bos.write(bytes);
           
            bow.flush()
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
    
  ----------------------------------------------------  
   //另一种写法io操作的写法
   // 核心：用OutputStreamWriter把字节流转成字符流，直接指定UTF-8编码
OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
// 套缓冲字符流，提升效率（和BufferedOutputStream同理）
BufferedWriter bw = new BufferedWriter(osw);

// 直接写字符串，无需转byte[]！
bw.write(jsonStr);

bw.flush(); // 强制刷缓冲区（可选，关闭流时会自动刷，但手动刷更保险）
  
    
```

IO,输出（Output）：把程序里的数据（比如你的 Todo 列表）写到文件里，输入（Input）：把文件里的数据读进程序里

 “流” 想象成**连接程序和文件的 “水管”**，数据就是水管里的 “水”

| 流的类型 |     传输单位     |              适用场景              |                         你的代码对应                         |
| :------: | :--------------: | :--------------------------------: | :----------------------------------------------------------: |
|  字节流  | 单个字节（byte） | 所有文件（文本、图片、视频、JSON） |    `FileOutputStream`/`FileInputStream`（处理二进制数据）    |
|  字符流  | 单个字符（char） |    仅文本文件（JSON、TXT、XML）    | `InputStreamReader`/`OutputStreamWriter`（处理文本，自动处理编码） |

**缓冲流（BufferedXXX）作用?**

提升 IO 效率,,,,

没有缓冲流：程序写 1 个字节，就和文件交互 1 次（频繁读写，速度慢）；

有缓冲流：先把数据存到 “内存缓冲区”（比如 8KB），攒够了再一次性写入文件（减少交互次数，速度提升 10 倍 +）；

#### 安卓的文件存储分类



内部沙箱存储，/data/data/你的包名/files/todo_backup.json

```
//内部文件目录
FileOutputStream fos = context.openFileOutput(BACKUP_FILE_NAME,Context.MODE_PRIVATE);
```

外部公共存储/storage/emulated/0/Download/todo_backup_external.json

```
 // 2. 获取Download目录路径
File downloadDir =Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
```

外部私有存储，/Android/data/你的包名/files/Download/xxx.json

```
//外部
context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
```



| 流类型 | 输入基类（读） | 输出基类（写） |                       你的代码里的子类                       |
| :----: | :------------: | :------------: | :----------------------------------------------------------: |
| 字节流 |  InputStream   |  OutputStream  | FileInputStream/FileOutputStream、BufferedInputStream/BufferedOutputStream |
| 字符流 |     Reader     |     Writer     | InputStreamReader/OutputStreamWriter、BufferedReader/BufferedWriter |

代码里反复用`StandardCharsets.UTF_8`，这是解决乱码的关键：

文本转字节时，编码（UTF-8）和解码（比如 GBK）不一致（比如 “你” 用 UTF-8 是 3 个字节，用 GBK 是 2 个字节，解错就成了 “�”）

安卓 / 开发通用规则：**所有文本 IO 都指定 UTF-8 编码**，不要用系统默认编码（不同手机默认编码可能不同）。



|             场景             |            推荐写法            |           核心理由           |
| :--------------------------: | :----------------------------: | :--------------------------: |
|      写 JSON/TXT 等文本      |    BufferedWriter（字符流）    |   代码简洁、编码处理更直观   |
| 写图片 / 视频 / 音频等二进制 | BufferedOutputStream（字节流） | 避免字符编码转换导致数据损坏 |

读写 JSON/TXT 等文本，FileIOStream → StreamReader/Writer → Buffer

读写图片 / 视频等二进制  FileIOStream → Buffer（跳过字符流）

二进制文件千万不要套字符流！！！数据损坏

**为啥要缓存流都要flush()？**

`flush()` 的作用是**不管缓冲区有没有满，立刻把内存缓冲区里的所有数据写入文件**（而不是等缓冲区满 / 流关闭才写）。

**数据量小，没填满缓冲区**：比如你的 Todo JSON 只有 100 字节，远小于 8KB 缓冲区，不调用 flush 的话，数据会一直留在内存里，直到你调用`bw.close()`（流关闭时会自动触发 flush）才写入文件；

**防止数据丢失**：如果程序在`write`之后、`close`之前崩溃（比如闪退），缓冲区里的数据会直接丢了，调用 flush 能确保数据及时写入文件；

### 外部存储

逻辑差不多，只是需要权限。就不过多陈述

```
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

```



## 六模块，网络请求

就是发送网络请求，接受响应

**序列化和反序列化**

|           概念           |                         本质 / 定义                          |                           核心作用                           |
| :----------------------: | :----------------------------------------------------------: | :----------------------------------------------------------: |
|          序列化          | 将 Java 内存中的**对象**（如 User 实例）转换为**可传输 / 存储的格式**（如 JSON 字符串）的过程 | 解决内存对象无法直接存储（文件 / 数据库）、传输（网络）的问题 |
|         反序列化         | 将序列化后的格式（如 JSON 字符串）还原为 Java 内存中可操作的**对象**（如 User 实例）的过程 | 把存储 / 传输过来的格式化数据，恢复成 Java 能直接调用方法、处理逻辑的对象 |
| JSON 对象（JSON字符串）  | 遵循 JSON 语法的**文本字符串**（键值对格式），跨语言通用，无任何可执行能力 |        作为跨系统 / 跨语言数据传输、存储的 “通用格式”        |
| JsonObject（JSONObject） | Java 第三方库（FastJSON/Gson 等）提供的**工具类**，本质是封装 JSON 字符串的 Java 对象 | 让 Java 能像操作 Map 一样**便捷操作 JSON 字符串**（增删改查键值对），是 JSON 字符串和 Java 对象之间的 “操作桥梁” |

.json格式的文件，JSON 是一种**有严格语法规则的文本数据格式**

你传入的时候需要传入`"{\"name\":\"张三\",\"age\":20}"` 是 “符合 JSON 语法的字符串”，Java 中双引号 `"` 需要用 `\"` 转义）。。其实就是传入{\"name\":\"张三\",\"age\":20}这样的字符串，，gson完全可以做到。。。



JSON（JavaScript Object Notation）是一种**轻量级的数据交换格式**，它基于字符串，但有严格的语法规则，目的是让数据结构化、可解析。

**原生的手动解析**

```
// Android 解析JSON对象
JSONObject jsonObject = new JSONObject(jsonStr);
String name = jsonObject.getString("name"); 
int age = jsonObject.getInt("age");

// Android 解析JSON数组
JSONArray jsonArray = new JSONArray(jsonArrStr);
for (int i=0; i<jsonArray.length(); i++){
    JSONObject item = jsonArray.getJSONObject(i);
}
```

**GSON自动映射**

```
// 1. 定义实体类（字段名和JSON的key一致）
public class Person {
    private String name;
    private int age;
    // getter/setter（必须有，GSON需要）
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

// 2. GSON自动解析（一行代码）
Gson gson = new Gson();
Person person = gson.fromJson(jsonStr, Person.class); 

// 解析数组（需要TypeToken）
Type personType = new TypeToken<List<Person>>(){}.getType();
List<Person> persons = gson.fromJson(jsonArrStr, personType);
```





### okHttp

1.OkHttpClient,,,,,OkHttp 的 “核心引擎”：管理连接、超时、拦截器（比如你的日志拦截器）

```
OkHttpManager.getInstance () 构建的单例，配置了 15 秒超时 + 日志拦截
```

2.Request,单个请求的 “说明书”：指定 URL、请求方式（GET/POST）、请求体

```
NetWorkUtil 里的new Request.Builder().url().post(requestBody).build()
```

3RequestBody,POST 请求的 “参数包”：装要发给服务器的 JSON 数据

```
RequestBody.create(jsonStr, JSON_TYPE)
```

4Call,单个请求的 “任务对象”：发起异步 / 同步请求

```
OkHttpManager.getInstance().newCall(request)
```

5Callback,请求结果的 “回调”：处理成功 / 失败，运行在子线程

```
enqueue (new Callback () { onFailure/onResponse })
```

OkHttpManager.getInstance()

要复用OkHttpClient，要OkHttpClient单例,为了复用连接池。

连接池是复用 TCP 连接，，三次握手后，发请求，不断开，，把连接放进「连接池」。

同时解耦，同一配置日志拦截器和超时时间

```
package com.example.test.utils;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * 这种单例模式，就是防止多并发导致的问题
 */
public class OkHttpManager {
    private static volatile OkHttpClient okHttpClient;
    //超时时间
    private static final int TIMEOUT = 15;

    // 私有化构造方法，禁止外部new
    private OkHttpManager() {
    }

    /**
     * 本质就是不用直接new OkHttpClient这样，因为要配置日志拦截器和超时时间
     * @return
     */
    public static OkHttpClient getInstance() {
        if (okHttpClient == null) {
            synchronized (OkHttpClient.class) {
                if (okHttpClient == null) {
                    //配置日志拦截器
                    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                    //配置日志级别，BODY = 打印请求+响应的完整内容（包括JSON）
                    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                    //构建OKHttpClient
                    okHttpClient = new OkHttpClient.Builder()
                            .addInterceptor(loggingInterceptor)
                            .connectTimeout(TIMEOUT, TimeUnit.SECONDS) // 连接超时
                            .readTimeout(TIMEOUT, TimeUnit.SECONDS)   // 读取超时
                            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)  // 写入超时
                            .build();
                }
            }

        }
        return okHttpClient;
    }

}

```



以`postTodo`为例，需要手动做 5 件事

```
// 1. 手动把Java对象转JSON字符串
String jsonStr = gson.toJson(postBean);
// 2. 手动构建请求体
RequestBody requestBody = RequestBody.create(jsonStr, JSON_TYPE);
// 3. 手动构建Request（指定URL、请求方式、请求体）
Request request = new Request.Builder().url(TODO_POST_URL).post(requestBody).build();
// 4. 手动发起异步请求，在Callback里处理结果（子线程）
OkHttpManager.getInstance().newCall(request).enqueue(new Callback() {
    @Override
    public void onResponse(Call call, Response response) throws IOException {
        // 5. 手动解析JSON字符串为Java对象
        String responseJson = response.body().string();

        TodoResponseBean bean = gson.fromJson(responseJson, TodoResponseBean.class);
        // 6. 手动切回主线程更新UI
        mainHandler.post(() -> callback.onSuccess(bean));
    }
});
```

注意！！

`response.body()`的核心是**字节流**（二进制数据），数据以字节流的形式(二进制)存储在内存缓冲区或临时文件

`string()`的作用是 “一次性读取所有字节→按 UTF-8 转字符串→关闭流”，所以只能调用一次；





OkHttp的异步回调运行在子线程

OkHttpManager.getInstance().newCall(request).enqueue()，OkHttp 会从自己的线程池里拿出一个**子线程**来执行网络请求，并在子线程中回调 `onResponse`/`onFailure` 方法。

此时需要切回主线程

```
  mainHandler.post(() -> callback.onSuccess(bean));
```

**检查网络是否连接的方法**

```
//检查网络状态
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;
        //拿网络管理员对象
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        // Android 10 及以上使用新 API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            if (cm == null) return false;
            //当前活跃的网络对象
            Network activeNetwork = cm.getActiveNetwork();
            //网络的能力对象
            NetworkCapabilities networkCapabilities = cm.getNetworkCapabilities(activeNetwork);
            if (networkCapabilities == null) return false;
            //检查是否有网络连接
            return ( networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    ||  networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                    &&  networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else {
            try {
                android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                //isConnected()过时，被弃用
                return activeNetwork != null && activeNetwork.isConnected();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }


    }
```

### Retrofit

定义接口,用注解描述接口。。调用接口可以生成一个Call任务

可以在这里写不同的任务，符合Resultful风格

```
public interface TodoApiService {
    // @POST("post")：指定POST请求，路径拼在BaseUrl后
    // @Body：请求体，自动把TodoPostBean转JSON
    // 返回Call<TodoResponseBean>：响应自动转TodoResponseBean
    @POST("post")
    Call<TodoResponseBean> postTodo(@Body TodoPostBean postBean);
}
```

构建 Retrofit 单例

```
public class RetrofitManager {
    private static final String BASE_URL = "https://httpbin.org/"; // 必须以/结尾
    private static volatile Retrofit retrofit;

    private static Retrofit getInstance() {
        if (retrofit == null) {
            synchronized (Retrofit.class) {
                if (retrofit == null) {
                    retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL) // 基础URL
                            .client(OkHttpManager.getInstance()) // 复用OkHttp的配置（日志、超时）
                            .addConverterFactory(GsonConverterFactory.create()) // 自动JSON解析
               
                            .build();
                }
            }
        }
        return retrofit;
    }

    // 获取API实例（给外部调用）
    public static TodoApiService getTodoApi() {
        return getInstance().create(TodoApiService.class);
    }
}
```

注意！！！

解析响应，，，，  .addConverterFactory(GsonConverterFactory.create()) // 自动JSON解析，这个是导入的新gson包叫retrofitConverterGson，去解析自动响应。





调用 Retrofit 接口

```
// Retrofit写法（替换你NetWorkUtil里的postTodo）
public static void postTodoWithRetrofit(Context context, TodoPostBean postBean, TodoCallback callback) {
    // 1. 检查网络（复用你原有的isNetworkAvailable）
    if (!isNetworkAvailable(context)) {
        callback.onFail("无网络连接");
        return;
    }

    // 2. 获取API实例
    TodoApiService apiService = RetrofitManager.getTodoApi();
    // 3. 发起异步请求（Retrofit的Call）
    Call<TodoResponseBean> call = apiService.postTodo(postBean);
    call.enqueue(new retrofit2.Callback<TodoResponseBean>() {
        @Override
        public void onResponse(Call<TodoResponseBean> call, retrofit2.Response<TodoResponseBean> response) {
            if (response.isSuccessful()) {
                // 自动解析成TodoResponseBean，不用手动gson.fromJson
                TodoResponseBean bean = response.body();
                // 切回主线程（和你OkHttp里的mainHandler一样）
                mainHandler.post(() -> callback.onSuccess(bean));
            } else {
                String errorMsg = "响应失败：" + response.code();
                mainHandler.post(() -> callback.onFail(errorMsg));
            }
        }

        @Override
        public void onFailure(Call<TodoResponseBean> call, Throwable t) {
            String errorMsg = "POST请求失败：" + t.getMessage();
            mainHandler.post(() -> callback.onFail(errorMsg));
        }
    });
}
```



## 七模块，权限

### 前情提要

android9  API28

android10  API29    开始分区存储，

这个是外部存储的分区。

分为App 专属外部目录，存 App 的缓存、下载文件（卸载会删）

公共媒体分区，存相册、音乐等用户需要保留的文件

其他公共分区



android11  API30

强制分区存储，不能再用老规则

android12  API31

android12L API32

android13  API 33

媒体权限拆分，把存储权限拆成图片 / 视频 / 音频，访问相册不用动态申请了。

android14  API 34

android15  API 35

android16  API 36

#### 普通权限

不涉及隐私

只需在 `AndroidManifest.xml` 中声明

安装时自动授予，**不用代码申请**

#### 危险权限

涉及用户隐私

必须**代码动态申请**

用户可拒绝、可在设置关闭

常见：1存储，2相机，3位置，4电话，5麦克风



此项目用到存储权限和网络权限，通知权限。

网络权限用到的是普通权限。

存储权限MANAGE_EXTERNAL_STORAGE，是特殊权限。

WRITE/READ_EXTERNAL_STORAGE是危险权限。



### 存储权限和版本规则

**之前版本（Android≤5.1）**

只需清单声明`WRITE/READ_EXTERNAL_STORAGE`。

**Android 6.0（API23）--Android 9.0（API28）**

Android 6.0动态权限的起点，6.0-9 版本需要同时申请`READ + WRITE`。



 **Android 10（API29），分区存储默认开启**

requestLegacyExternalStorage=true.临时关闭分区存储。让 10 版本仍能像 9 及以下一样直接写公共 Download 目录（这是过渡方案）。



**Android 11（API30）：强制分区存储.`requestLegacyExternalStorage`失效；**`WRITE/READ_EXTERNAL_STORAGE`仅能访问「媒体文件」，不能访问 Download 目录的 JSON、文档等非媒体文件；

访问所有文件需申请`MANAGE_EXTERNAL_STORAGE`（特殊权限，**不能用`requestPermissions`申请**，必须跳系统设置）。这个权限谷歌审核严格，只有文件管理器、备份类核心应用能通过，普通应用上线会被拒（实习项目无所谓，正式上线要换方案）。



**Android13(API33)媒体权限拆分**

废弃`READ_EXTERNAL_STORAGE`，拆分为 3 个**普通权限**（无需动态申请，清单声明即可）：

- `READ_MEDIA_IMAGES`（读图片）
- `READ_MEDIA_VIDEO`（读视频）
- `READ_MEDIA_AUDIO`（读音频）

对 Download 目录的非媒体文件（如你的 JSON 备份文件）无影响，仍需`MANAGE_EXTERNAL_STORAGE`。



**外部私有应用目录和内部私有应用目录（外部沙箱和内部沙箱）**

分区存储下最推荐的无权限方案

申请权限的方式



 **存储访问框架（SAF）**没用到，先标记下//todo

```
如果不想申请MANAGE_EXTERNAL_STORAGE，可以让用户手动选择文件 / 保存位置，系统授权，无需任何权限：
场景：替代「直接写公共 Download 目录」，合规且不用权限；
```



**MediaStore：读写公共媒体文件的无权限方案**,Android10 + 可以用`MediaStore`读写

-----------------------------------------------------------------------

然后是本项目。

版本适配有点混乱就先不多说了，因为调试的手机是34，所以用的是MANAGE_EXTERNAL_STORAGE。

低版本的危险权限也有申请 ，比如

```
Manifest.permission.READ_EXTERNAL_STORAGE,
Manifest.permission.WRITE_EXTERNAL_STORAGE
```

#### 如何申请？

**老版本方法**，requestPermissions（已经弃用）

```
 ActivityCompat.requestPermissions(
                    this,                  // 上下文
                    new String[]{Manifest.permission.CAMERA}, // 要申请的权限数组
                    REQUEST_CODE_CAMERA    // 请求码（用来区分不同权限）
            );
            
 @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    回调方法
    }
```

权限结果要写在`onRequestPermissionsResult`方法里（和申请代码分离，容易乱）



我用的是，ActivityResultLauncher启动器。

申请逻辑和回调逻辑写在一起。既能申请危险权限，也能处理跳系统设置（比如你申请`MANAGE_EXTERNAL_STORAGE`）、跳其他 Activity 等场景，一套 API 通用，不用请求码，每个启动器对应一个权限场景，无冲突。

```
1.先声明一个启动器
  private ActivityResultLauncher<String> cameraPermissionLauncher;
  
2注册启动器
 cameraPermissionLauncher = registerForActivityResult(
 			//new ActivityResultContracts.RequestMultiplePermissions(), // 多权限合约
 			//  new ActivityResultContracts.StartActivityForResult(), // 跳Activity合约
                new ActivityResultContracts.RequestPermission(), // 单个权限申请合约
                isGranted -> { // 回调逻辑（申请结果直接在这里处理）
                    if (isGranted) {
                        Toast.makeText(this, "相机权限已授予", Toast.LENGTH_SHORT).show();
                        openCamera();
                    } else {
                        Toast.makeText(this, "相机权限被拒绝", Toast.LENGTH_SHORT).show();
                    }
                }
        );


 // 3. 检查并申请权限
    private void checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // 没有权限 → 调用launch发起申请（不用请求码）
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            Toast.makeText(this, "已有相机权限", Toast.LENGTH_SHORT).show();
            openCamera();
        }
    }
//
```

#### 通知权限

```
<!--Android 8.0（API26）+  前台服务权限 ，没有这个权限，调用startForegroundService会直接崩溃-->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
//Android 12（API31）的SyncService的foregroundServiceType="dataSync"和这个权限对应，声明后系统知道服务用途是数据同步，更合规，避免被系统误判为 “无用服务” 杀死
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
 <!-- Android 13+ 通知权限（必要，需动态申请） -->
 <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>

```

#### 网络权限

```
 都是普通权限
 <!-- 网络核心权限（必须） -->
    <uses-permission android:name="android.permission.INTERNET"/>
    <!-- 检查网络状态 -->
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
```



## 八模块，service和通知

#### 模块描述

实现**前台 Service（SyncService）** 用于后台定时同步本地未上传的 Todo 数据到云端，结合通知栏展示同步状态（避免 Service 被系统杀死）；通过`NotificationUtil`封装通知渠道创建、通知构建逻辑，适配不同 Android 版本的前台服务规则，保证服务稳定性与用户感知。

service分为启动式service和绑定式service



NotificationUtil

```
/**
 * 通知工具类：创建前台服务的通知渠道+通知（找工作项目必备）
 */
public class NotificationUtil {
    // 通知渠道ID（自定义，唯一即可）
    public static final String CHANNEL_ID = "sync_service_channel";
    // 前台服务通知ID（自定义，>0即可）
    public static final int SERVICE_NOTIFICATION_ID = 1001;

    /**
     * 创建通知渠道（Android 8.0+必须）
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //安卓8以上
            NotificationChannel notificationChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "随笔同步服务",
                    NotificationManager.IMPORTANCE_LOW// 低优先级，不弹窗，只在通知栏显示
            );
            //注册渠道
            NotificationManager manager =  context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(notificationChannel);
        }
    }

    /**
     * 获取前台服务的通知对象
     */
    public static Notification getSyncServiceNotification(Context context) {
        createNotificationChannel(context);
        //构建通知
        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("随笔同步服务")
                .setContentText("正在后台自动同步你的随笔，每一分钟一次...")
                .setSmallIcon(R.drawable.start_label_icon) // 替换为你的APP图标（必须是应用内资源）
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}

```

**关键解析**：

- `IMPORTANCE_LOW`：适配后台服务场景，通知仅展示在通知栏，无弹窗 / 响铃；
- `setSmallIcon`是通知必填项，缺失会导致通知无法显示。





```
/**
 * 随笔同步前台服务（找工作项目核心功能）
 * 核心：带通知栏，保活率高，用于自动同步随笔到云端
 */
public class SyncService extends Service {
    private static final String TAG = "SyncService";
    // 关键：添加标记，防止重复启动定时任务
    private boolean isSyncTaskRunning = false; // 初始为false
    private Handler syncHandler;
    //接口实例，
    private TodoApiService todoApiService;
    // 同步间隔：10分钟（测试时可改成30秒，方便验证）
    private static final long SYNC_INTERVAL = 60 * 1000; // 10分钟=600000毫秒
    private int successCount = 0;
    private int failCount = 0;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "服务创建：onCreate");
        // 关键：启动前台服务（绑定通知栏，避免被系统杀死）
        // 适配Android 12+启动前台服务
        Notification notification = NotificationUtil.getSyncServiceNotification(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startForeground(NotificationUtil.SERVICE_NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NotificationUtil.SERVICE_NOTIFICATION_ID, notification);
        }
        // 2. 初始化Handler和Retrofit
        syncHandler = new Handler(Looper.getMainLooper());
        todoApiService = RetrofitManager.getTodoApi();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "服务启动：onStartCommand");
        // 面试考点：返回START_STICKY → 服务被系统杀死后，系统会尝试重启（保活关键）
        //启动定时任务
        if (!isSyncTaskRunning) {
            startSyncTask();
            isSyncTaskRunning = true;
        }
        return START_STICKY;
    }

    /**
     * 核心：定时同步任务（每隔SYNC_INTERVAL执行一次）
     */
    private void startSyncTask() {
        syncHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 执行同步逻辑
                syncUnUploadedTodos();
                // 循环执行：执行完一次后，延迟SYNC_INTERVAL再执行
                syncHandler.postDelayed(this, SYNC_INTERVAL);
            }
        }, SYNC_INTERVAL);
    }

    /**
     * 核心逻辑：同步未上传的随笔到云端
     */
    private void syncUnUploadedTodos() {
        Log.d(TAG, "开始同步未上传的随笔...");
        // 1. 查询本地未同步的随笔（子线程操作数据库，避免ANR）
        new Thread(() -> {
            List<TodoEntity> unSyncList = DbUtil.getUnSyncTodos(SyncService.this);
            if (unSyncList.isEmpty()) {
                Log.d(TAG, "暂无未同步的随笔");
                return;
            }
            Log.d(TAG, "即将调用Retrofit请求");
            //计数器，因为会有一些未同步的数据，总不能每一个都弹窗。此时就需要批量同步发送一次广播
            CountDownLatch latch = new CountDownLatch(unSyncList.size());
            successCount=0;
            failCount= 0;
            List<String> failTitles = new ArrayList<>();
            for (TodoEntity todo : unSyncList) {
                // 把TodoEntity转成PostBean（适配Retrofit的请求体）
                TodoPostBean postBean = new TodoPostBean(
                        todo.getTitle(),
                        todo.getContent(),
                        todo.getCreateTime()
                );

                //调用Retrofit的post请求
                todoApiService.postTodo(postBean).enqueue(new Callback<TodoResponseBean>() {
                    @Override
                    public void onResponse(Call<TodoResponseBean> call, Response<TodoResponseBean> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            //同步成功，更新数据库同步状态
                            new Thread(() -> {
                                Log.d(TAG, "进入子线程");
                                try {
                                    DbUtil.updateTodoSyncStatus(SyncService.this, todo.getId(), true);

                                    Log.d(TAG, "随笔同步成功：" + todo.getTitle());
                                    successCount++;
                                } catch (Exception e) {
                                    Log.e(TAG, "错误" + e.getMessage());
                                    failCount++;
                                }
                                finally {
                                    // 不管成功/失败，计数器-1
                                    latch.countDown();
                                }
                            }).start();
                        }else {
                            failCount++;
                            latch.countDown();
                        }
                    }

                    @Override
                    public void onFailure(Call<TodoResponseBean> call, Throwable t) {
                        Log.e(TAG, "随笔同步失败：" + todo.getTitle() + "，原因：" + t.getMessage());
                        failCount++;
                        latch.countDown();
                    }
                });
            }
            // 2. 等待所有请求完成（计数器归0）
            try {
                latch.await(); // 阻塞，直到所有请求完成
            } catch (InterruptedException e) {
                Log.e(TAG, "等we待同步完成被中断：" + e.getMessage());
                Thread.currentThread().interrupt();
            }

            // 批量同步，发送一次广播
        new Handler(Looper.getMainLooper()).postDelayed(()->{
            String msg = "同步完成：成功" + successCount + "条，失败" + failCount + "条";
            sendSyncResultBroadcast(msg);
            updateNotification(msg);
        },1000);
        }).start();
    }
	//更新通知
    private void updateNotification(String content) {
        Notification build = new NotificationCompat.Builder(this, NotificationUtil.CHANNEL_ID)
                .setContentTitle("随笔同步服务")
                .setContentText(content)
                .setSmallIcon(R.drawable.start_label_icon)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        //更新通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NotificationUtil.SERVICE_NOTIFICATION_ID, build,ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        }else {
            startForeground(NotificationUtil.SERVICE_NOTIFICATION_ID, build);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "服务销毁：onDestroy");

        // 停止定时任务（避免内存泄漏）
        syncHandler.removeCallbacksAndMessages(null);
        // 停止前台服务，移除通知
        stopForeground(true);
    }

    //这个是绑定式的service要用的
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    // 同步成功时发广播
    private void sendSyncResultBroadcast(String msg) {
        Intent intent = new Intent(LocalSyncReceiver.ACTION_SYNC_RESULT);
        intent.putExtra(LocalSyncReceiver.KEY_SYNC_MSG, msg);
        //发送本地广播
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}

```

**关键解析**：

1. 前台服务启动

   - Android 12+（API31）必须指定`foregroundServiceType`（如`DATA_SYNC`），否则崩溃；
   - `startForeground()`必须传入通知（ID>0），否则服务会被判定为后台服务并杀死。

   ```
     private void startSyncService() {
           Intent intent = new Intent(this, SyncService.class);
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
               startForegroundService(intent);
           } else {
               startService(intent);
           }
       }
   ```

   

2. 定时任务

   - `Handler.postDelayed()`实现循环定时，优于`Timer`（线程安全）；
   - `isSyncTaskRunning`标记防止多次`startService()`导致定时任务叠加。

3. CountDownLatch

   - 作用：等待所有异步网络请求完成后统一处理结果（更新通知、发广播）；
   - `latch.await()`阻塞子线程，`latch.countDown()`每个请求完成后调用，计数器 - 1。

4. 保活与资源释放

   `onStartCommand`返回`START_STICKY`：服务被杀死后系统尝试重启；

   - `onDestroy`中清空`Handler`任务、停止前台服务，避免内存泄漏

service的开启





#### 核心知识点

##### 1. 前台 Service vs 普通 Service

|       特性        |          普通 Service          |             前台 Service             |
| :---------------: | :----------------------------: | :----------------------------------: |
|    系统优先级     |         低（易被杀死）         |    高（几乎不被杀死，绑定通知栏）    |
|     通知要求      |               无               | 必须绑定通知（Android 8.0 + 需渠道） |
| Android 12 + 适配 | 无需指定 foregroundServiceType |       必须指定（如 DATA_SYNC）       |
|     适用场景      |         短时间后台任务         |   长时间后台任务（数据同步、定位）   |



## 九模块，broadcastReceiver广播组件

#### 模块描述

实现**本地广播接收器（LocalSyncReceiver）** ，接收 SyncService 发送的同步结果广播，通过回调接口通知 UI 层（Activity/Fragment）刷新 Todo 列表，实现 Service 与 UI 的解耦通信。

##### 1. 本地广播接收器（LocalSyncReceiver）

作用：接收应用内广播，通过回调接口通知 UI 层同步完成，仅在应用内传播（安全、高效）。

```
package com.example.test.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * 本地广播接收器：接收Service的同步结果
 * SyncService 同步完成后，用本地广播通知 MainActivity 更新 UI（比如显示同步成功提示）
 */
public class LocalSyncReceiver extends BroadcastReceiver {
    // 广播Action（自定义，唯一即可）
    public static final String ACTION_SYNC_RESULT = "com.example.test.action.SYNC_RESULT";
    // 同步结果的Key
    public static final String KEY_SYNC_MSG = "sync_msg";
    private OnSyncResultListener listener;
    // 定义回调接口，用于通知页面刷新列表
    public interface OnSyncResultListener {
        void onSyncCompleted(); // 同步完成，需要刷新列表
    }
    public LocalSyncReceiver(OnSyncResultListener listener) {
        this.listener = listener;
    }
    // 提供无参构造（给MainActivity用）和有参构造（给Fragment用）
    public LocalSyncReceiver() {
        this.listener = null;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String msg = intent.getStringExtra(KEY_SYNC_MSG);
        // 通知页面刷新列表
        if (listener != null) {
            listener.onSyncCompleted();
        }
    }
}

```



##### 网络广播接收器

```
package com.example.test.broadcast;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.example.test.service.SyncService;
import com.example.test.utils.NetWorkUtil;

/**
 * 网络状态广播接收器（系统广播）
 * 监听网络变化，断网提示，联网触发同步
 */
public class NetworkReceiver extends BroadcastReceiver {
    //记录上次的网络记录
    private boolean lastNetworkState = true;
    public static final String TAG = "Broad_cast";
    /**
     *
     * @param context 系统调用这个接收器时给的context
     * @param intent The Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        //. 获取当前网络状态
        boolean  currentConnected  = NetWorkUtil.isNetworkAvailable(context);
        Log.d(TAG,"进入onReceive");
        Log.d(TAG,"lastNetworkState:"+String.valueOf(lastNetworkState));
        Log.d(TAG,"currentConnected:"+String.valueOf(currentConnected));
        if (currentConnected && !lastNetworkState){
            Log.d(TAG,"进入网络已恢复");
            Toast.makeText(context, "网络已恢复，开始自动同步随笔", Toast.LENGTH_SHORT).show();
            // 联网后启动同步服务（触发一次同步）
            startService(context);
        }
        if (!currentConnected && lastNetworkState){
            Log.d(TAG,"进入网络已断开");
            Toast.makeText(context, "网络已断开，暂停随笔同步", Toast.LENGTH_SHORT).show();
            stopService(context);
        }
        lastNetworkState = currentConnected;
    }
    /**
     * 关闭同步服务（直接调用，系统静默处理已停止的情况）
     */
    private void stopService(Context context) {
        Intent serviceIntent = new Intent(context, SyncService.class);
        context.stopService(serviceIntent);
        Log.d(TAG, "暂停同步服务");
    }
    private  void startService(Context context) {
        Intent serviceIntent = new Intent(context, SyncService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
```





**关键解析**：

- 本地广播：通过`LocalBroadcastManager`发送 / 接收，仅应用内传播，避免外部拦截；
- Action 匹配：`onReceive`中先验证 Action，防止接收无关广播；
- 回调接口：解耦接收器与 UI 层，让 UI 自定义同步完成后的逻辑。

使用

注册广播时，绑定receiver和过滤器intentFile，里面传入action,public static final String ACTION_SYNC_RESULT = "com.example.test.action.SYNC_RESULT";

```
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
```



## 十模块，多线程

### 并发和串行

**并发**：核心是「多个任务在同一时间段内推进」（宏观看起来 “同时做”），≠ 真并行（多核 CPU 才是真同时执行）。

**串行**：核心是「多个任务排队，做完一个再做下一个」，同一时间只有 1 个任务在执行。

- 安卓 / 鸿蒙的核心并发形态：UI 主线程 + 子线程 宏观并发执行；
- 串行的核心范围：**单个线程内部（无论 UI 主线程、子线程）代码永远串行执行**（代码一行行跑，执行栈串行）。

#### UI 线程串行的底层实现

- 安卓：Looper + MessageQueue（MQ）—— 保证 UI 线程内所有任务（UI 交互、定时器、UI 更新）串行执行；
- 鸿蒙：EventRunner + EventQueue —— 逻辑和安卓完全一致，仅命名不同。

#### Looper+MessageQueue 运作逻辑（鸿蒙 EventRunner+EventQueue 同理）

1. 绑定规则：Looper 和 MQ 是「线程专属」—— 一个线程只能有 1 个 Looper + 1 个 MQ，Looper 只处理当前线程的 MQ 任务；

2. 代码执行分层：

   - 同步代码（读文件、写文件、简单计算）：直接进入线程执行栈，串行执行，不进 MQ；
   - 异步任务（定时器、跨线程 UI 更新）：被 Handler 封装成 Message 投递到 MQ 排队，到时间后由 Looper 取出，最终进入线程执行栈串行执行；

   

3. 核心风险：UI 线程执行栈被耗时操作（网络、大文件解析、复杂计算）占用 → Looper 无法快速处理 UI 事件 → 界面卡顿 / ANR（应用无响应）。

#### 核心避坑

❌ 绝对禁止：UI 线程执行任何耗时操作（同步 / 异步都不行）—— 最终都会占用 UI 线程执行栈，导致 UI 卡住；

✅ 正确做法：所有耗时操作（网络、文件读写、大计算）放子线程，主线程只做「轻量 UI 更新」（毫秒级完成）。



### 子线程和主线程通信的方法

**Handler**

创建时绑定主线程的Looper



```
1直接重写方法，来处理逻辑
// 1. 在Activity/Fragment中创建Handler（自动绑定主线程Looper）
private Handler mMainHandler = new Handler(Looper.getMainLooper()) {
    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        // 此方法运行在主线程，可更新UI
        switch (msg.what) {
            case 1:
                String content = (String) msg.obj;
                tvContent.setText(content); // 更新TextView
                break;
        }
    }
};


// Activity的runOnUiThread源码
public final void runOnUiThread(Runnable action) {
    if (Thread.currentThread() != mUiThread) {
        mHandler.post(action); // 不在主线程 → 用Handler投递
    } else {
        action.run(); // 已在主线程 → 直接执行
    }
}
子线程调用，
// 2. 子线程中发送消息到主线程
new Thread(() -> {
    // 模拟耗时操作（如网络请求、数据库查询）
    String result = "子线程获取的数据";
    
    // 方式A：发送Message
    Message msg = Message.obtain(); // 复用Message，避免创建新对象
    msg.what = 1; // 消息标识（区分不同消息）
    msg.obj = result; // 携带数据
    mMainHandler.sendMessage(msg);

    // 方式B：发送Runnable（更简洁）
    mMainHandler.post(() -> {
        tvContent.setText(result); // 直接在Runnable中更新UI
    });
}).start();
```



避免内存泄漏：Activity 销毁时需移除 Handler 的未处理消息（否则 Handler 持有 Activity 引用）：

 mMainHandler.removeCallbacksAndMessages(null); // 清空所有消息



其他拓展

LiveData

```
// 1. 定义LiveData
private MutableLiveData<String> mData = new MutableLiveData<>();

// 2. 观察数据（自动在主线程回调）
mData.observe(this, data -> {
    tvContent.setText(data); // 主线程更新UI
});

// 3. 子线程更新数据
new Thread(() -> {
    mData.postValue("子线程数据"); // postValue：子线程更新；setValue：主线程更新
}).start();
```

