package com.lsd.eshouse.controller;

import com.lsd.eshouse.common.constant.RentValueRangeBlock;
import com.lsd.eshouse.common.dto.HouseDTO;
import com.lsd.eshouse.common.dto.SupportAddressDTO;
import com.lsd.eshouse.common.dto.UserDTO;
import com.lsd.eshouse.common.form.RentSearchForm;
import com.lsd.eshouse.common.vo.MultiResultVo;
import com.lsd.eshouse.common.vo.R;
import com.lsd.eshouse.common.vo.ResultVo;
import com.lsd.eshouse.entity.SupportAddress;
import com.lsd.eshouse.service.AddressService;
import com.lsd.eshouse.service.HouseService;
import com.lsd.eshouse.service.SearchService;
import com.lsd.eshouse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * 普通用户租房页面相关
 * <p>
 * Created by lsd
 * 2020-01-28 10:29
 */
@Controller
public class RentController {

    @Autowired
    private AddressService addressService;
    @Autowired
    private HouseService houseService;
    @Autowired
    private UserService userService;
    @Autowired
    private SearchService searchService;

    /**
     * 租房页面查询接口
     */
    @GetMapping("rent/house")
    public String rentHousePage(@ModelAttribute RentSearchForm searchForm,
                                Model model, HttpSession session,
                                RedirectAttributes redirectAttributes) {
        // 若searchForm没有选择城市，则用session中的城市
        if (searchForm.getCityEnName() == null) {
            String cityEnNameInSession = (String) session.getAttribute("cityEnName");
            if (cityEnNameInSession == null) {
                redirectAttributes.addAttribute("msg", "must_chose_city");
                return "redirect:/index";
            }
            searchForm.setCityEnName(cityEnNameInSession);
        } else {
            // searchForm选择了城市则放入session
            session.setAttribute("cityEnName", searchForm.getCityEnName());
        }
        // 查询城市是否存在
        ResultVo<SupportAddressDTO> city = addressService.findCity(searchForm.getCityEnName());
        if (!city.isSuccess()) {
            redirectAttributes.addAttribute("msg", "must_chose_city");
            return "redirect:/index";
        }
        model.addAttribute("currentCity", city.getResult());

        // 查询城市下的所有地区
        MultiResultVo<SupportAddressDTO> addressResult = addressService.findAllRegionsByCityName(searchForm.getCityEnName());
        if (addressResult.getResult() == null || addressResult.getTotal() < 1) {
            redirectAttributes.addAttribute("msg", "must_chose_city");
            return "redirect:/index";
        }

        // 查询所有房源
        MultiResultVo<HouseDTO> serviceMultiResult = houseService.query(searchForm);
        model.addAttribute("total", serviceMultiResult.getTotal());
        model.addAttribute("houses", serviceMultiResult.getResult());
        if (searchForm.getRegionEnName() == null) {
            searchForm.setRegionEnName("*");
        }
        model.addAttribute("searchBody", searchForm);
        model.addAttribute("regions", addressResult.getResult());
        model.addAttribute("priceBlocks", RentValueRangeBlock.PRICE_RANGE_MAP);
        model.addAttribute("areaBlocks", RentValueRangeBlock.AREA_RANGE_MAP);
        model.addAttribute("currentPriceBlock", RentValueRangeBlock.matchPrice(searchForm.getPriceBlock()));
        model.addAttribute("currentAreaBlock", RentValueRangeBlock.matchArea(searchForm.getAreaBlock()));
        return "rent-list";
    }

    /**
     * house详情页面
     */
    @GetMapping("rent/house/show/{id}")
    public String show(@PathVariable(value = "id") Integer houseId,
                       Model model) {
        if (houseId <= 0) {
            return "404";
        }
        // 查询house
        ResultVo<HouseDTO> serviceResult = houseService.findCompleteOne(houseId);
        if (!serviceResult.isSuccess()) {
            return "404";
        }
        HouseDTO houseDTO = serviceResult.getResult();
        // 查询城市地区信息
        Map<SupportAddress.Level, SupportAddressDTO>
                addressMap = addressService.findCityAndRegion(houseDTO.getCityEnName(), houseDTO.getRegionEnName());
        SupportAddressDTO city = addressMap.get(SupportAddress.Level.CITY);
        SupportAddressDTO region = addressMap.get(SupportAddress.Level.REGION);
        model.addAttribute("city", city);
        model.addAttribute("region", region);
        // 用户信息
        ResultVo<UserDTO> userDTOServiceResult = userService.findById(houseDTO.getAdminId());
        model.addAttribute("agent", userDTOServiceResult.getResult());
        model.addAttribute("house", houseDTO);
        // 地区房源数量聚合信息
        final ResultVo<Long> districtAggResult = searchService.aggregateDistrictHouse(city.getEnName(), region.getEnName(), houseDTO.getDistrict());
        model.addAttribute("houseCountInDistrict", districtAggResult.getResult());
        return "house-detail";
    }

    /**
     * 自动补全接口
     */
    @GetMapping("rent/house/autocomplete")
    @ResponseBody
    public R autocomplete(@RequestParam(value = "prefix") String prefix) {

        if (prefix.isEmpty()) {
            return R.ok(R.StatusEnum.BAD_REQUEST);
        }
        ResultVo<List<String>> result = searchService.suggest(prefix);
        return R.ok(result.getResult());
    }

}
