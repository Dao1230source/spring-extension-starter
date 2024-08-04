package org.source.spring.log.handler;

import org.aopalliance.intercept.MethodInvocation;
import org.jetbrains.annotations.NotNull;
import org.source.spring.exception.BizExceptionEnum;
import org.source.spring.expression.ExtendEvaluationContext;
import org.source.spring.expression.ExtendRootObject;
import org.source.spring.expression.VariableConstants;
import org.source.spring.log.*;
import org.source.spring.log.annotation.Log;
import org.source.spring.trace.TraceContext;
import org.source.spring.utility.SystemUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.*;

@ConditionalOnProperty(prefix = "org.source.spring.enabled", name = "log", havingValue = "true", matchIfMissing = true)
@AutoConfiguration
public class LogHandler extends LogAnnotationHandler<Log, LogHandler> {

    @Override
    public Log obtainAnnotation(MethodInvocation invocation) {
        return invocation.getMethod().getAnnotation(Log.class);
    }

    @Override
    public void before(MethodDetail<Log> detail) {
        Logs.putLog();
    }


    @Override
    public void finals(MethodDetail<Log> detail) {
        Logs.save(resolve(detail, LOG_EVALUATOR));
        Logs.removeLog();
    }

    @Override
    public boolean matches(@NotNull Method method, @NotNull Class<?> targetClass) {
        return method.isAnnotationPresent(Log.class);
    }

    @Override
    public LogHandler getProcessor() {
        return this;
    }

    @Override
    protected int order() {
        return 3;
    }

    public static List<LogData> resolve(MethodDetail<Log> detail, LogExpressionEvaluator logEvaluator) {
        LogData logData = detail.getLogData();
        Log logAnno = detail.getAnnotation();
        ExtendEvaluationContext<ExtendRootObject> context = detail.getEvaluationContext();
        syncFromContext(logData);
        syncCommonDataFromAnnotation(logData, logAnno, logEvaluator, context);
        LogData.setIfAbsent(logData, LogData::getUserId, LogData::setUserId, TraceContext::getUserId);
        Object param = logData.getParam();
        Object result = logData.getResult();
        context.setParam(param);
        context.setResult(result);
        if (param instanceof Collection<?> ps) {
            List<LogData> logDataList = new ArrayList<>(ps.size());
            Iterator<?> pIterator = ps.iterator();
            Iterator<?> rIterator = null;
            // 需要解析方法结果
            if (needParseResult(logAnno)) {
                BizExceptionEnum.LOG_RESULT_MUST_BE_COLLECTION.isTrue(result instanceof Collection<?>);
                // 消除警告
                assert result instanceof Collection<?>;
                Collection<?> rs = (Collection<?>) result;
                BizExceptionEnum.LOG_METHOD_PARAM_RESULT_MUST_EQUAL_SIZE.isTrue(ps.size() == rs.size(),
                        "param size:{}, result size:{}", ps.size(), rs.size());
                rIterator = rs.iterator();
            }
            int idx = 0;
            while (pIterator.hasNext()) {
                Object p = pIterator.next();
                Object r = null;
                if (Objects.nonNull(rIterator)) {
                    r = rIterator.next();
                }
                LogData copy = LogDataMapper.INSTANCE.copy(logData);
                copy.setParam(p);
                copy.setResult(r);
                syncMethodDataFromAnnotationIterator(copy, logAnno, logEvaluator, context, idx);
                logDataList.add(copy);
                idx += 1;
            }
            return logDataList;
        } else {
            LogData copy = LogDataMapper.INSTANCE.copy(logData);
            syncMethodDataFromAnnotation(copy, logAnno, logEvaluator, context);
            return List.of(copy);
        }
    }

    public static void syncFromContext(LogData logData) {
        logData.setLogId(Logs.getLogId());
        logData.setParentLogId(Logs.getParentLogId());
        logData.setRefId(Logs.getRefId());
        logData.setTitle(Logs.getTitle());
        logData.setDesc(Logs.getDesc());
        // 辅助数据
        logData.setUserId(Logs.getUserId());
        logData.setSystemType(Logs.getSystemType());
        logData.setBizType(Logs.getBizType());
        logData.setApplicationName(SystemUtil.getApplicationName());
        // 方法数据
        logData.setParam(Logs.getParam());
        logData.setResult(Logs.getResult());
        logData.setExtra(Logs.getExtra());
        logData.setMethodLocation(Logs.getMethodLocation());
    }

