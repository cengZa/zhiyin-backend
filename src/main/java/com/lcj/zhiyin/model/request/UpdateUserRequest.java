package com.lcj.zhiyin.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * todo: 本来想着管理员修改用户信息应该需要DTO封装但是好像也没必要？  该类暂时没用到
 */
@Data
public class UpdateUserRequest implements Serializable {

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String avatarUrl;
    private String phone;
    private String email;
    // 其他允许更新的字段……
}
