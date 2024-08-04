package org.source.spring.log.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataSourceLog {
    /**
     * 是否启用
     *
     * @return bool
     */
    boolean enabled() default true;

    /**
     * 作为key的字段，表的列名称
     *
     * @return columns
     */
    String[] keyColumns();

    /**
     * 父ID
     * <br/>
     * 当该日志是某条日志的子项时填写该值，如订单明细（orderItem）之于订单(order)，parentId = orderId
     */
    String parentLogId() default "";

    /**
     * 描述
     */
    String desc() default "";

    /**
     * 关联ID
     * <br>
     * 将多条日志关联为一个整体的ID，比如订单分为多个步骤：加购物车、提交、支付、卖家发货、签收等等步骤，
     * 每一个步骤都有自己的id，但它们的refId都是orderId
     */
    String refId() default "";

    /**
     * 不记录表
     *
     * @return tableNames
     */
    String[] excludeTableNames() default {};

    /**
     * 不记录列
     *
     * @return columnNames
     */
    String[] excludeColumns() default {};
}