    public static void syncCommonDataFromAnnotation(LogData logData, Log logAnno,
                                                    LogExpressionEvaluator logEvaluator,
                                                    ExtendEvaluationContext<ExtendRootObject> context) {
        LogData.setIfAbsent(logData, LogData::getParam, LogData::setParam,
                () -> logEvaluator.parse(context, logAnno.param(), Object.class));
        LogData.setIfAbsent(logData, LogData::getResult, LogData::setResult,
                () -> logEvaluator.parse(context, logAnno.result(), Object.class));
        LogData.setIfAbsent(logData, LogData::getMethodLocation, LogData::setMethodLocation,
                () -> logEvaluator.parse(context, logAnno.methodLocation(), String.class));
        LogData.setIfAbsent(logData, LogData::getExtra, LogData::setExtra,
                () -> logEvaluator.parse(context, logAnno.extra(), Object.class));
        LogData.setIfAbsent(logData, LogData::getUserId, LogData::setUserId,
                () -> logEvaluator.parse(context, logAnno.userId(), String.class));
    }

    public static void syncMethodDataFromAnnotation(LogData logData, Log logAnno,
                                                    LogExpressionEvaluator logEvaluator,
                                                    ExtendEvaluationContext<ExtendRootObject> context) {
        LogData.setIfAbsent(logData, LogData::getLogId, LogData::setLogId,
                () -> logEvaluator.parse(context, logAnno.logId(), String.class));
        LogData.setIfAbsent(logData, LogData::getParentLogId, LogData::setParentLogId,
                () -> logEvaluator.parse(context, logAnno.parentLogId(), String.class));
        LogData.setIfAbsent(logData, LogData::getRefId, LogData::setRefId,
                () -> logEvaluator.parse(context, logAnno.refId(), String.class));
        LogData.setIfAbsent(logData, LogData::getTitle, LogData::setTitle,
                () -> logEvaluator.parse(context, logAnno.title(), String.class));
        LogData.setIfAbsent(logData, LogData::getDesc, LogData::setDesc,
                () -> logEvaluator.parse(context, logAnno.desc(), String.class));
    }

    public static boolean needParseResult(Log logAnno) {
        return logAnno.logId().contains(VariableConstants.RESULT_SP_EL)
                || (StringUtils.hasText(logAnno.parentLogId()) && logAnno.parentLogId().contains(VariableConstants.RESULT_SP_EL))
                || (StringUtils.hasText(logAnno.refId()) && logAnno.refId().contains(VariableConstants.RESULT_SP_EL))
                || logAnno.title().contains(VariableConstants.RESULT_SP_EL)
                || (StringUtils.hasText(logAnno.desc()) && logAnno.desc().contains(VariableConstants.RESULT_SP_EL))
                ;
    }

    public static void syncMethodDataFromAnnotationIterator(LogData logData, Log logAnno,
                                                            LogExpressionEvaluator logEvaluator,
                                                            ExtendEvaluationContext<ExtendRootObject> context,
                                                            final int idx) {
        LogData.setIfAbsent(logData, LogData::getLogId, LogData::setLogId,
                () -> logEvaluator.parse(context, logEvaluator.replacePlaceHolder(logAnno.logId(), idx), String.class));
        LogData.setIfAbsent(logData, LogData::getParentLogId, LogData::setParentLogId,
                () -> logEvaluator.parse(context, logEvaluator.replacePlaceHolder(logAnno.parentLogId(), idx), String.class));
        LogData.setIfAbsent(logData, LogData::getRefId, LogData::setRefId,
                () -> logEvaluator.parse(context, logEvaluator.replacePlaceHolder(logAnno.refId(), idx), String.class));
        LogData.setIfAbsent(logData, LogData::getTitle, LogData::setTitle,
                () -> logEvaluator.parse(context, logEvaluator.replacePlaceHolder(logAnno.title(), idx), String.class));
        LogData.setIfAbsent(logData, LogData::getDesc, LogData::setDesc,
                () -> logEvaluator.parse(context, logEvaluator.replacePlaceHolder(logAnno.desc(), idx), String.class));
    }
}
