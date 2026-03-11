package com.example.test.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.test.MainActivity;
import com.example.test.R;
import com.example.test.database.TodoDatabase;
import com.example.test.databinding.ItemListBinding;
import com.example.test.entity.TodoEntity;
import com.example.test.fragment.AddFragment;
import com.example.test.utils.LogUtil;

import java.util.ArrayList;
import java.util.List;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.VH> {
    private List<TodoEntity> list = new ArrayList<>();
    private Context context;
    private DeleteData deleteData;
    private String TAG = "Todo_Adapter";

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