package com.lsd.eshouse.common.advice;

import com.lsd.eshouse.common.vo.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;

/**
 * 错误页面 和 Json 响应 的全局处理 Controller
 * <p>
 * Created by lsd
 * 2020-01-23 11:09
 */
@Controller
public class AppErrorController implements ErrorController {

    private static final String ERROR_PATH = "/error";
    @Autowired
    private ErrorAttributes errorAttributes;

    /**
     * the path of the error page
     */
    @Override
    public String getErrorPath() {
        return ERROR_PATH;
    }

    /**
     * 响应跳转错误页面
     */
    @RequestMapping(value = ERROR_PATH, produces = MediaType.TEXT_HTML_VALUE)
    public String errorPageHandler(HttpServletRequest req, HttpServletResponse res) {
        // 根据不同响应状态，跳转对应错误页面
        switch (res.getStatus()) {
            case 403:
                return "403";
            case 404:
                return "404";
            case 500:
                return "500";
        }
        // 其他错误返回主页
        return "index";
    }


    @RequestMapping(value = ERROR_PATH)
    @ResponseBody
    public R errorApiHandler(HttpServletRequest req) {
        WebRequest webRequest = new ServletWebRequest(req);
        // 获取经过DefaultErrorAttributes处理后的tomcat返回的错误信息Map，但不需要包含错误堆栈信息
        Map<String, Object> errorAttributes = this.errorAttributes.getErrorAttributes(webRequest, false);
        int statusCode = (int) Optional.ofNullable(req.getAttribute("javax.servlet.error.status_code"))
                .orElse(500);
        // 获取错误信息Map中的message字段
        String errorMessage = errorAttributes.getOrDefault("message", "服务器内部错误").toString();
        return R.error(statusCode, errorMessage);
    }

}
