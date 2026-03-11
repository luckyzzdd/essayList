package com.example.test.entity;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

//建表，自动生产建表sql
@Entity(tableName = "todo_table")
public class TodoEntity  implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    private long id; // 待办ID（唯一标识）
    private String content;// 待办内容字段
    private String createTime; // 待办创建时间（新手拓展：方便后续排序）
    private String title;
    //默认没有同步，只在同步的时候，给标记
    private boolean isSync=false;
    //room通过反射创建对象需要

    public boolean isSync() {
        return isSync;
    }

    public void setSync(boolean sync) {
        isSync = sync;
    }

    public TodoEntity() {
    }
    @Ignore
    public TodoEntity(String content, String createTime,String title) {
        this.content = content;
        this.createTime = createTime;
        this.title= title;
    }

    protected TodoEntity(Parcel in) {
        id = in.readLong();
        content = in.readString();
        createTime = in.readString();
    }

    public static final Creator<TodoEntity> CREATOR = new Creator<TodoEntity>() {
        @Override
        public TodoEntity createFromParcel(Parcel in) {
            return new TodoEntity(in);
        }

        @Override
        public TodoEntity[] newArray(int size) {
            return new TodoEntity[size];
        }
    };
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(content);
        dest.writeString(createTime);
        dest.writeString(title);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            dest.writeBoolean(isSync);
        }
    }
}
