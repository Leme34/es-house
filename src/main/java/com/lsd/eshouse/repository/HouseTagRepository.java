package com.lsd.eshouse.repository;

import com.lsd.eshouse.entity.HouseTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Set;

public interface HouseTagRepository extends JpaRepository<HouseTag, Integer>, JpaSpecificationExecutor<HouseTag> {

    List<HouseTag> findAllByHouseId(Integer id);

    List<HouseTag> findAllByHouseIdIn(Set<Integer> keySet);
}
