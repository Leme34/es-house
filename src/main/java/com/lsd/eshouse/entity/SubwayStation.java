package com.lsd.eshouse.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "subway_station")
public class SubwayStation implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", insertable = false, nullable = false)
    private Integer id;

    /**
     * 所属地铁线id
     */
    @Column(name = "subway_id", nullable = false)
    private Integer subwayId;

    /**
     * 站点名称
     */
    @Column(name = "name", nullable = false)
    private String name;


}
