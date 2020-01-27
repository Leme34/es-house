package com.lsd.eshouse.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "subway")
public class Subway implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", insertable = false, nullable = false)
    private Integer id;

    /**
     * 线路名
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * 所属城市英文名缩写
     */
    @Column(name = "city_en_name", nullable = false)
    private String cityEnName;


}
