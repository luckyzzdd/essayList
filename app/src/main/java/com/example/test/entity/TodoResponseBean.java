package com.example.test.entity;

/**
 * POST请求的响应实体类（和接口返回的JSON对应）
 */
public class TodoResponseBean {
    // 核心字段：接口返回的提交参数（和你POST的内容一致）
    private TodoPostBean json;

    // Getter + Setter
    public TodoPostBean getJson() { return json; }
    public void setJson(TodoPostBean json) { this.json = json; }
}