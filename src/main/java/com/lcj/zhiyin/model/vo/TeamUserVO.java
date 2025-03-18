package com.lcj.zhiyin.model.vo;

import com.lcj.zhiyin.model.domain.User;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 队伍和用户信息封装类（脱敏）
 */
@Data
public class TeamUserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1899063007109226944L;

    /**
     * id
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建人用户信息
     */
    private User createUser;

    /**
     * 队伍已加入的用户数
     */
    private Integer hasJoinNum;

    /**
     * 该用户是否已加入队伍
     */
    private boolean hasJoin = false;
}
