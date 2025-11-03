package org.source.spring.log.annotation;

import org.source.spring.common.spel.VariableConstants;
import org.source.spring.log.enums.LogBizTypeEnum;
import org.source.spring.log.enums.LogSystemTypeEnum;

import java.lang.annotation.*;

/**
* 所有的String都是spEl表达式
*/
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {
    /**
     * ID
     */
    String logId();

    /**
     * 父ID
     * <br/>
     * 当该日志是某条日志的子项时填写该值，如订单明细（orderItem）之于订单(order)，parentId = orderId
     */
    String parentLogId() default "";

    /**
     * 关联ID
     * <br>
     * 将多条日志关联为一个整体的ID，比如订单分为多个步骤：加购物车、提交、支付、卖家发货、签收等等步骤，
     * 每一个步骤都有自己的id，但它们的refId都是orderId
     */
    String refId() default "";

    /**
     * 标题
     */
    String title();

    /**
     * 描述
     */
    String desc() default "";

    /**
     * 系统代码层次的分类 {@link LogSystemTypeEnum}
     */
    LogSystemTypeEnum systemType() default LogSystemTypeEnum.DEFAULT;

    /**
     * 实际业务层级的分类 {@link LogBizTypeEnum}
     */
    LogBizTypeEnum bizType() default LogBizTypeEnum.DEFAULT;

    /**
     * 默认登录用户
     *
     * @return userId
     */
    String userId() default "";

    /**
     * 请求数据，默认方法的第一个参数
     */
    String param() default "#args[0]";

    /**
     * 返回数据，默认方法的返回值
     */
    String result() default VariableConstants.METHOD_RESULT_SP_EL;

    /**
     * 方法定位
     */
    String methodLocation() default VariableConstants.METHOD_LOCATION_SP_EL;

    /**
     * 扩展数据
     */
    String extra() default VariableConstants.EXTRA_SP_EL;
}