package com.lsd.eshouse.common.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 前端框架Datatables响应结构
 */
@Accessors(chain = true)
@Data
public class ApiDataTableResponse extends R {
    private int draw;
    private long recordsTotal;
    private long recordsFiltered;

    public ApiDataTableResponse(R.StatusEnum status) {
        this(status.getCode(), status.getMessage(), null);
    }

    public ApiDataTableResponse(int code, String message, Object data) {
        super(code, message, data);
    }

}
