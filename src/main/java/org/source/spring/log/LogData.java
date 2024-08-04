package org.source.spring.log;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.source.spring.log.enums.LogBizTypeEnum;
import org.source.spring.log.enums.LogSystemTypeEnum;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class LogData {
    /*
    基础数据
     */
    /**
     * ID
     * <br>
     */
    @NotEmpty(message = "日志ID(logId)不能为空")
    private String logId;
    /**
     * 父ID
     * <br/>
     * 当该日志是某条日志的子项时填写该值，如订单明细（orderItem）之于订单(order)，parentId = orderId
     */
    private String parentLogId;
    /**
     * 关联ID
     * <br>
     * 将多条日志关联为一个整体的ID，比如订单分为多个步骤：加购物车、提交、支付、卖家发货、签收等等步骤，
     * 每一个步骤都有自己的id，但它们的refId都是orderId
     */
    private String refId;
    /**
     * 标题
     */
    @NotEmpty(message = "标题(title)不能为空")
    private String title;
    /**
     * 描述
     */
    private String desc;
    /*
    辅助数据
     */
    /**
     * 默认登录用户
     */
    private String userId;
    /**
     * 系统代码层次的分类 {@link LogSystemTypeEnum}
     */
    private Integer systemType;
    /**
     * 实际业务层级的分类 {@link LogBizTypeEnum}
     */
    private Integer bizType;
    /**
     * 应用名称
     */
    private String applicationName;
    /*
     方法数据，和方法有关的数据，辅助获得基础数据
     */
    /**
     * 请求数据
     */
    private Object param;
    /**
     * 返回数据
     */
    private Object result;
    /*
    扩展数据
     */
    /**
     * 扩展数据
     */
    private Object extra;
    /**
     * 定位记录日志的方法
     */
    private String methodLocation;
    /*
    异常数据，自动赋值
     */
    /**
     * 异常信息
     */
    private String exceptionMessage;
    /*
    统计数据，自动赋值
     */
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    public static <T> void setIfAbsent(LogData logData,
                                       Function<LogData, T> getter,
                                       BiConsumer<LogData, T> setter,
                                       Supplier<T> supplier) {
        if (Objects.isNull(getter.apply(logData))) {
            setter.accept(logData, supplier.get());
        }
    }

}
