package com.lsd.eshouse.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 房屋信息表
 */
@Accessors(chain = true)
@Table(name = "house")
@Entity
@Data
public class House implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * house唯一标识
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", insertable = false, nullable = false)
    private Integer id;

    @Column(name = "title", nullable = false)
    private String title;

    /**
     * 价格
     */
    @Column(name = "price", nullable = false)
    private Integer price;

    /**
     * 面积
     */
    @Column(name = "area", nullable = false)
    private Integer area;

    /**
     * 卧室数量
     */
    @Column(name = "room", nullable = false)
    private Integer room;

    /**
     * 楼层
     */
    @Column(name = "floor", nullable = false)
    private Integer floor;

    /**
     * 总楼层
     */
    @Column(name = "total_floor", nullable = false)
    private Integer totalFloor;

    /**
     * 被看次数
     */
    @Column(name = "watch_times")
    private Integer watchTimes = 0;

    /**
     * 建立年限
     */
    @Column(name = "build_year", nullable = false)
    private Integer buildYear;

    /**
     * 房屋状态 0-未审核 1-审核通过 2-已出租 3-逻辑删除
     */
    @Column(name = "status", nullable = false)
    private Integer status = 0;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    /**
     * 最近数据更新时间
     */
    @Column(name = "last_update_time", nullable = false)
    private LocalDateTime lastUpdateTime;

    /**
     * 城市标记缩写 如 北京bj
     */
    @Column(name = "city_en_name", nullable = false)
    private String cityEnName;

    /**
     * 地区英文简写 如昌平区 cpq
     */
    @Column(name = "region_en_name", nullable = false)
    private String regionEnName;

    /**
     * 封面
     */
    @Column(name = "cover")
    private String cover;

    /**
     * 房屋朝向
     */
    @Column(name = "direction", nullable = false)
    private Integer direction;

    /**
     * 距地铁距离 默认-1 附近无地铁
     */
    @Column(name = "distance_to_subway", nullable = false)
    private Integer distanceToSubway = -1;

    /**
     * 客厅数量
     */
    @Column(name = "parlour", nullable = false)
    private Integer parlour = 0;

    /**
     * 所在小区
     */
    @Column(name = "district", nullable = false)
    private String district;

    /**
     * 所属管理员id
     */
    @Column(name = "admin_id", nullable = false)
    private Integer adminId;

    @Column(name = "bathroom", nullable = false)
    private Integer bathroom = 0;

    /**
     * 街道
     */
    @Column(name = "street", nullable = false)
    private String street;


}
