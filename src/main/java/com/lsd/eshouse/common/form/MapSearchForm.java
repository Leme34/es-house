package com.lsd.eshouse.common.form;

import lombok.Data;

/**
 * 地图页面查询表单
 * <p>
 * Created by lsd
 * 2020-02-11 11:25
 */
@Data
public class MapSearchForm {
    /** 当前城市名称 */
    private String cityEnName;

    /** 地图缩放级别 */
    private int level = 12;

    /**
     * 地图左上角经纬度，用于确定地图显区域
     */
    private Double leftLongitude;
    private Double leftLatitude;
    /**
     * 地图右下角经纬度，用于确定地图显区域
     */
    private Double rightLongitude;
    private Double rightLatitude;

    /**
     * 分页排序相关参数
     */
    private String orderBy = "lastUpdateTime";
    private String orderDirection = "desc";
    private int start = 0;
    private int size = 5;
    // 保证start>=0
    public int getStart() {
        return Math.max(start, 0);
    }
    // 保证size<=100
    public int getSize() {
        return Math.min(size, 100);
    }

}
