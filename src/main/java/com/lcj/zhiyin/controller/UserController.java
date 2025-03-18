package com.lcj.zhiyin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lcj.zhiyin.common.BaseResponse;
import com.lcj.zhiyin.common.ErrorCode;
import com.lcj.zhiyin.common.ResultUtils;
import com.lcj.zhiyin.common.response.LoginResponseData;
import com.lcj.zhiyin.exception.BusinessException;
import com.lcj.zhiyin.model.domain.User;
import com.lcj.zhiyin.model.request.UserLoginRequest;
import com.lcj.zhiyin.model.request.UserRegisterRequest;
import com.lcj.zhiyin.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/user")
@CrossOrigin(origins = {"http://localhost:3000"})
@Slf4j
@Validated
@Tag(name="User控制器")
public class UserController {

    @Resource
    private UserService userService;

    @Operation(summary = "用户注册请求")
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody @Validated UserRegisterRequest request) {
        long userId = userService.userRegister(
                request.getUserAccount(),
                request.getUserPassword(),
                request.getCheckPassword()
        );
        return ResultUtils.success(userId);
    }

    @Operation(summary = "用户登录请求")
    @PostMapping("/login")
    public BaseResponse<LoginResponseData> userLogin(@RequestBody @Validated UserLoginRequest request) {
        LoginResponseData responseData = userService.userLogin(request.getUserAccount(), request.getUserPassword());
        return ResultUtils.success(responseData);
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public BaseResponse<?> userLogout(@RequestHeader("Authorization") String token) {
        // 调用业务层处理注销逻辑
        userService.userLogout(token);
        return ResultUtils.success("注销成功");
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        String userAccount = auth.getName();
        User user = userService.getUserByUserAccount(userAccount);
        return ResultUtils.success(user);
    }

    @Operation(summary = "搜索用户(用户名)", description = "模糊查询")
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(@RequestParam @NotBlank String username) {
        List<User> userList = userService.searchUsersByUsername(username);
        return ResultUtils.success(userList);
    }

    @Operation(summary = "搜索用户(标签)", description = "")
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam List<String> tagNameList) {
        List<User> userList = userService.searchUsersByTags(tagNameList);
        return ResultUtils.success(userList);
    }

    /**
     * 获取推荐用户（通过缓存）//todo: 这只是分页查询？
     */
    @Cacheable(value = "recommend_users", key = "#pageNum + '-' + #pageSize")
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(@RequestParam long pageSize, @RequestParam long pageNum) {
        Page<User> userPage = userService.page(new Page<>(pageNum, pageSize));
        return ResultUtils.success(userPage);
    }

    @Operation(summary = "更新用户信息", description = "ADMIN")
    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody @Validated User user) {
        return ResultUtils.success(userService.updateUser(user));
    }

    @Operation(summary = "删除用户", description = "ADMIN")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<Boolean> deleteUser(@PathVariable long id) {
        log.info("用户 {} 删除成功",id);
        return ResultUtils.success(userService.removeById(id));
    }

    @Operation(summary = "根据标签返回最匹配的用户")
    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(@RequestParam @Min(1) @Max(20) long num) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User loginUser = userService.getUserByUserAccount(auth.getName());
        return ResultUtils.success(userService.matchUsers(num, loginUser));
    }
}
