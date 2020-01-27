package com.lsd.eshouse.common.constant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 带区间的常用数值定义
 *
 * Created by lsd
 * 2020-01-27 21:08
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class RentValueRangeBlock {
    private String key;
    private int min;
    private int max;

    /** 价格区间Map */
    public static final Map<String, RentValueRangeBlock> PRICE_RANGE_MAP;

    /** 面积区间定义 */
    public static final Map<String, RentValueRangeBlock> AREA_RANGE_MAP;

    /** 无限制区间 */
    public static final RentValueRangeBlock ALL = new RentValueRangeBlock("*", -1, -1);

    static {
        PRICE_RANGE_MAP = Map.of(
                "*-1000", new RentValueRangeBlock("*-1000", -1, 1000),
                "1000-3000", new RentValueRangeBlock("1000-3000", 1000, 3000),
                "3000-*", new RentValueRangeBlock("3000-*", 3000, -1)
        );

        AREA_RANGE_MAP = Map.of(
                "*-30", new RentValueRangeBlock("*-30", -1, 30),
                "30-50", new RentValueRangeBlock("30-50", 30, 50),
                "50-*", new RentValueRangeBlock("50-*", 50, -1)
        );
    }

    /**
     * 根据key返回对应价格区间
     */
    public static RentValueRangeBlock matchPrice(String key){
        if (key==null){
            return ALL;
        }
        var range = PRICE_RANGE_MAP.get(key);
        if (range == null) {
            return ALL;
        }
        return range;
    }

    /**
     * 根据key返回对应地区区间
     */
    public static RentValueRangeBlock matchArea(String key){
        if (key==null){
            return ALL;
        }
        var range = AREA_RANGE_MAP.get(key);
        if (range == null) {
            return ALL;
        }
        return range;
    }

}
