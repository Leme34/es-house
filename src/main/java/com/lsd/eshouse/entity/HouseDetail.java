package com.lsd.eshouse.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.io.Serializable;

@Accessors(chain = true)
@Entity
@Table(name = "house_detail")
@Data
public class HouseDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", insertable = false, nullable = false)
    private Integer id;

    /**
     * 详细描述
     */
    @Column(name = "description")
    private String description;

    /**
     * 户型介绍
     */
    @Column(name = "layout_desc")
    private String layoutDesc;

    /**
     * 交通出行
     */
    @Column(name = "traffic")
    private String traffic;

    /**
     * 周边配套
     */
    @Column(name = "round_service")
    private String roundService;

    /**
     * 租赁方式
     */
    @Column(name = "rent_way", nullable = false)
    private Integer rentWay;

    /**
     * 详细地址
     */
    @Column(name = "address", nullable = false)
    private String address;

    /**
     * 附近地铁线id
     */
    @Column(name = "subway_line_id")
    private Integer subwayLineId;

    /**
     * 附近地铁线名称
     */
    @Column(name = "subway_line_name")
    private String subwayLineName;

    /**
     * 地铁站id
     */
    @Column(name = "subway_station_id")
    private Integer subwayStationId;

    /**
     * 地铁站名
     */
    @Column(name = "subway_station_name")
    private String subwayStationName;

    /**
     * 对应house的id
     */
    @Column(name = "house_id", nullable = false)
    private Integer houseId;


}
