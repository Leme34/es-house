package com.lsd.eshouse.service;

import com.lsd.eshouse.entity.Role;
import com.lsd.eshouse.entity.User;
import com.lsd.eshouse.repository.RoleRepository;
import com.lsd.eshouse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by lsd
 * 2020-01-23 20:50
 */
@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        final User user = userRepository.findByName(username)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("authError"));
        // 查询权限
        final List<Role> roleList = roleRepository.findRolesByUserId(user.getId());
        if (CollectionUtils.isEmpty(roleList)) {
            throw new DisabledException("权限不足");
    }
        // 设置security的权限集合
        final List<GrantedAuthority> simpleAuthorities = roleList.stream()
                .map(role ->
                        new SimpleGrantedAuthority("ROLE_" + role.getName())
                ).collect(Collectors.toList());
        user.setAuthorities(simpleAuthorities);
        return user;
    }

}
