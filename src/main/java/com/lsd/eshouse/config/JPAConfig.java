package com.lsd.eshouse.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * jpa配置类
 * <p>
 * Created by lsd
 * 2020-01-22 11:29
 */
@EntityScan(basePackages = "com.lsd.eshouse.entity") //实体类扫描
@EnableJpaRepositories(basePackages = "com.lsd.eshouse.repository") //dao包扫描
@Configuration
public class JPAConfig {

}
