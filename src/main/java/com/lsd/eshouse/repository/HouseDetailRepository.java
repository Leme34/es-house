package com.lsd.eshouse.repository;

import com.lsd.eshouse.entity.HouseDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Set;

public interface HouseDetailRepository extends JpaRepository<HouseDetail, Integer>, JpaSpecificationExecutor<HouseDetail> {

    HouseDetail findByHouseId(Integer houseId);

    List<HouseDetail> findAllByHouseIdIn(List<Integer> houseIds);

}
