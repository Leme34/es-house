package com.lsd.eshouse.common.vo;

import lombok.Data;

/**
 * 服务接口通用结构
 */
@Data
public class ResultVo<T> {
    private boolean success;
    private String message;
    private T result;

    public ResultVo(boolean success) {
        this.success = success;
    }

    public ResultVo(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public ResultVo(boolean success, String message, T result) {
        this.success = success;
        this.message = message;
        this.result = result;
    }


    public static <T> ResultVo<T> success() {
        return new ResultVo<>(true);
    }

    public static <T> ResultVo<T> of(T result) {
        ResultVo<T> resultVo = new ResultVo<>(true);
        resultVo.setResult(result);
        return resultVo;
    }

    public static <T> ResultVo<T> notFound() {
        return new ResultVo<>(false, Message.NOT_FOUND.getValue());
    }

    public enum Message {
        NOT_FOUND("Not Found Resource!"),
        NOT_LOGIN("User not login!");

        private String value;

        Message(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
