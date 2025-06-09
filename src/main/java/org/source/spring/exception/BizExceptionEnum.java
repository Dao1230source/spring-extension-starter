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
    DOC_DATA_CLASS_NOT_DEFINED("Doc Data Class Not Defined"),
    ;

    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }

}
