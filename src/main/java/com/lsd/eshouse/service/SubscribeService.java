package com.lsd.eshouse.service;

import com.lsd.eshouse.common.constant.HouseSubscribeStatus;
import com.lsd.eshouse.common.dto.HouseDTO;
import com.lsd.eshouse.common.dto.HouseSubscribeDTO;
import com.lsd.eshouse.common.vo.MultiResultVo;
import com.lsd.eshouse.common.vo.ResultVo;
import org.springframework.data.util.Pair;

import java.time.LocalDateTime;

/**
 * Created by lsd
 * 2020-02-21 16:06
 */
public interface SubscribeService {

    /**
     * 把房源加入我的待看清单
     *
     * @param houseId
     */
    ResultVo addSubscribeOrder(Integer houseId);

    /**
     * 获取对应状态的待看清单
     */
    MultiResultVo<Pair<HouseDTO, HouseSubscribeDTO>> querySubscribeList(HouseSubscribeStatus status, int start, int size);

    /**
     * 预约看房
     */
    ResultVo subscribe(Integer houseId, LocalDateTime orderTime, String telephone, String desc);

    /**
     * 取消预约看房
     */
    ResultVo cancelSubscribe(Integer houseId);

    /**
     * 管理员查询预约信息接口
     *
     * @param start
     * @param size
     */
    MultiResultVo<Pair<HouseDTO, HouseSubscribeDTO>> findSubscribeList(int start, int size);

    /**
     * 管理员确认完成用户看房预约
     */
    ResultVo finishSubscribe(Integer houseId);
}
