package com.lsd.eshouse.controller;

import com.lsd.eshouse.common.vo.R;
import com.lsd.eshouse.common.dto.SubwayDTO;
import com.lsd.eshouse.common.dto.SubwayStationDTO;
import com.lsd.eshouse.common.dto.SupportAddressDTO;
import com.lsd.eshouse.service.AddressService;
import com.lsd.eshouse.common.vo.MultiResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 地区
 *
 * Created by lsd
 * 2020-01-26 12:02
 */
@Controller
public class AddressController {

    @Autowired
    private AddressService addressService;

    /**
     * 获取支持城市列表
     */
    @GetMapping("address/support/cities")
    @ResponseBody
    public R getSupportCities() {
        MultiResultVo<SupportAddressDTO> result = addressService.findAllCities();
        if (result.getResultSize() == 0) {
            return R.ok(R.StatusEnum.NOT_FOUND);
        }
        return R.ok(result.getResult());
    }

    /**
     * 获取对应城市支持区域列表
     */
    @GetMapping("address/support/regions")
    @ResponseBody
    public R getSupportRegions(@RequestParam(name = "city_name") String cityEnName) {
        MultiResultVo<SupportAddressDTO> addressResult = addressService.findAllRegionsByCityName(cityEnName);
        if (addressResult.getResult() == null || addressResult.getTotal() < 1) {
            return R.ok(R.StatusEnum.NOT_FOUND);
        }
        return R.ok(addressResult.getResult());
    }

    /**
     * 获取具体城市所支持的地铁线路
     */
    @GetMapping("address/support/subway/line")
    @ResponseBody
    public R getSupportSubwayLine(@RequestParam(name = "city_name") String cityEnName) {
        List<SubwayDTO> subways = addressService.findAllSubwayByCity(cityEnName);
        if (subways.isEmpty()) {
            return R.ok(R.StatusEnum.NOT_FOUND);
        }
        return R.ok(subways);
    }

    /**
     * 获取对应地铁线路所支持的地铁站点
     */
    @GetMapping("address/support/subway/station")
    @ResponseBody
    public R getSupportSubwayStation(@RequestParam(name = "subway_id") Integer subwayId) {
        List<SubwayStationDTO> stationDTOS = addressService.findAllStationBySubway(subwayId);
        if (stationDTOS.isEmpty()) {
            return R.ok(R.StatusEnum.NOT_FOUND);
        }
        return R.ok(stationDTOS);
    }

}
