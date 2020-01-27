package com.lsd.eshouse.controller;

import com.lsd.eshouse.common.constant.HouseOperation;
import com.lsd.eshouse.common.constant.HouseStatus;
import com.lsd.eshouse.common.constant.RentValueRangeBlock;
import com.lsd.eshouse.common.dto.*;
import com.lsd.eshouse.common.form.DatatableSearchForm;
import com.lsd.eshouse.common.form.RentSearchForm;
import com.lsd.eshouse.common.vo.ApiDataTableResponse;
import com.lsd.eshouse.common.vo.R;
import com.lsd.eshouse.entity.SupportAddress;
import com.lsd.eshouse.common.form.HouseForm;
import com.lsd.eshouse.service.AddressService;
import com.lsd.eshouse.service.HouseService;
import com.lsd.eshouse.common.vo.MultiResultVo;
import com.lsd.eshouse.common.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.Map;

/**
 * 房子
 * <p>
 * Created by lsd
 * 2020-01-25 22:36
 */
@Controller
public class HouseController {

    @Autowired
    private HouseService houseService;
    @Autowired
    private AddressService addressService;


    /**
     * 新增房源接口
     */
    @PostMapping("admin/add/house")
    @ResponseBody
    public R addHouse(@Valid @ModelAttribute("form-house-add") HouseForm houseForm, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return new R(HttpStatus.BAD_REQUEST.value(), bindingResult.getAllErrors().get(0).getDefaultMessage(), null);
        }

        if (houseForm.getPhotos() == null || houseForm.getCover() == null) {
            return R.error(HttpStatus.BAD_REQUEST.value(), "必须上传图片");
        }

        Map<SupportAddress.Level, SupportAddressDTO> addressMap = addressService.findCityAndRegion(houseForm.getCityEnName(), houseForm.getRegionEnName());
        if (addressMap.keySet().size() != 2) {
            return R.error(R.StatusEnum.NOT_VALID_PARAM);
        }

        ResultVo<HouseDTO> result = houseService.save(houseForm);
        if (result.isSuccess()) {
            return R.ok(result.getResult());
        }

        return R.ok(R.StatusEnum.NOT_VALID_PARAM);
    }

    /**
     * 房源列表页
     */
    @PostMapping("admin/houses")
    @ResponseBody
    public ApiDataTableResponse houses(@ModelAttribute DatatableSearchForm searchBody) {
        MultiResultVo<HouseDTO> result = houseService.adminQuery(searchBody);

        ApiDataTableResponse response = new ApiDataTableResponse(R.StatusEnum.SUCCESS);
        response.setRecordsFiltered(result.getTotal())
                .setRecordsTotal(result.getTotal())
                .setDraw(searchBody.getDraw())
                .setData(result.getResult());
        return response;
    }

    /**
     * 房源信息编辑页面的查询
     */
    @GetMapping("admin/house/edit")
    public String houseEditPage(@RequestParam(value = "id") Integer id, Model model) {
        if (id == null || id < 1) {
            return "404";
        }
        // 查询house信息
        ResultVo<HouseDTO> houseVo = houseService.findCompleteOne(id);
        if (!houseVo.isSuccess()) {
            return "404";
        }
        HouseDTO result = houseVo.getResult();
        model.addAttribute("house", result);
        // 地址信息
        Map<SupportAddress.Level, SupportAddressDTO> addressMap = addressService.findCityAndRegion(result.getCityEnName(), result.getRegionEnName());
        model.addAttribute("city", addressMap.get(SupportAddress.Level.CITY));
        model.addAttribute("region", addressMap.get(SupportAddress.Level.REGION));
        // 地铁信息
        HouseDetailDTO detailDTO = result.getHouseDetail();
        ResultVo<SubwayDTO> subwayServiceResult = addressService.findSubway(detailDTO.getSubwayLineId());
        if (subwayServiceResult.isSuccess()) {
            model.addAttribute("subway", subwayServiceResult.getResult());
        }
        // 地铁站信息
        ResultVo<SubwayStationDTO> subwayStationVo = addressService.findSubwayStation(detailDTO.getSubwayStationId());
        if (subwayStationVo.isSuccess()) {
            model.addAttribute("station", subwayStationVo.getResult());
        }
        return "admin/house-edit";
    }

    /**
     * 房源信息编辑保存
     */
    @PostMapping("admin/house/edit")
    @ResponseBody
    public R saveHouse(@Valid @ModelAttribute("form-house-edit") HouseForm houseForm, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return new R(HttpStatus.BAD_REQUEST.value(), bindingResult.getAllErrors().get(0).getDefaultMessage(), null);
        }
        Map<SupportAddress.Level, SupportAddressDTO> addressMap = addressService.findCityAndRegion(houseForm.getCityEnName(), houseForm.getRegionEnName());
        if (addressMap.keySet().size() != 2) {
            return R.ok(R.StatusEnum.NOT_VALID_PARAM);
        }
        ResultVo result = houseService.update(houseForm);
        if (result.isSuccess()) {
            return R.ok(null);
        }
        R response = R.ok(R.StatusEnum.BAD_REQUEST);
        response.setMessage(result.getMessage());
        return response;
    }

    /**
     * 修改封面接口
     */
    @PostMapping("admin/house/cover")
    @ResponseBody
    public R updateCover(@RequestParam(value = "cover_id") Integer coverId,
                         @RequestParam(value = "target_id") Integer targetId) {
        ResultVo result = this.houseService.updateCover(coverId, targetId);
        if (result.isSuccess()) {
            return R.ok(R.StatusEnum.SUCCESS);
        } else {
            return R.ok(HttpStatus.BAD_REQUEST.value(), result.getMessage());
        }
    }

    /**
     * 审核接口
     */
    @PutMapping("admin/house/operate/{id}/{operation}")
    @ResponseBody
    public R operateHouse(@PathVariable(value = "id") Integer id,
                          @PathVariable(value = "operation") int operation) {
        if (id <= 0) {
            return R.ok(R.StatusEnum.NOT_VALID_PARAM);
        }
        ResultVo result;

        switch (operation) {
            case HouseOperation.PASS:
                result = houseService.updateStatus(id, HouseStatus.PASSES.getValue());
                break;
            case HouseOperation.PULL_OUT:
                result = houseService.updateStatus(id, HouseStatus.NOT_AUDITED.getValue());
                break;
            case HouseOperation.DELETE:
                result = houseService.updateStatus(id, HouseStatus.DELETED.getValue());
                break;
            case HouseOperation.RENT:
                result = houseService.updateStatus(id, HouseStatus.RENTED.getValue());
                break;
            default:
                return R.ok(R.StatusEnum.BAD_REQUEST);
        }

        if (result.isSuccess()) {
            return R.ok(null);
        }
        return R.ok(HttpStatus.BAD_REQUEST.value(),
                result.getMessage());
    }


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

}
