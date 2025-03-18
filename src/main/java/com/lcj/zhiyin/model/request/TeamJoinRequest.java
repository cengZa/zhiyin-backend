package com.lcj.zhiyin.model.request;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户加入队伍请求体
 */
@Data
public class TeamJoinRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 3191241716373120793L;

    @NotNull(message = "队伍ID不能为空")
    private Long teamId;

    // 如果队伍为加密状态，密码需要传入；此处不做强制校验，业务逻辑中做判断
    private String password;
}
