package com.lsd.eshouse.service;

import com.lsd.eshouse.common.dto.HouseDTO;
import com.lsd.eshouse.common.form.DatatableSearchForm;
import com.lsd.eshouse.common.form.HouseForm;
import com.lsd.eshouse.common.form.MapSearchForm;
import com.lsd.eshouse.common.form.RentSearchForm;
import com.lsd.eshouse.common.vo.MultiResultVo;
import com.lsd.eshouse.common.vo.ResultVo;

/**
 * 房屋管理服务接口
 */
public interface HouseService {
    /**
     * 新增
     */
    ResultVo<HouseDTO> save(HouseForm houseForm);

    MultiResultVo<HouseDTO> adminQuery(DatatableSearchForm searchBody);

    /**
     * 查询完整房源信息
     */
    ResultVo<HouseDTO> findCompleteOne(Integer id);

    ResultVo update(HouseForm houseForm);

    /**
     * 移除图片
     */
    ResultVo removePhoto(Integer id);

    ResultVo updateCover(Integer coverId, Integer targetId);

    /**
     * 更新房源状态
     */
    ResultVo updateStatus(Integer id, int status);

    /**
     * 查询房源信息
     */
    MultiResultVo<HouseDTO> query(RentSearchForm searchForm);

    /**
     * 地图查询整个城市的房源
     */
    MultiResultVo<HouseDTO> mapSearchByCity(MapSearchForm form);

    /**
     * 地图查询当前地图视野边界范围内的房源
     */
    MultiResultVo<HouseDTO> mapSearchByBound(MapSearchForm form);
}
