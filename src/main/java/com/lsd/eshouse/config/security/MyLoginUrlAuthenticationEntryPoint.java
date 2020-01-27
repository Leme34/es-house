package com.lsd.eshouse.config.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * 覆盖默认的登录URL认证入口点，实现用户认证失败时，根据不同的请求前缀跳转到对应的登录页面URL
 * <p>
 * 当访问普通用户相关页面而登录认证失败时，跳转"/user/login"普通用户登录页面
 * 当访问管理员用户相关页面而登录认证失败时，跳转"/admin/login"普通用户登录页面
 * <p>
 * Created by lsd
 * 2020-01-24 15:17
 */
public class MyLoginUrlAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

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
}
