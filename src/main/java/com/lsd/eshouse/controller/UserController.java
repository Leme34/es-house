package com.lsd.eshouse.controller;

import com.lsd.eshouse.common.utils.LoginUserUtil;
import com.lsd.eshouse.common.vo.R;
import com.lsd.eshouse.common.vo.ResultVo;
import com.lsd.eshouse.service.UserService;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by lsd
 * 2020-01-24 14:15
 */
@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/user/login")
    public String loginPage() {
        return "user/login";
    }


    @GetMapping("/user/center")
    public String centerPage() {
        return "user/center";
    }


    /**
     * 修改用户信息
     * @param profile
     * @param value
     * @return
     */
    @PostMapping(value = "api/user/info")
    @ResponseBody
    public R updateUserInfo(@RequestParam(value = "profile") String profile,
                            @RequestParam(value = "value") String value) {
        if (value.isEmpty()) {
            return R.ok(R.StatusEnum.BAD_REQUEST);
        }
        if ("email".equals(profile) && !LoginUserUtil.checkEmail(value)) {
            return R.ok(HttpStatus.SC_BAD_REQUEST, "不支持的邮箱格式");
        }
        ResultVo result = userService.modifyUserProfile(profile, value);
        if (result.isSuccess()) {
            return R.ok("");
        } else {
            return R.ok(HttpStatus.SC_BAD_REQUEST, result.getMessage());
        }
    }


}
