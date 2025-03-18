package com.lcj.zhiyin.model.enums;

import lombok.Getter;
import lombok.Setter;

/**
 * 队伍状态枚举
 */
@Getter
public enum TeamStatusEnum {

    PUBLIC(0, "公开"),
    PRIVATE(1, "私有"),
    SECRET(2, "加密");

    private final int value;

    private final String text;

    public static TeamStatusEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (TeamStatusEnum teamStatusEnum : values()) {
            if (teamStatusEnum.getValue() == value) {
                return teamStatusEnum;
            }
        }
        return null;
    }

    TeamStatusEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

}
