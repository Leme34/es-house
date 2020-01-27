package com.lsd.eshouse.common.dto;

import lombok.Data;

/**
 * 七牛云API操作的返回结果DTO
 */
@Data
public final class QiNiuPutResult {
    public String key;
    public String hash;
    public String bucket;
    public int width;
    public int height;
}
