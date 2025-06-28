package org.source.spring.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.source.spring.i18n.annotation.I18nDict;
import org.source.spring.i18n.annotation.I18nRef;
import org.source.spring.i18n.enums.I18nRefTypeEnum;
import org.source.utility.exceptions.EnumProcessor;

@Getter
@AllArgsConstructor
@I18nDict(value = @I18nRef(type = I18nRefTypeEnum.FIELD, value = "message"))
public enum BizExceptionEnum implements EnumProcessor<BizException> {

    /**
     * runtime
     */
    RUNTIME_EXCEPTION("服务异常"),
    THROW_EXCEPTION("系统异常"),

    /**
     * validate
     */
    PARAM_EXCEPTION("请求参数校验异常"),
    ENUM_EXCEPTION("枚举值校验异常"),

    /**
     * utility
     */
    NOT_EXISTS_DELETE_COLUMN("不存在@LogicDelete注解的列"),
    /**
     * http request
     */
    INVALID_RPC_REQUEST("无效的RPC请求"),

    /**
     * log
     */
    LOG_RESULT_MUST_BE_COLLECTION("方法的结果必须是集合类型"),
    LOG_METHOD_PARAM_RESULT_MUST_EQUAL_SIZE("方法的参数和结果集合大小必须相等"),

    /**
     *
     */
    OBJECT_VALUE_CLASS_NOT_DEFINED("object value class not defined"),
    OBJECT_TYPE_NOT_DEFINED("object type not defined"),
    OBJECT_CANNOT_FIND_VALUE("cannot find value by id"),
    OBJECT_NEW_OBJECT_ENTITY_NONNULL("新建 Object 实体类的方法返回值必须不为空"),
    OBJECT_NEW_OBJECT_BODY_ENTITY_NONNULL("新建 ObjectBody 实体类方法的返回值必须不为空"),
    OBJECT_NEW_RELATION_ENTITY_NONNULL("新建 Relation 实体类方法的返回值必须不为空"),
    ;

    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }

}
