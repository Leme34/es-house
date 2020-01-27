package com.lsd.eshouse.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Created by lsd
 * 2020-01-24 11:49
 */
@Controller
public class BaseController {

    @GetMapping(value = {"/","/index"})
    public String index() {
        return "index";
    }

    @GetMapping("/logout/page")
    public String logoutPage() {
        return "logout";
    }

    @GetMapping("/404")
    public String notfoundPage() {
        return "404";
    }

    @GetMapping("/403")
    public String accessErrorPage() {
        return "403";
    }

    @GetMapping("/500")
    public String internalErrorPage() {
        return "500";
    }

}
