package org.source.spring.log.processor;

import lombok.Data;
import lombok.Getter;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.source.spring.common.exception.SpExtExceptionEnum;
import org.source.spring.common.spel.ExtendEvaluationContext;
import org.source.spring.common.spel.ExtendRootObject;
import org.source.spring.common.utility.SystemUtil;
import org.source.spring.log.LogData;
import org.source.spring.log.LogDataMapper;
import org.source.spring.log.LogExpressionEvaluator;
import org.source.spring.log.Logs;
import org.source.spring.log.enums.LogScopeEnum;
import org.source.spring.trace.TraceContext;
import org.source.spring.uid.UidPrefix;
import org.source.spring.uid.Uids;
import org.source.utility.utils.Streams;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.UnaryOperator;

@Getter
public abstract class AbstractLogAnnotationProcessor<A extends Annotation> implements Serializable {
    protected static final LogExpressionEvaluator LOG_EVALUATOR = new LogExpressionEvaluator();

    @Data
    public static class MethodDetail<A> {
        protected MethodInvocation invocation;
        protected LogData logData;
        protected ExtendEvaluationContext<ExtendRootObject> evaluationContext;
        protected A annotation;
    }

    public MethodDetail<A> load(MethodInvocation invocation) {
        MethodDetail<A> detail = new MethodDetail<>();
        detail.invocation = invocation;
        detail.logData = new LogData();
        detail.evaluationContext = LOG_EVALUATOR.createContext(invocation);
        detail.annotation = this.obtainAnnotation(invocation);
        return detail;
    }

    public abstract boolean matches(Method method, Class<?> targetClass);

    public abstract LogScopeEnum getLogScope();

    public @Nullable UidPrefix getUidPrefix() {
        return null;
    }

    @Nullable
    public A obtainAnnotation(MethodInvocation invocation) {
        return null;
    }

    @Nullable
    public LogAnnoData obtainLogAnnoData(MethodDetail<A> detail) {
        return null;
    }

    public void before(MethodDetail<A> detail) {
    }

    public void after(MethodDetail<A> detail) {
    }

    public void exception(MethodDetail<A> detail) {
    }

    public void finals(List<LogData> logData) {
    }

    public void doBefore(MethodDetail<A> detail) {
        detail.logData.setStartTime(LocalDateTime.now());
        Logs.init(this.getLogScope());
        this.before(detail);
    }

    public void doAfter(MethodDetail<A> detail, @Nullable Object result) {
        detail.logData.setEndTime(LocalDateTime.now());
        detail.evaluationContext.setMethodResult(result);
        this.after(detail);
    }

    public void doException(MethodDetail<A> detail, Exception ex) {
        String stackTrace = ExceptionUtils.getStackTrace(ex);
        String exceptionMessage = stackTrace.substring(0, Math.min(1000, stackTrace.length()));
        detail.logData.setExceptionMessage(exceptionMessage);
        this.exception(detail);
    }

    public void doFinal(MethodDetail<A> detail) {
        List<LogData> logData = this.process(detail);
        this.finals(logData);
        Logs.remove(this.getLogScope());
    }

    public int order() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    protected List<LogData> process(MethodDetail<A> detail) {
        LogAnnoData logAnno = this.obtainLogAnnoData(detail);
        if (Objects.isNull(logAnno)) {
            return List.of();
        }
        LogExpressionEvaluator logEvaluator = LOG_EVALUATOR;
        LogData logData = detail.getLogData();
        ExtendEvaluationContext<ExtendRootObject> context = detail.getEvaluationContext();
        LogScopeEnum logScope = this.getLogScope();
        this.syncFromContext(logData, logScope);
        this.syncOnceData(logData, logAnno, logEvaluator, context);
        Object param = logData.getParam();
        Object result = logData.getResult();
        context.setParam(param);
        context.setResult(result);
        context.setExtra(logData.getExtra());
        boolean paramIsCollection = Objects.nonNull(param) && param instanceof Collection<?>;
        boolean resultIsCollection = Objects.nonNull(result) && result instanceof Collection<?>;
        // 单条
        if (!paramIsCollection && !resultIsCollection) {
            LogData copy = LogDataMapper.INSTANCE.copy(logData);
            this.syncIteratorData(copy, logAnno, logEvaluator, context, UnaryOperator.identity());
            return List.of(copy);
        }
        // 批量
        Collection<?> ps = null;
        Collection<?> rs = null;
        if (paramIsCollection) {
            ps = ((Collection<?>) param);
        }
        if (resultIsCollection) {
            rs = ((Collection<?>) result);
        }
        if (Objects.isNull(ps)) {
            ps = Streams.of(rs).map(r -> param).toList();
        }
        if (Objects.isNull(rs)) {
            rs = Streams.of(ps).map(r -> param).toList();
        }
        SpExtExceptionEnum.LOG_METHOD_PARAM_RESULT_MUST_EQUAL_SIZE.isTrue(ps.size() == rs.size(),
                "param size:{}, result size:{}", ps.size(), rs.size());
        List<LogData> logDataList = new ArrayList<>(ps.size());
        Iterator<?> pIterator = ps.iterator();
        Iterator<?> rIterator = rs.iterator();
        int idx = 0;
        while (pIterator.hasNext()) {
            Object p = pIterator.next();
            Object r = rIterator.next();
            LogData copy = LogDataMapper.INSTANCE.copy(logData);
            copy.setParam(p);
            copy.setResult(r);
            final int i = idx;
            this.syncIteratorData(copy, logAnno, logEvaluator, context, s -> logEvaluator.replacePlaceholder(s, i));
            logDataList.add(copy);
            idx++;
        }
        return logDataList;
    }

