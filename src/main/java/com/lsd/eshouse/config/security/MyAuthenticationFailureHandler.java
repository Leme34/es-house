package com.lsd.eshouse.config.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录验证失败处理器
 * <p>
 * Created by lsd
 * 2020-01-25 12:41
 */
public class MyAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    private MyLoginUrlAuthenticationEntryPoint myLoginUrlAuthenticationEntryPoint;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        // 应该跳去哪里登录认证
        String loginUrl = myLoginUrlAuthenticationEntryPoint.determineUrlToUseForThisRequest(request, response, exception);
        // 简单的把异常信息放在查询串中
        loginUrl += "?" + exception.getMessage();
        // 设置跳转的登录认证URL
        setDefaultFailureUrl(loginUrl);
        // 走父类的逻辑进行跳转
        super.onAuthenticationFailure(request, response, exception);
    }

}
