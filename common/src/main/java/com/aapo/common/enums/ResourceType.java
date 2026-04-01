package com.aapo.common.enums;

import com.aapo.common.exception.BadRequestException;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 用户状态
 */
@Getter
public enum ResourceType {
    ARTICLE(0, "文章"),
    ;
    @EnumValue
    int value;
    String desc;

    ResourceType(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static ResourceType of(int value) {
        if (value == 0) {
            return ARTICLE;
        }
        throw new BadRequestException("资源类型错误");
    }
}