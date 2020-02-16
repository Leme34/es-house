package com.lsd.eshouse.common.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 请求响应体
 * <p>
 * Created by lsd
 * 2020-01-23 10:05
 */
@NoArgsConstructor
@Data
public class R {

    private int code;
    private String message;
    private Object data;
    // 是否还有更多数据
    private boolean more;

    @Getter
    @AllArgsConstructor
    public enum StatusEnum {
        SUCCESS(200, "success"),
        BAD_REQUEST(400, "Bad Request"),
        NOT_FOUND(404, "Not Found"),
        INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
        NOT_VALID_PARAM(40005, "Not valid Params"),
        NOT_SUPPORTED_OPERATION(40006, "Operation not supported"),
        NOT_LOGIN(50000, "Not Login");

        private int code;
        private String message;
    }

    public R(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static R ok() {
        return new R(StatusEnum.SUCCESS.getCode(), StatusEnum.SUCCESS.getMessage(), null);
    }

    public static R ok(Object data) {
        return new R(StatusEnum.SUCCESS.getCode(), StatusEnum.SUCCESS.getMessage(), data);
    }

    public static R ok(int code, String message) {
        return new R(code, message, null);
    }

    public static R error(String message, Object data) {
        return new R(500, message, data);
    }

    public static R error(StatusEnum statusEnum, Object data) {
        return new R(statusEnum.getCode(), statusEnum.getMessage(), data);
    }

    public static R error(StatusEnum statusEnum) {
        return new R(statusEnum.getCode(), statusEnum.getMessage(), null);
    }

    public static R error(int code, String message) {
        return new R(code, message, null);
    }

}
