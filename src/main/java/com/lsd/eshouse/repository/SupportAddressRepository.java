package com.lsd.eshouse.repository;

import com.lsd.eshouse.entity.SupportAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface SupportAddressRepository extends JpaRepository<SupportAddress, Integer>, JpaSpecificationExecutor<SupportAddress> {

    /**
     * 获取所有支持的行政单位（地区）信息
     */
    List<SupportAddress> findAllByLevel(String level);

    SupportAddress findByEnNameAndLevel(String enName, String level);

    SupportAddress findByEnNameAndBelongTo(String enName, String belongTo);

    List<SupportAddress> findAllByLevelAndBelongTo(String level, String belongTo);

}
