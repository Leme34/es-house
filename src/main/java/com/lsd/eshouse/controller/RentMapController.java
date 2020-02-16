package com.lsd.eshouse.controller;

import com.lsd.eshouse.common.dto.HouseBucketDTO;
import com.lsd.eshouse.common.dto.HouseDTO;
import com.lsd.eshouse.common.dto.SupportAddressDTO;
import com.lsd.eshouse.common.form.MapSearchForm;
import com.lsd.eshouse.common.vo.MultiResultVo;
import com.lsd.eshouse.common.vo.R;
import com.lsd.eshouse.common.vo.ResultVo;
import com.lsd.eshouse.service.AddressService;
import com.lsd.eshouse.service.HouseService;
import com.lsd.eshouse.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;

/**
 * 地图页面接口
 * <p>
 * Created by lsd
 * 2020-02-09 10:54
 */
@Controller
public class RentMapController {

    @Autowired
    private AddressService addressService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private HouseService houseService;

    /**
     * 查询选定城市下各个地区的聚合信息
     *
     * @param cityEnName 城市名称
     */
    @GetMapping("rent/house/map")
    public String rentMapPage(@RequestParam(value = "cityEnName") String cityEnName,
                              Model model,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        // 城市是否存在
        ResultVo<SupportAddressDTO> cityResult = addressService.findCity(cityEnName);
        if (!cityResult.isSuccess()) {
            redirectAttributes.addAttribute("msg", "must_chose_city");
            return "redirect:/index";
        }
        session.setAttribute("cityName", cityEnName);
        model.addAttribute("city", cityResult.getResult());
        // 查询该城市下的所有区
        MultiResultVo<SupportAddressDTO> regions = addressService.findAllRegionsByCityName(cityEnName);

        MultiResultVo<HouseBucketDTO> aggResultVo = searchService.mapAggregateByCity(cityEnName);
        model.addAttribute("aggData", aggResultVo.getResult());
        model.addAttribute("total", aggResultVo.getTotal());
        model.addAttribute("regions", regions.getResult());
        return "rent-map";
    }

    /**
     * 根据地图缩放级别查询地图当前视野边界范围内的房源
     */
    @GetMapping("rent/house/map/houses")
    @ResponseBody
    public R rentMapHouses(@ModelAttribute MapSearchForm form) {
        if (StringUtils.isBlank(form.getCityEnName())) {
            return R.ok(HttpStatus.BAD_REQUEST.value(), "必须选择城市");
        }
        MultiResultVo<HouseDTO> houseDTOResult;
        // 如果缩放级别小于13则查询整个城市的房源
        if (form.getLevel() < 13) {
            houseDTOResult = houseService.mapSearchByCity(form);
        } else {
            // 放大后的地图查询必须要传递当前地图视野的边界参数
            houseDTOResult = houseService.mapSearchByBound(form);
        }
        R response = R.ok(houseDTOResult.getResult());
        response.setMore(houseDTOResult.getTotal() > (form.getStart() + form.getSize()));
        return response;
    }


}
