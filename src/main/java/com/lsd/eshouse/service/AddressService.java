package com.lsd.eshouse.service;

import com.lsd.eshouse.common.dto.SubwayDTO;
import com.lsd.eshouse.common.dto.SubwayStationDTO;
import com.lsd.eshouse.common.dto.SupportAddressDTO;
import com.lsd.eshouse.entity.SupportAddress;
import com.lsd.eshouse.common.vo.MultiResultVo;
import com.lsd.eshouse.common.vo.ResultVo;

import java.util.List;
import java.util.Map;

/**
 * 地址服务接口
 *
 * Created by lsd
 * 2020-01-26 09:30
 */
public interface AddressService {

    /**
     * 获取所有支持的城市列表
     */
    MultiResultVo<SupportAddressDTO> findAllCities();

    /**
     * 根据英文简写获取具体区域的信息
     */
    Map<SupportAddress.Level, SupportAddressDTO> findCityAndRegion(String cityEnName, String regionEnName);

    /**
     * 根据城市英文简写获取该城市所有支持的区域信息
     */
    MultiResultVo<SupportAddressDTO> findAllRegionsByCityName(String cityName);

    /**
     * 获取该城市所有的地铁线路
     */
    List<SubwayDTO> findAllSubwayByCity(String cityEnName);

    /**
     * 获取地铁线路所有的站点
     */
    List<SubwayStationDTO> findAllStationBySubway(Integer subwayId);

    /**
     * 获取地铁线信息
     */
    ResultVo<SubwayDTO> findSubway(Integer subwayId);

    /**
     * 获取地铁站点信息
     */
    ResultVo<SubwayStationDTO> findSubwayStation(Integer stationId);

    /**
     * 根据城市英文简写获取城市详细信息
     */
    ResultVo<SupportAddressDTO> findCity(String cityEnName);

}
