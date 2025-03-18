package com.lcj.zhiyin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lcj.zhiyin.common.response.LoginResponseData;
import com.lcj.zhiyin.exception.BusinessException;
import com.lcj.zhiyin.common.ErrorCode;
import com.lcj.zhiyin.model.domain.User;
import com.lcj.zhiyin.service.UserService;
import com.lcj.zhiyin.mapper.UserMapper;
import com.lcj.zhiyin.utils.AlgorithmUtils;
import com.lcj.zhiyin.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private UserMapper userMapper;

    private BCryptPasswordEncoder passwordEncoder;

    private StringRedisTemplate redisTemplate;

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {

        // 检查特殊字符（可根据需要保留）
        String validPattern = "[`~!@#$%^&*()+=|{}':;,\\\\.<>/?！￥…（）—【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号包含特殊字符");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不一致");
        }

        // 检查账号是否重复
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserAccount, userAccount);
        if (userMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 使用 BCrypt 加密密码
        String encryptedPassword = passwordEncoder.encode(userPassword);
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptedPassword);
        userMapper.insert(user);
        log.info("用户注册成功, User={}",user);
        return user.getId();
    }

    @Override
    public LoginResponseData userLogin(String userAccount, String userPassword) {

        // 检查特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;,\\\\.<>/?！￥…（）—【】‘；：”“’。，、？]";
        if (Pattern.compile(validPattern).matcher(userAccount).find()) {
            log.warn("登录失败： 账号包含特殊字符, userAccount={}", userAccount);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号包含特殊字符");
        }
        // 根据 userAccount 查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserAccount, userAccount);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            log.warn("登录失败：用户不存在, userAccount={}", userAccount);
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        // 使用 BCrypt 验证密码
        if (!passwordEncoder.matches(userPassword, user.getUserPassword())) {
            log.warn("登录失败：密码错误, userPassword={}", userPassword);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }

        String roleString = (user.getUserRole() == 1) ? "ADMIN" : "USER";

        String token = JwtUtil.generateToken(userAccount, roleString);

        String redisKey = "jwt:token:" + token;

        redisTemplate.opsForValue().set(redisKey, userAccount, 1, TimeUnit.DAYS);
        Object value = redisTemplate.opsForValue().get(redisKey);
        if(value != null) log.info("存入Redis成功 value= {}", value);
        else log.warn("存入Redis失败!");

        log.info("用户 {} 登陆成功,本次登录数据库中的User => {}",userAccount,user);

        return new LoginResponseData(getSafetyUser(user), token);
    }

    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        BeanUtils.copyProperties(originUser, safetyUser, "userPassword","createTime","updateTime","isDelete","avatarUrl");
        log.info("响应脱敏后的safeUser=> {}",safetyUser);
        return safetyUser;
    }

    @Override
    public void userLogout(String token) {

        String redisKey = "jwt:token:" + token;
        String redisValue = redisTemplate.opsForValue().get(redisKey);
        Boolean deleted = redisTemplate.delete(redisKey);
        if(deleted==null || !deleted){
            log.warn("登出失败， Token未在Redis中找到");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登出失败");
        }
        log.info("用户 {} 登出成功, Token已删除, token={}", redisValue, token);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> searchUsersByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(User::getUsername, username);
        return userMapper.selectList(queryWrapper);
    }

    @Override
    public List<User> searchUsersByTags(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 构造 SQL 条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        StringBuilder condition = new StringBuilder();
        condition.append("(");
        for (int i = 0; i < tagNameList.size(); i++) {
            if(i>0) condition.append(" AND ");
            condition.append("JSON_CONTAINS(tags, '\"").append(tagNameList.get(i)).append("\"')");
        }
        condition.append(")");
        queryWrapper.apply(condition.toString());
        List<User> userList = userMapper.selectList(queryWrapper);
        return userList.stream().map(this::getSafetyUser).toList();
    }



    // 依赖 @PreAuthorize 完成权限校验，省去手动验证
    @PreAuthorize("hasRole('ADMIN') or #user.userAccount == principal")
    public int updateUser(User user) {

        User oldUser = userMapper.selectById(user.getId());
        if (oldUser == null) {
            log.warn("更新时: 用户不存在");
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return userMapper.updateById(user);
    }

    @Override
    public User getUserByUserAccount(String userAccount) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserAccount, userAccount);
        return userMapper.selectOne(queryWrapper);
    }

    @Override
    public User getLoginUser(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !auth.isAuthenticated() || auth.getPrincipal() == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        // ? : return this.getUserByUserAccount()有什么区别
        return getUserByUserAccount(auth.getName());
    }

    @Override
    public Map<Long, User> getUsersByIds(Set<Long> userIds){
        if(userIds == null || userIds.isEmpty()){
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(User::getId, userIds);
        List<User> userList = userMapper.selectList(queryWrapper);
        return userList.stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }

    @Override
    public List<User> matchUsers(long num, User loginUser) {
        // 查询所有状态为正常且 tags 不为空的用户，仅查询 id 和 tags 字段
        LambdaQueryWrapper<User> lambdaQuery = new LambdaQueryWrapper<>();
        lambdaQuery.select(User::getId, User::getTags)
                .isNotNull(User::getTags)
                .eq(User::getUserStatus, 0);
        List<User> userList = this.list(lambdaQuery);

        // 计算每个用户与当前用户标签之间的编辑距离，过滤掉自己或标签为空的用户
        List<Pair<User, Long>> pairList = userList.stream()
                .filter(user -> ( user.getId() != loginUser.getId() ) && !CollectionUtils.isEmpty(user.getTags()) )
                .map(user -> {
                    List<String> userTagList = user.getTags();
                    long distance = AlgorithmUtils.minDistance(loginUser.getTags(), userTagList);
                    return Pair.of(user, distance);
                })
                .toList();

        // 按编辑距离升序排序，并取前 num 个用户
        List<Pair<User, Long>> topPairs = pairList.stream()
                .sorted(Comparator.comparingLong(Pair::getRight))
                .limit(num)
                .toList();

        // 提取匹配的用户 ID 列表
        List<Long> userIdList = topPairs.stream()
                .map(pair -> pair.getLeft().getId())
                .collect(Collectors.toList());

        // 根据 userId 查询完整用户信息，并构建一个 id -> 安全用户 的映射
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", userIdList);
        Map<Long, User> idUserMap = this.list(userQueryWrapper)
                .stream()
                .collect(Collectors.toMap(User::getId, this::getSafetyUser));

        // 按 userIdList 顺序构造最终返回的用户列表
        return userIdList.stream()
                .map(idUserMap::get)
                .collect(Collectors.toList());
    }

}
