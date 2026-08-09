package org.source.spring.object.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 关联操作
 */
@AllArgsConstructor
@Getter
public enum RelateTypeEnum {
    /**
     * 新增
     */
    ADD(1, "新增"),
    /**
     * 删除
     */
    DELETE(0, "删除");
    private final Integer type;
    private final String desc;
}