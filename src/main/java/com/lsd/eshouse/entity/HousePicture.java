package com.lsd.eshouse.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

/**
 * 房屋图片信息
 */
@Data
@Table(name = "house_picture")
@Entity
public class HousePicture implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", insertable = false, nullable = false)
    private Integer id;

    /**
     * 所属房屋id
     */
    @Column(name = "house_id", nullable = false)
    private Integer houseId;

    /**
     * 图片路径
     */
    @Column(name = "cdn_prefix", nullable = false)
    private String cdnPrefix;

    /**
     * 宽
     */
    @Column(name = "width")
    private Integer width;

    /**
     * 高
     */
    @Column(name = "height")
    private Integer height;

    /**
     * 所属房屋位置
     */
    @Column(name = "location")
    private String location;

    /**
     * 文件名
     */
    @Column(name = "path", nullable = false)
    private String path;


}
