package com.example.test.entity;

/**
 * POST请求的参数实体类（和接口要求的JSON字段对应）
 */
public class TodoPostBean {
    private String title;       // 随笔标题
    private String content;     // 随笔内容
    private String createTime;  // 创建时间

    // 构造方法（方便快速创建对象）
    public TodoPostBean(String title, String content, String createTime) {
        this.title = title;
        this.content = content;
        this.createTime = createTime;
    }
    // Getter + Setter（Gson转JSON需要）
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}