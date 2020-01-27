package com.lsd.eshouse.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

/**
 * 房屋标签映射关系表
 */
@NoArgsConstructor
@Data
@Entity
@Table(name = "house_tag")
public class HouseTag implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 房屋id
     */
    @Column(name = "house_id", nullable = false)
    private Integer houseId;

    /**
     * 标签id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", insertable = false, nullable = false)
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    public HouseTag(Integer houseId, String name) {
        this.houseId = houseId;
        this.name = name;
    }

}
