package com.lsd.eshouse.repository;

import com.lsd.eshouse.entity.SubwayStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface SubwayStationRepository extends JpaRepository<SubwayStation, Integer>, JpaSpecificationExecutor<SubwayStation> {

    List<SubwayStation> findAllBySubwayId(Integer subwayId);

}
