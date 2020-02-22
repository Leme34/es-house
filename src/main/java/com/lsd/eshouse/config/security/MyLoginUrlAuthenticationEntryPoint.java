package com.lsd.eshouse.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsd.eshouse.common.vo.R;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 覆盖默认的登录URL认证入口点，实现用户认证/权限校验失败时，根据不同的请求前缀跳转到对应的登录页面URL
 * <p>
 * 当访问普通用户相关页面而登录认证失败时，跳转"/user/login"普通用户登录页面
 * 当访问管理员用户相关页面而登录认证失败时，跳转"/admin/login"普通用户登录页面
 * <p>
 * Created by lsd
 * 2020-01-24 15:17
 */
public class MyLoginUrlAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {
    @Autowired
    private ObjectMapper objectMapper;

    // Map<请求路径正则, 登录认证页面的URL>
    private static final Map<String, String> urlEntryPointMap = new HashMap<>();

    private static final PathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 构造方法
     *
     * @param loginFormUrl URL where the login page can be found. Should either be
     *                     relative to the web-app context path (include a leading {@code /}) or an absolute
     *                     URL.
     */
    public MyLoginUrlAuthenticationEntryPoint(String loginFormUrl) {
        // 赋值默认的登录页面URL
        super(loginFormUrl);
        // 普通用户登录页面URL映射
        urlEntryPointMap.put("/user/**", "/user/login");
        // 管理员用户登录页面URL映射
        urlEntryPointMap.put("/admin/**", "/admin/login");
    }

    /**
     * 根据当前请求路径决定对应的登录页面URL
     *
     * @return 该请求应该跳转到哪里进行登录认证
     */
    @Override
    protected String determineUrlToUseForThisRequest(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        // 正则匹配（去除上下文路径后的）请求路径
        final String path = StringUtils.replace(request.getRequestURI(), request.getContextPath(), "");
        for (Map.Entry<String, String> entry : urlEntryPointMap.entrySet()) {
            if (pathMatcher.match(entry.getKey(), path)) {
                return entry.getValue();
            }
        }
        // 匹配不到则按照原security逻辑处理跳转默认登录url
        return super.determineUrlToUseForThisRequest(request, response, exception);
    }


    /**
     * 执行重定向（或跳转）处理
     * 若Ajax请求则返回Json，否则跳转到 this.determineUrlToUseForThisRequest() 返回的登录表单URL
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        // 根据路径判断是否Api（Ajax）请求
        String uri = request.getRequestURI();
        if (StringUtils.startsWith(uri, "/api")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            ResponseEntity<R> responseEntity = ResponseEntity.ok(R.error(HttpStatus.SC_FORBIDDEN, "没有权限访问"));
            PrintWriter pw = response.getWriter();
            pw.write(objectMapper.writeValueAsString(responseEntity));
            pw.close();
        } else {  //否则跳转到 this.determineUrlToUseForThisRequest() 返回的登录表单URL
            super.commence(request, response, authException);
        }
    }
}
