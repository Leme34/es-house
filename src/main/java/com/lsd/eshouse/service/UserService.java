package com.lsd.eshouse.service;

import com.lsd.eshouse.common.dto.UserDTO;
import com.lsd.eshouse.common.vo.ResultVo;
import com.lsd.eshouse.entity.Role;
import com.lsd.eshouse.entity.User;
import com.lsd.eshouse.repository.RoleRepository;
import com.lsd.eshouse.repository.UserRepository;
import org.modelmapper.ModelMapper;
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
    @Autowired
    private ModelMapper modelMapper;

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

    public ResultVo<UserDTO> findById(Integer userId) {
        final User user = userRepository.findById(userId).get();
        if (user == null) {
            return ResultVo.notFound();
        }
        final UserDTO userDTO = modelMapper.map(user, UserDTO.class);
        return ResultVo.of(userDTO);
    }
}
