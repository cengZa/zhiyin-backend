package com.lcj.zhiyin.model.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 创建队伍请求体
 */
@Data
public class TeamCreateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 3191241716373120793L;

    @NotBlank(message = "队伍名称不能为空")
    @Size(max = 20, message = "队伍名称不能超过20字符")
    private String name;

    @Size(max = 512, message = "描述长度不能超过512字符")
    private String description;

    @NotNull(message = "最大人数不能为空")
    @Min(value = 1, message = "队伍人数不能少于1人")
    @Max(value = 20, message = "队伍人数不能超过20人")
    private Integer maxNum;

    @Future(message = "超时时间必须大于当前时间")
    private Date expireTime;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    @NotNull(message = "队伍状态不能为空")
    private Integer status;

    @Size(max = 32, message = "密码长度不能超过32字符")
    private String password;
}
