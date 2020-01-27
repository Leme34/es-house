package com.lsd.eshouse.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Created by lsd
 * 2020-01-24 14:15
 */
@Controller
public class UserController {

    @GetMapping("/user/login")
    public String loginPage() {
        return "user/login";
    }


    @GetMapping("/user/center")
    public String centerPage() {
        return "user/center";
    }

}
