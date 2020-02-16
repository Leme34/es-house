package com.lsd.eshouse.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聚合结果DTO
 *
 * Created by lsd
 * 2020-02-09 11:11
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class HouseBucketDTO {

    /**
     * 聚合bucket的key
     */
    private String key;

    /**
     * 聚合结果值
     */
    private long count;

}
