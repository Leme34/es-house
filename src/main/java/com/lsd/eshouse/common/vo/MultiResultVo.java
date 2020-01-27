package com.lsd.eshouse.common.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 通用List数据Vo
 */
@AllArgsConstructor
@Data
public class MultiResultVo<T> {
    private long total;
    private List<T> result;

    public int getResultSize() {
        return this.result == null ? 0 : this.result.size();
    }
}
