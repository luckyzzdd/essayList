package com.example.test.entity;

public class HitokotoBean {
    // 字段名必须和JSON的key一致（或用@SerializedName注解）
    private String hitokoto; // 文案内容
    private String from;     // 文案来源

    // Getter + Setter（必须有，Gson反射用）
    public String getHitokoto() {
        return hitokoto;
    }

    public void setHitokoto(String hitokoto) {
        this.hitokoto = hitokoto;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
