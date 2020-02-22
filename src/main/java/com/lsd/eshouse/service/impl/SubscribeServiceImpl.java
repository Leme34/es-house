package com.lsd.eshouse.service.impl;

import com.lsd.eshouse.common.constant.HouseSubscribeStatus;
import com.lsd.eshouse.common.dto.HouseDTO;
import com.lsd.eshouse.common.dto.HouseSubscribeDTO;
import com.lsd.eshouse.common.utils.LoginUserUtil;
import com.lsd.eshouse.common.vo.MultiResultVo;
import com.lsd.eshouse.common.vo.ResultVo;
import com.lsd.eshouse.entity.House;
import com.lsd.eshouse.entity.HouseSubscribe;
import com.lsd.eshouse.repository.HouseRepository;
import com.lsd.eshouse.repository.HouseSubscribeRepository;
import com.lsd.eshouse.service.SubscribeService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lsd
 * 2020-02-21 16:07
 */
@Service
public class SubscribeServiceImpl implements SubscribeService {

    @Autowired
    private HouseSubscribeRepository subscribeRepository;
    @Autowired
    private HouseRepository houseRepository;
    @Autowired
    private ModelMapper modelMapper;

    @Override
    @Transactional
    public ResultVo addSubscribeOrder(Integer houseId) {
        Integer userId = LoginUserUtil.getLoginUserId();
        HouseSubscribe subscribe = subscribeRepository.findByHouseIdAndUserId(houseId, userId);
        if (subscribe != null) {
            return new ResultVo(false, "已加入预约");
        }
        // 查询房屋信息
        House house = houseRepository.findById(houseId).get();
        if (house == null) {
            return new ResultVo(false, "查无此房");
        }
        // 入库
        final var now = LocalDateTime.now();
        subscribe = new HouseSubscribe()
                .setCreateTime(now)
                .setLastUpdateTime(now)
                .setUserId(userId)
                .setHouseId(houseId)
                .setStatus(HouseSubscribeStatus.IN_ORDER_LIST.getValue())
                .setAdminId(house.getAdminId());
        subscribeRepository.save(subscribe);
        return ResultVo.success();
    }

    @Override
    public MultiResultVo<Pair<HouseDTO, HouseSubscribeDTO>> querySubscribeList(HouseSubscribeStatus status, int start, int size) {
        Integer userId = LoginUserUtil.getLoginUserId();
        Pageable pageable = PageRequest.of(start / size, size, new Sort(Sort.Direction.DESC, "createTime"));
        Page<HouseSubscribe> page = subscribeRepository.findAllByUserIdAndStatus(userId, status.getValue(), pageable);
        return wrapper(page);
    }

    @Transactional
    @Override
    public ResultVo subscribe(Integer houseId, LocalDateTime orderTime, String telephone, String desc) {
        Integer userId = LoginUserUtil.getLoginUserId();
        HouseSubscribe subscribe = subscribeRepository.findByHouseIdAndUserId(houseId, userId);
        if (subscribe == null) {
            return new ResultVo(false, "无预约记录");
        }
        if (subscribe.getStatus() != HouseSubscribeStatus.IN_ORDER_LIST.getValue()) {
            return new ResultVo(false, "无法预约");
        }
        subscribe.setStatus(HouseSubscribeStatus.IN_ORDER_TIME.getValue())
                .setLastUpdateTime(LocalDateTime.now())
                .setTelephone(telephone)
                .setDesc(desc)
                .setOrderTime(orderTime);
        subscribeRepository.save(subscribe);
        return ResultVo.success();
    }

    @Transactional
    @Override
    public ResultVo cancelSubscribe(Integer houseId) {
        Integer userId = LoginUserUtil.getLoginUserId();
        HouseSubscribe subscribe = subscribeRepository.findByHouseIdAndUserId(houseId, userId);
        if (subscribe == null) {
            return new ResultVo(false, "无预约记录");
        }
        subscribeRepository.deleteById(subscribe.getId());
        return ResultVo.success();
    }

    @Override
    public MultiResultVo<Pair<HouseDTO, HouseSubscribeDTO>> findSubscribeList(int start, int size) {
        Integer userId = LoginUserUtil.getLoginUserId();
        Pageable pageable = PageRequest.of(start / size, size, new Sort(Sort.Direction.DESC, "orderTime"));
        Page<HouseSubscribe> page = subscribeRepository.findAllByAdminIdAndStatus(userId, HouseSubscribeStatus.IN_ORDER_TIME.getValue(), pageable);
        return wrapper(page);
    }

    @Transactional
    @Override
    public ResultVo finishSubscribe(Integer houseId) {
        Integer adminId = LoginUserUtil.getLoginUserId();
        HouseSubscribe subscribe = subscribeRepository.findByHouseIdAndAdminId(houseId, adminId);
        if (subscribe == null) {
            return new ResultVo(false, "无预约记录");
        }
        subscribeRepository.updateStatus(subscribe.getId(), HouseSubscribeStatus.FINISH.getValue());
        // 更新被预约看房的次数
        houseRepository.updateWatchTimes(houseId);
        return ResultVo.success();
    }


    private MultiResultVo<Pair<HouseDTO, HouseSubscribeDTO>> wrapper(Page<HouseSubscribe> page) {
        List<Pair<HouseDTO, HouseSubscribeDTO>> result = new ArrayList<>();
        if (page.getSize() < 1) {
            return new MultiResultVo<>(page.getTotalElements(), result);
        }
        List<HouseSubscribeDTO> subscribeDTOS = new ArrayList<>();
        List<Integer> houseIds = new ArrayList<>();
        page.forEach(houseSubscribe -> {
            subscribeDTOS.add(modelMapper.map(houseSubscribe, HouseSubscribeDTO.class));
            houseIds.add(houseSubscribe.getHouseId());
        });
        Map<Integer, HouseDTO> idToHouseMap = new HashMap<>();
        Iterable<House> houses = houseRepository.findAllById(houseIds);
        houses.forEach(house ->
                idToHouseMap.put(house.getId(), modelMapper.map(house, HouseDTO.class))
        );
        for (HouseSubscribeDTO subscribeDTO : subscribeDTOS) {
            Pair<HouseDTO, HouseSubscribeDTO> pair = Pair.of(idToHouseMap.get(subscribeDTO.getHouseId()), subscribeDTO);
            result.add(pair);
        }
        return new MultiResultVo<>(page.getTotalElements(), result);
    }

}
