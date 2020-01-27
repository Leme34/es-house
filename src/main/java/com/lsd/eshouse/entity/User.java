package com.lsd.eshouse.entity;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 用户基本信息表
 */
@Data
@Entity
@Table(name = "user")
public class User implements UserDetails, Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户唯一id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", insertable = false, nullable = false)
    private Integer id;

    /**
     * 用户名
     */
    @Column(name = "name")
    private String name;

    /**
     * 电子邮箱
     */
    @Column(name = "email")
    private String email;

    /**
     * 电话号码
     */
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    /**
     * 密码
     */
    @Column(name = "password")
    private String password;

    /**
     * 用户状态 0-正常 1-封禁
     */
    @Column(name = "status", nullable = false)
    private Integer status = 0;

    /**
     * 用户账号创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    /**
     * 上次登录时间
     */
    @Column(name = "last_login_time", nullable = false)
    private LocalDateTime lastLoginTime;

    /**
     * 上次更新记录时间
     */
    @Column(name = "last_update_time", nullable = false)
    private LocalDateTime lastUpdateTime;

    /**
     * 头像
     */
    @Column(name = "avatar")
    private String avatar;

    // 由UserService查询并赋值的权限列表
    @Transient
    private List<GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getUsername() {
        return name;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
