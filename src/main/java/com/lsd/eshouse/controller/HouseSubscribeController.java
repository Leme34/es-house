package com.lsd.eshouse.controller;

import com.lsd.eshouse.common.constant.HouseSubscribeStatus;
import com.lsd.eshouse.common.dto.HouseDTO;
import com.lsd.eshouse.common.dto.HouseSubscribeDTO;
import com.lsd.eshouse.common.utils.LoginUserUtil;
import com.lsd.eshouse.common.vo.ApiDataTableResponse;
import com.lsd.eshouse.common.vo.MultiResultVo;
import com.lsd.eshouse.common.vo.R;
import com.lsd.eshouse.common.vo.ResultVo;
import com.lsd.eshouse.service.HouseService;
import com.lsd.eshouse.service.SubscribeService;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 我的待看清单、预约看房 接口
 * <p>
 * Created by lsd
 * 2020-02-21 15:10
 */
@Controller
public class HouseSubscribeController {

    @Autowired
    private SubscribeService subscribeService;

    /**
     * 把房源加入我的待看清单
     *
     * @param houseId
     * @return
     */
    @PostMapping(value = "api/user/house/subscribe")
    @ResponseBody
    public R subscribeHouse(@RequestParam(value = "house_id") Integer houseId) {
        ResultVo result = subscribeService.addSubscribeOrder(houseId);
        if (result.isSuccess()) {
            return R.ok("");
        } else {
            return R.ok(HttpStatus.SC_BAD_REQUEST, result.getMessage());
        }
    }

    /**
     * 查询我的待看清单
     */
    @GetMapping(value = "api/user/house/subscribe/list")
    @ResponseBody
    public R userSubscribeList(
            @RequestParam(value = "start", defaultValue = "0") int start,
            @RequestParam(value = "size", defaultValue = "3") int size,
            @RequestParam(value = "status") int status) {
        MultiResultVo<Pair<HouseDTO, HouseSubscribeDTO>> result = subscribeService.querySubscribeList(HouseSubscribeStatus.of(status), start, size);
        if (result.getResultSize() == 0) {
            return R.ok(result.getResult());
        }
        R response = R.ok(result.getResult());
        response.setMore(result.getTotal() > (start + size));
        return response;
    }


    /**
     * 用户预约看房
     */
    @PostMapping(value = "api/user/house/subscribe/date")
    @ResponseBody
    public R subscribeDate(
            @RequestParam(value = "houseId") Integer houseId,
            @RequestParam(value = "orderTime") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDateTime orderTime,
            @RequestParam(value = "desc", required = false) String desc,
            @RequestParam(value = "telephone") String telephone) {
        if (orderTime == null) {
            return R.ok(HttpStatus.SC_BAD_REQUEST, "请选择预约时间");
        }
        if (!LoginUserUtil.checkTelephone(telephone)) {
            return R.ok(HttpStatus.SC_BAD_REQUEST, "手机格式不正确");
        }
        ResultVo serviceResult = subscribeService.subscribe(houseId, orderTime, telephone, desc);
        if (serviceResult.isSuccess()) {
            return R.ok(R.StatusEnum.SUCCESS);
        } else {
            return R.ok(HttpStatus.SC_BAD_REQUEST, serviceResult.getMessage());
        }
    }

    /**
     * 用户取消预约看房
     */
    @DeleteMapping(value = "api/user/house/subscribe")
    @ResponseBody
    public R cancelSubscribe(@RequestParam(value = "houseId") Integer houseId) {
        ResultVo serviceResult = subscribeService.cancelSubscribe(houseId);
        if (serviceResult.isSuccess()) {
            return R.ok(R.StatusEnum.SUCCESS);
        } else {
            return R.ok(HttpStatus.SC_BAD_REQUEST, serviceResult.getMessage());
        }
    }


    /**
     * 管理员查看用户预约看房记录页面
     */
    @GetMapping("admin/house/subscribe")
    public String houseSubscribe() {
        return "admin/subscribe";
    }

    /**
     * 管理员查看用户预约看房记录
     */
    @GetMapping("admin/house/subscribe/list")
    @ResponseBody
    public R adminSubscribeList(@RequestParam(value = "draw") int draw,
                           @RequestParam(value = "start") int start,
                           @RequestParam(value = "length") int size) {
        MultiResultVo<Pair<HouseDTO, HouseSubscribeDTO>> result = subscribeService.findSubscribeList(start, size);
        ApiDataTableResponse response = new ApiDataTableResponse(R.StatusEnum.SUCCESS)
                .setDraw(draw)
                .setRecordsFiltered(result.getTotal())
                .setRecordsTotal(result.getTotal());
        response.setData(result.getResult());
        return response;
    }


    /**
     * 管理员确认完成用户看房预约
     */
    @PostMapping("admin/finish/subscribe")
    @ResponseBody
    public R finishSubscribe(@RequestParam(value = "house_id") Integer houseId) {
        if (houseId < 1) {
            return R.ok(R.StatusEnum.BAD_REQUEST);
        }
        ResultVo serviceResult = subscribeService.finishSubscribe(houseId);
        if (serviceResult.isSuccess()) {
            return R.ok("");
        } else {
            return R.ok(R.StatusEnum.BAD_REQUEST.getCode(), serviceResult.getMessage());
        }
    }

}
