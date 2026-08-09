package org.source.spring.object.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.source.utility.exception.BaseException;
import org.source.utility.exception.EnumProcessor;

@Getter
@AllArgsConstructor
public enum ObjectExceptionEnum implements EnumProcessor<BaseException> {
    /**
     * object
     */
    SAVE_ERROR("AbstractObjectProcessor.merge() error"),
    DATA_CLASS_NOT_DEFINED("object value class not defined"),
    TYPE_NOT_DEFINED("object type not defined"),
    OBJECT_CANNOT_FIND_VALUE("cannot find value by id"),
    OBJECT_NEW_OBJECT_ENTITY_NONNULL("新建 Object 实体类的方法返回值必须不为空"),
    OBJECT_NEW_OBJECT_BODY_ENTITY_NONNULL("新建 ObjectBody 实体类方法的返回值必须不为空"),
    OBJECT_NEW_RELATION_ENTITY_NONNULL("新建 Relation 实体类方法的返回值必须不为空"),
    RELATE_FAILED("关联对象失败");

    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }

}
