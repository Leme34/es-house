package com.lsd.eshouse.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import javax.persistence.*;
import java.io.Serializable;

/**
 * 支持的行政单位（地区）
 */
@Entity
@Table(name = "support_address")
@Data
public class SupportAddress implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", insertable = false, nullable = false)
    private Integer id;

    /**
     * 上一级行政单位名
     */
    @Column(name = "belong_to", nullable = false)
    private String belongTo = "0";

    /**
     * 行政单位英文名缩写
     */
    @Column(name = "en_name", nullable = false)
    private String enName;

    /**
     * 行政单位中文名
     */
    @Column(name = "cn_name", nullable = false)
    private String cnName;

    /**
     * 行政级别 市-city 地区-region
     */
    @Column(name = "level", nullable = false)
    private String level;

    @AllArgsConstructor
    @Getter
    public enum Level{
        CITY("city"),
        REGION("region");

        private String value;

        public static Level of(String value) {
            for (Level level : Level.values()) {
                if (level.getValue().equals(value)) {
                    return level;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    /**
     * 百度地图经度
     */
    @Column(name = "baidu_map_lng", nullable = false)
    private Double baiduMapLng;

    /**
     * 百度地图纬度
     */
    @Column(name = "baidu_map_lat", nullable = false)
    private Double baiduMapLat;


}
