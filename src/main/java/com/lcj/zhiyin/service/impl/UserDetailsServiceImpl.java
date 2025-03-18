package com.lcj.zhiyin.service.impl;

import com.lcj.zhiyin.exception.BusinessException;
import com.lcj.zhiyin.common.ErrorCode;
import com.lcj.zhiyin.model.domain.User;
import com.lcj.zhiyin.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);
    @Resource
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String userAccount) throws UsernameNotFoundException {
        User user = userService.getUserByUserAccount(userAccount);
        log.info("loadUserByUserName => {}", user);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUserAccount())
                .password(user.getUserPassword())  // 数据库存储的加密密码
                .roles(user.getUserRole() == 1 ? "ADMIN" : "USER")  // 角色权限
                .build();
    }
}
