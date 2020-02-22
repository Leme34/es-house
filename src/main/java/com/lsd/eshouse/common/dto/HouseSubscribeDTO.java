package com.lsd.eshouse.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约看房实体类
 */
@Data
public class HouseSubscribeDTO {
    private Long id;

    private Long houseId;

    private Long userId;

    private Long adminId;

    // 预约状态 1-加入待看清单 2-已预约看房时间 3-看房完成
    private int status;

    private LocalDateTime createTime;

    private LocalDateTime lastUpdateTime;

    private LocalDateTime orderTime;

    private String telephone;

    private String desc;

}
