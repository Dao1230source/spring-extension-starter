package org.source.spring.log.annotation;

import org.source.spring.common.spel.ExtendRootObject;
import org.source.spring.log.datasource.DataSourceLogData;
import org.source.spring.log.enums.LogBizTypeEnum;
import org.source.spring.log.enums.LogSystemTypeEnum;
import org.source.spring.uid.UidPrefix;
import org.source.spring.uid.Uids;

import java.lang.annotation.*;

/**
 * <pre>
 * DataSourceLog是Log的变种
 * 其中spEl上下文对象{@link ExtendRootObject}中的几个值默认如下：
 * param = null
 * result ={@literal List<DataSourceLogData>}，详情见 {@link DataSourceLogData}
 * extra = null
 *
 * logId ={@link Uids#stringId(UidPrefix)}
 * bizId = 可指定，或为空时默认 logId
 * </pre>
 */
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
     * 关联ID
     * <br>
     * 将多条日志关联为一个整体的ID，比如订单分为多个步骤：加购物车、提交、支付、卖家发货、签收等等步骤，
     * 每一个步骤都有自己的id，但它们的refId都是orderId
     */
    String refBizId() default "";

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
    LogSystemTypeEnum systemType() default LogSystemTypeEnum.DB;

    /**
     * 实际业务层级的分类 {@link LogBizTypeEnum}
     */
    LogBizTypeEnum bizType() default LogBizTypeEnum.USER;

    /**
     * 作为key的字段，表的列名称
     *
     * @return columns
     */
    String[] keyColumns();

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