package com.lsd.eshouse.service;

import com.lsd.eshouse.common.dto.HouseBucketDTO;
import com.lsd.eshouse.common.form.MapSearchForm;
import com.lsd.eshouse.common.form.RentSearchForm;
import com.lsd.eshouse.common.vo.MultiResultVo;
import com.lsd.eshouse.common.vo.ResultVo;

import java.util.List;

/**
 * 房源搜索
 * <p>
 * Created by lsd
 * 2020-01-28 22:02
 */
public interface SearchService {

    /**
     * 提供给kafka消息监听调用的真正建立房源索引接口
     *
     * @param retry 当前索引构建已重试的次数
     */
    void doIndex(Integer houseId, int retry);


    /**
     * 建立房源索引接口，只是发送kafka消息
     */
    void index(Integer houseId);

    /**
     * 提供给kafka消息监听调用的真正房源索引删除接口
     *
     * @param retry 当前索引删除已重试的次数
     */
    void doRemove(Integer houseId, int retry);

    /**
     * 删除房源索引，只是发送kafka消息
     */
    void remove(Integer houseId);

    /**
     * 房源搜索接口
     *
     * @return 匹配的房源ids
     */
    MultiResultVo<Integer> search(RentSearchForm searchForm);

    /**
     * 根据输入内容获取补全建议关键词
     * @param prefix 用户输入的内容
     */
    ResultVo<List<String>> suggest(String prefix);


    /**
     * 聚合特定小区的房源数
     */
    ResultVo<Long> aggregateDistrictHouse(String cityEnName, String regionEnName, String district);


    /**
     * 聚合地图页面该城市下每个地区的数据
     * @param cityEnName 城市名称
     */
    MultiResultVo<HouseBucketDTO> mapAggregateByCity(String cityEnName);

    /**
     * 搜索出此城市的所有房源id
     */
    MultiResultVo<Integer> mapSearchByCity(MapSearchForm form);

    /**
     * 搜索出此边界范围的所有房源id
     */
    MultiResultVo<Integer> mapSearchByBound(MapSearchForm form);
}
