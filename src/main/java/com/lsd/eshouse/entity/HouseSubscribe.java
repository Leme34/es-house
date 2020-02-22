package com.lsd.eshouse.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预约看房信息表
 */
@Accessors(chain = true)
@Data
@Entity
@Table(name = "house_subscribe")
public class HouseSubscribe implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 房源发布者id
     */
    @Column(name = "admin_id", nullable = false)
    private Integer adminId;

    /**
     * 数据创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    /**
     * 用户描述
     */
    @Column(name = "`desc`")
    private String desc;

    /**
     * 房源id
     */
    @Column(name = "house_id", nullable = false)
    private Integer houseId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", insertable = false, nullable = false)
    private Integer id;

    /**
     * 记录更新时间
     */
    @Column(name = "last_update_time", nullable = false)
    private LocalDateTime lastUpdateTime;

    /**
     * 预约时间
     */
    @Column(name = "order_time")
    private LocalDateTime orderTime;

    /**
     * 预约状态 1-加入待看清单 2-已预约看房时间 3-看房完成
     */
    @Column(name = "status", nullable = false)
    private Integer status = 0;

    /**
     * 联系电话
     */
    @Column(name = "telephone")
    private String telephone;

    /**
     * 用户id
     */
    @Column(name = "user_id", nullable = false)
    private Integer userId;


}