    /**
     * 首先从 LogContext 上下文中获取数据，上下文数据一般是手动设置的，优先级最高，非空时，才会赋值到logData中
     *
     * @param logData 日志数据
     */
    protected void syncFromContext(LogData logData, LogScopeEnum scope) {
        logData.setNotNull(LogData::setBizId, Logs.getBizId(scope));
        logData.setNotNull(LogData::setParentBizId, Logs.getParentBizId(scope));
        logData.setNotNull(LogData::setRefBizId, Logs.getRefBizId(scope));
        logData.setNotNull(LogData::setTitle, Logs.getTitle(scope));
        logData.setNotNull(LogData::setDesc, Logs.getDesc(scope));
        // 辅助数据
        logData.setNotNull(LogData::setLogId, Logs.getLogId(scope));
        logData.setNotNull(LogData::setUserId, Logs.getUserId(scope));
        logData.setNotNull(LogData::setSystemType, Logs.getSystemType(scope));
        logData.setNotNull(LogData::setBizType, Logs.getBizType(scope));
        logData.setNotNull(LogData::setApplicationName, SystemUtil.getApplicationName());
        // 方法数据
        logData.setNotNull(LogData::setParam, Logs.getParam(scope));
        logData.setNotNull(LogData::setResult, Logs.getResult(scope));
        logData.setNotNull(LogData::setExtra, Logs.getExtra(scope));
        logData.setNotNull(LogData::setMethodLocation, Logs.getMethodLocation(scope));
    }

    /**
     * 只需一次计算的数据
     *
     * @param logData 日志数据
     */
    protected void syncOnceData(LogData logData, LogAnnoData logAnno,
                                LogExpressionEvaluator logEvaluator,
                                ExtendEvaluationContext<ExtendRootObject> context) {
        logData.setIfAbsent(LogData::getParam, LogData::setParam,
                () -> logEvaluator.parse(context, logAnno.getParam(), Object.class));
        logData.setIfAbsent(LogData::getResult, LogData::setResult,
                () -> logEvaluator.parse(context, logAnno.getResult(), Object.class));
        logData.setIfAbsent(LogData::getMethodLocation, LogData::setMethodLocation,
                () -> logEvaluator.parse(context, logAnno.getMethodLocation(), String.class));
        logData.setIfAbsent(LogData::getExtra, LogData::setExtra,
                () -> logEvaluator.parse(context, logAnno.getExtra(), Object.class));
        logData.setIfAbsent(LogData::getUserId, LogData::setUserId,
                () -> logEvaluator.parse(context, logAnno.getUserId(), String.class));
        logData.setIfAbsent(LogData::getUserId, LogData::setUserId, TraceContext::getUserIdOrDefault);
    }

    /**
     * 迭代中需多次计算的数据
     *
     * @param logData 日志数据
     */
    protected void syncIteratorData(LogData logData, LogAnnoData logAnno,
                                    LogExpressionEvaluator logEvaluator,
                                    ExtendEvaluationContext<ExtendRootObject> context,
                                    UnaryOperator<String> spElHandler) {
        logData.setIfAbsent(LogData::getBizId, LogData::setBizId,
                () -> logEvaluator.parse(context, spElHandler.apply(logAnno.getBizId()), String.class));
        logData.setIfAbsent(LogData::getParentBizId, LogData::setParentBizId,
                () -> logEvaluator.parse(context, spElHandler.apply(logAnno.getParentBizId()), String.class));
        logData.setIfAbsent(LogData::getRefBizId, LogData::setRefBizId,
                () -> logEvaluator.parse(context, spElHandler.apply(logAnno.getRefBizId()), String.class));
        logData.setIfAbsent(LogData::getTitle, LogData::setTitle,
                () -> logEvaluator.parse(context, spElHandler.apply(logAnno.getTitle()), String.class));
        logData.setIfAbsent(LogData::getDesc, LogData::setDesc,
                () -> logEvaluator.parse(context, spElHandler.apply(logAnno.getDesc()), String.class));
        logData.setIfAbsent(LogData::getLogId, LogData::setLogId,
                () -> logEvaluator.parse(context, spElHandler.apply(logAnno.getLogId()), String.class));
        // logId 为空默认 uid
        logData.setIfAbsent(LogData::getLogId, LogData::setLogId, () -> Uids.stringId(this.getUidPrefix()));
        // bizId 为空默认 logId
        logData.setIfAbsent(LogData::getBizId, LogData::setBizId, logData::getLogId);
    }
}