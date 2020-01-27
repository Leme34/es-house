package com.lsd.eshouse.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

/**
 * 用户角色表
 */
@Table(name = "role")
@Data
@Entity
public class Role implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", insertable = false, nullable = false)
    private Integer id;

    /**
     * 用户id
     */
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    /**
     * 用户角色名
     */
    @Column(name = "name", nullable = false)
    private String name;


}
