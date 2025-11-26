package com.example.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code; // 状态码
    private String message; // 返回消息
    private T data; // 返回数据
    // 私有构造器，防止直接实例化
    private Result() {}
    // 静态方法：构建成功结果
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = 200;
        result.message = "Success";
        result.data = data;
        return result;
    }
    // 静态方法：构建失败结果
    public static <T> Result<T> failure(String message) {
        Result<T> result = new Result<>();
        result.code = 500;
        result.message = message;
        return result;
    }
    // 链式编程支持
    public Result<T> code(Integer code) {
        this.code = code;
        return this;
    }
    public Result<T> message(String message) {
        this.message = message;
        return this;
    }
}