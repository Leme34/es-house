package com.lsd.eshouse.config.security;

import com.lsd.eshouse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring Security配置类
 * <p>
 * Created by lsd
 * 2020-01-23 12:11
 */
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true) //启用@prePostEnabled注解
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserService userService;
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 资源访问权限
        http.authorizeRequests()
                .antMatchers("/static/**").permitAll()          // 静态资源
                .antMatchers("/admin/login").permitAll()        // 管理员登录入口
                .antMatchers("/user/login").permitAll()         // 用户登录入口
                .antMatchers("/admin/**").hasRole("ADMIN")
                .antMatchers("/user/**").hasAnyRole("ADMIN", "USER")
                .antMatchers("/api/user/**").hasAnyRole("ADMIN", "USER")
                .and()
                .formLogin()
                .loginProcessingUrl("/login") // 配置角色登录处理入口
                .failureHandler(authenticationFailureHandler())
                .and()
                .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/logout/page")
                .deleteCookies("JSESSIONID").invalidateHttpSession(true)      //登出时删除cookie的JSESSIONID，并使session失效
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(loginUrlAuthenticationEntryPoint()) //认证失败后，根据不同的请求前缀跳转到不同的登录页面
                .accessDeniedPage("/403");

        http.csrf().disable();
        // iframe 同源允许访问
        http.headers().frameOptions().sameOrigin();
    }

    //用户认证
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        //设置认证提供者
        auth.authenticationProvider(authenticationProvider());
        //自定义的UserDetailsService提供自定义用户信息获取服务
        auth.userDetailsService(userService).passwordEncoder(passwordEncoder);
    }

    /**
     * AuthenticationProvider 提供用户UserDetails的具体验证方式，
     * 在其中可以自定义用户密码的加密、验证方式
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        //通过dao层提供验证
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userService);
        // 设置密码加密方式,用于登录时的密码比对
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return authenticationProvider;
    }


    @Bean
    public MyLoginUrlAuthenticationEntryPoint loginUrlAuthenticationEntryPoint() {
        // 默认跳转普通用户登录页面
        return new MyLoginUrlAuthenticationEntryPoint("/user/login");
    }

    @Bean
    public MyAuthenticationFailureHandler authenticationFailureHandler() {
        return new MyAuthenticationFailureHandler();
    }

}
