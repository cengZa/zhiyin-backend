package com.lcj.zhiyin.service;

import com.lcj.zhiyin.common.response.LoginResponseData;
import com.lcj.zhiyin.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户服务
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录，返回脱敏后的用户信息
     */
    LoginResponseData userLogin(String userAccount, String userPassword);

    /**
     * 脱敏用户信息
     */
    User getSafetyUser(User originUser);

    /**
     * 用户注销
     */
    void userLogout(String token);

    /**
     * 根据用户名搜索用户（仅管理员使用）
     */
    List<User> searchUsersByUsername(String username);

    /**
     * 根据标签搜索用户
     */
    List<User> searchUsersByTags(List<String> tagNameList);

    /**
     * 更新用户信息
     */
    int updateUser(User user);


    /**
     * 根据用户账号获取用户（用于 Spring Security 获取当前用户）
     */
    User getUserByUserAccount(String userAccount);

    /**
     * 获取当前登录的用户
     */
    User getLoginUser();

    Map<Long, User> getUsersByIds(Set<Long> userIds);

//    /**
//     * 判断是否为管理员（基于 Authentication）
//     */
//    boolean isAdmin(Authentication authentication);
//
//    /**
//     * 判断是否为管理员（基于用户信息）
//     */
//    boolean isAdmin(User loginUser);

    /**
     * 匹配用户
     */
    List<User> matchUsers(long num, User loginUser);
}
