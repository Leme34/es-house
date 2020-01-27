package com.lsd.eshouse.repository;

import com.lsd.eshouse.entity.Subway;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface SubwayRepository extends JpaRepository<Subway, Integer>, JpaSpecificationExecutor<Subway> {

    List<Subway> findAllByCityEnName(String cityEnName);

}
