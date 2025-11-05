package org.source.spring.log.processor;

import lombok.Data;
import org.source.spring.log.enums.LogBizTypeEnum;
import org.source.spring.log.enums.LogSystemTypeEnum;

@Data
public class LogAnnoData {
    /**
     * bizId
     */
    private String bizId;

    /**
     * 父ID
     * <br/>
     * 当该日志是某条日志的子项时填写该值，如订单明细（orderItem）之于订单(order)，parentId = orderId
     */
    private String parentBizId;

    /**
     * 关联ID
     * <br>
     * 将多条日志关联为一个整体的ID，比如订单分为多个步骤：加购物车、提交、支付、卖家发货、签收等等步骤，
     * 每一个步骤都有自己的id，但它们的refId都是orderId
     */
    private String refBizId;

    /**
     * 标题
     */
    private String title;

    /**
     * 描述
     */
    private String desc;

    /**
     * 系统代码层次的分类 {@link LogSystemTypeEnum}
     */
    private LogSystemTypeEnum systemType;

    /**
     * 实际业务层级的分类 {@link LogBizTypeEnum}
     */
    private LogBizTypeEnum bizType;

    /**
     * ID
     */
    private String logId;

    /**
     * 默认登录用户
     *
     */
    private String userId;

    /**
     * 方法定位
     */
    private String methodLocation;
    /**
     * 请求数据，默认方法的第一个参数
     */
    private String param;
    /**
     * 返回数据，默认方法的返回值
     */
    private String result;
    /**
     * 扩展数据
     */
    private String extra;
}