package org.source.spring.log.processor;

import org.aopalliance.intercept.MethodInvocation;
import org.source.spring.common.exception.SpExtExceptionEnum;
import org.source.spring.common.spel.ExtendEvaluationContext;
import org.source.spring.common.spel.ExtendRootObject;
import org.source.spring.common.spel.VariableConstants;
import org.source.spring.common.utility.SystemUtil;
import org.source.spring.log.LogData;
import org.source.spring.log.LogDataMapper;
import org.source.spring.log.LogExpressionEvaluator;
import org.source.spring.log.Logs;
import org.source.spring.log.annotation.Log;
import org.source.spring.trace.TraceContext;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.*;

public class LogLogAnnoProcessor extends AbstractLogAnnotationProcessor<Log, LogLogAnnoProcessor> {

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
    public boolean matches(Method method, Class<?> targetClass) {
        return method.isAnnotationPresent(Log.class);
    }

    @Override
    public int order() {
        return 3;
    }

    public static List<LogData> resolve(MethodDetail<Log> detail, LogExpressionEvaluator logEvaluator) {
        LogData logData = detail.getLogData();
        Log logAnno = detail.getAnnotation();
        ExtendEvaluationContext<ExtendRootObject> context = detail.getEvaluationContext();
        syncFromContext(logData);
        syncCommonDataFromAnnotation(logData, logAnno, logEvaluator, context);
        Object param = logData.getParam();
        Object result = logData.getResult();
        context.setParam(param);
        context.setResult(result);
        context.setResult(logData.getExtra());
        if (param instanceof Collection<?> ps) {
            List<LogData> logDataList = new ArrayList<>(ps.size());
            Iterator<?> pIterator = ps.iterator();
            Iterator<?> rIterator = null;
            // 需要解析方法结果
            if (needParseResult(logAnno)) {
                SpExtExceptionEnum.LOG_RESULT_MUST_BE_COLLECTION.isTrue(result instanceof Collection<?>);
                // 消除警告
                assert result instanceof Collection<?>;
                Collection<?> rs = (Collection<?>) result;
                SpExtExceptionEnum.LOG_METHOD_PARAM_RESULT_MUST_EQUAL_SIZE.isTrue(ps.size() == rs.size(),
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
                syncBizDataFromAnnotationIterator(copy, logAnno, logEvaluator, context, idx);
                logDataList.add(copy);
                idx += 1;
            }
            return logDataList;
        } else {
            LogData copy = LogDataMapper.INSTANCE.copy(logData);
            syncBizDataFromAnnotation(copy, logAnno, logEvaluator, context);
            return List.of(copy);
        }
    }

    /**
     * 首先从 LogContext 上下文中获取数据，上下文数据一般时手动设置的，优先级最高，非空时，才会赋值到logData中
     *
     * @param logData 日志数据
     */
    public static void syncFromContext(LogData logData) {
        logData.setNotNull(LogData::setLogId, Logs.getLogId());
        logData.setNotNull(LogData::setParentLogId, Logs.getParentLogId());
        logData.setNotNull(LogData::setRefId, Logs.getRefId());
        logData.setNotNull(LogData::setTitle, Logs.getTitle());
        logData.setNotNull(LogData::setDesc, Logs.getDesc());
        // 辅助数据
        logData.setNotNull(LogData::setUserId, Logs.getUserId());
        logData.setNotNull(LogData::setSystemType, Logs.getSystemType());
        logData.setNotNull(LogData::setBizType, Logs.getBizType());
        logData.setNotNull(LogData::setApplicationName, SystemUtil.getApplicationName());
        // 方法数据
        logData.setNotNull(LogData::setParam, Logs.getParam());
        logData.setNotNull(LogData::setResult, Logs.getResult());
        logData.setNotNull(LogData::setExtra, Logs.getExtra());
        logData.setNotNull(LogData::setMethodLocation, Logs.getMethodLocation());
    }

    /**
     * 从 Log 注解中获取数据，优先级低于 LogContext 上下文数据，只有 LogData 中值不为空时才赋值
     * <p>
     * 这里先处理通用数据，方法数据+用户userId
     *
     * @param logData 日志数据
     */
    public static void syncCommonDataFromAnnotation(LogData logData, Log logAnno,
                                                    LogExpressionEvaluator logEvaluator,
                                                    ExtendEvaluationContext<ExtendRootObject> context) {
        logData.setIfAbsent(LogData::getParam, LogData::setParam,
                () -> logEvaluator.parse(context, logAnno.param(), Object.class));
        logData.setIfAbsent(LogData::getResult, LogData::setResult,
                () -> logEvaluator.parse(context, logAnno.result(), Object.class));
        logData.setIfAbsent(LogData::getMethodLocation, LogData::setMethodLocation,
                () -> logEvaluator.parse(context, logAnno.methodLocation(), String.class));
        logData.setIfAbsent(LogData::getExtra, LogData::setExtra,
                () -> logEvaluator.parse(context, logAnno.extra(), Object.class));
        logData.setIfAbsent(LogData::getUserId, LogData::setUserId,
                () -> logEvaluator.parse(context, logAnno.userId(), String.class));
        logData.setIfAbsent(LogData::getUserId, LogData::setUserId, TraceContext::getUserId);
    }

    public static boolean needParseResult(Log logAnno) {
        return logAnno.logId().contains(VariableConstants.RESULT_SP_EL)
                || (StringUtils.hasText(logAnno.parentLogId()) && logAnno.parentLogId().contains(VariableConstants.RESULT_SP_EL))
                || (StringUtils.hasText(logAnno.refId()) && logAnno.refId().contains(VariableConstants.RESULT_SP_EL))
                || logAnno.title().contains(VariableConstants.RESULT_SP_EL)
                || (StringUtils.hasText(logAnno.desc()) && logAnno.desc().contains(VariableConstants.RESULT_SP_EL))
                ;
    }

    /**
     * 从 Log 注解中获取数据，优先级低于 LogContext 上下文数据，只有 LogData 中值不为空时才赋值
     * <p>
     * 这里处理业务数据
     *
     * @param logData 日志数据
     */
    public static void syncBizDataFromAnnotation(LogData logData, Log logAnno,
                                                 LogExpressionEvaluator logEvaluator,
                                                 ExtendEvaluationContext<ExtendRootObject> context) {
        logData.setIfAbsent(LogData::getLogId, LogData::setLogId,
                () -> logEvaluator.parse(context, logAnno.logId(), String.class));
        logData.setIfAbsent(LogData::getParentLogId, LogData::setParentLogId,
                () -> logEvaluator.parse(context, logAnno.parentLogId(), String.class));
        logData.setIfAbsent(LogData::getRefId, LogData::setRefId,
                () -> logEvaluator.parse(context, logAnno.refId(), String.class));
        logData.setIfAbsent(LogData::getTitle, LogData::setTitle,
                () -> logEvaluator.parse(context, logAnno.title(), String.class));
        logData.setIfAbsent(LogData::getDesc, LogData::setDesc,
                () -> logEvaluator.parse(context, logAnno.desc(), String.class));
    }

    public static void syncBizDataFromAnnotationIterator(LogData logData, Log logAnno,
                                                         LogExpressionEvaluator logEvaluator,
                                                         ExtendEvaluationContext<ExtendRootObject> context,
                                                         final int idx) {
        logData.setIfAbsent(LogData::getLogId, LogData::setLogId,
                () -> logEvaluator.parse(context, logEvaluator.replacePlaceholder(logAnno.logId(), idx), String.class));
        logData.setIfAbsent(LogData::getParentLogId, LogData::setParentLogId,
                () -> logEvaluator.parse(context, logEvaluator.replacePlaceholder(logAnno.parentLogId(), idx), String.class));
        logData.setIfAbsent(LogData::getRefId, LogData::setRefId,
                () -> logEvaluator.parse(context, logEvaluator.replacePlaceholder(logAnno.refId(), idx), String.class));
        logData.setIfAbsent(LogData::getTitle, LogData::setTitle,
                () -> logEvaluator.parse(context, logEvaluator.replacePlaceholder(logAnno.title(), idx), String.class));
        logData.setIfAbsent(LogData::getDesc, LogData::setDesc,
                () -> logEvaluator.parse(context, logEvaluator.replacePlaceholder(logAnno.desc(), idx), String.class));
    }
}