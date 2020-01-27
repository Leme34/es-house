package com.lsd.eshouse.repository;

import com.lsd.eshouse.entity.HousePicture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface HousePictureRepository extends JpaRepository<HousePicture, Integer>, JpaSpecificationExecutor<HousePicture> {

    List<HousePicture> findAllByHouseId(Integer id);
}
