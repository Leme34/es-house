package com.lsd.eshouse.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 索引中存储的自动补全关键词对象
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class HouseSuggest {
    private String input;
    private int weight = 10; // 默认权重

    public HouseSuggest(String input){
        this.input = input;
    }
}
