package com.lcj.zhiyin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户注册请求体
 */
@Data
public class UserRegisterRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 3191241716373120793L;

    @NotBlank(message = "账号不能为空")
    @Schema(description = "用户账号")
    @Size(min = 1, max = 20, message = "账号必须在1到20个字符之间")
    private String userAccount;

    @Schema(description = "用户名称")
    private String userName;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "用户密码")
    @Size(min = 4, max = 32, message = "密码必须在4到32个字符之间")
    private String userPassword;

    @NotBlank(message = "校验密码不能为空")
    @Schema(description = "用户校验密码")
    @Size(min = 4, max = 32, message = "校验密码必须在4到32个字符之间")
    private String checkPassword;

}
