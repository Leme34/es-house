package com.lsd.eshouse.controller;

import com.lsd.eshouse.common.dto.UserDTO;
import com.lsd.eshouse.common.vo.R;
import com.lsd.eshouse.common.vo.ResultVo;
import com.lsd.eshouse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by lsd
 * 2020-01-23 17:14
 */
@Controller
public class AdminController {

    @Autowired
    private UserService userService;

    @GetMapping("/admin/login")
    public String adminLogin() {
        return "admin/login";
    }

    @GetMapping("/admin/center")
    public String adminCenterPage() {
        return "admin/center";
    }

    @GetMapping("/admin/welcome")
    public String welcomePage() {
        return "admin/welcome";
    }

    /**
     * 房源列表页
     */
    @GetMapping("admin/house/list")
    public String houseListPage() {
        return "admin/house-list";
    }

    /**
     * 新增房源功能页
     */
    @GetMapping("admin/add/house")
    public String addHousePage() {
        return "admin/house-add";
    }

    /**
     * 管理员用户信息
     */
    @GetMapping("admin/user/{userId}")
    @ResponseBody
    public R getUserInfo(@PathVariable(value = "userId") Integer userId) {
        if (userId == null || userId < 1) {
            return R.ok(R.StatusEnum.BAD_REQUEST);
        }
        ResultVo<UserDTO> serviceResult = userService.findById(userId);
        if (!serviceResult.isSuccess()) {
            return R.ok(R.StatusEnum.NOT_FOUND);
        } else {
            return R.ok(serviceResult.getResult());
        }
    }

}
