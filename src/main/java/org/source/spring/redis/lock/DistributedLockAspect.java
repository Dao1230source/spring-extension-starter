package org.source.spring.redis.lock;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.source.spring.expression.SpElUtil;
import org.source.utility.utils.Strings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Method;
import java.util.*;

/**
 * <pre>
 * 使用注解来处理分布式锁主要原因有两点：
 * 1、对整个方法加锁时，方便使用
 * 2、当方法上有数据库事物注解（@Transactional）时，
 * 如果在方法内加锁，代码执行：释放锁->执行事物，此时分布式事物不安全，可能造成“超卖现象”
 * 使用注解加锁，代码执行：执行事物->释放锁，分布式事物安全
 * </pre>
 */
@Slf4j
@AllArgsConstructor
@Aspect
@ConditionalOnBean(RedisTemplate.class)
@ConditionalOnProperty(prefix = "org.source.spring.enabled", name = "lock", havingValue = "true", matchIfMissing = true)
@AutoConfiguration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class DistributedLockAspect {

    private final RedissonClient redissonClient;

    @Around(value = "@annotation(distributedLock)")
    public Object inventoryLock(ProceedingJoinPoint point, DistributedLock distributedLock) throws Throwable {
        String name = point.getSignature().toShortString();
        log.debug("为该方法[{}]添加分布式锁, distributedLock:{}", name, distributedLock);
        RLock rLock = getRedissonLock(point, distributedLock);
        if (Objects.isNull(rLock)) {
            return point.proceed();
        }
        // 获取到锁
        rLock.lock();
        try {
            return point.proceed();
        } finally {
            rLock.unlock();
        }
    }

    public String getParameterName(String spEl) {
        int start = spEl.indexOf("#");
        if (start > 0) {
            int end = spEl.indexOf(".", start);
            return spEl.substring(start + 1, end);
        }
        return null;
    }

    public RLock getRedissonLock(ProceedingJoinPoint point, DistributedLock distributedLock) {
        String key = distributedLock.key();
        log.debug("key spEl: {}", key);
        String parameterName = getParameterName(key);
        // 没有获取到参数占位符，表明不是 spEl 表达式
        if (StringUtils.isEmpty(parameterName)) {
            log.error("spEl 表达式中没有参数占位符,parameterName:{}", parameterName);
            throw new IllegalArgumentException(Strings.format("spEl 表达式中没有参数占位符,parameterName:{}", parameterName));
        }
        Object value;
        DistributedLock.ValueParser valueParser = distributedLock.valueParser();
        if (StringUtils.isNotEmpty(valueParser.expression())) {
            value = parse(point, valueParser.expression(), valueParser.type());
        } else {
            Method method = ((MethodSignature) point.getSignature()).getMethod();
            String[] parameterNames = SpElUtil.getParameterNames(method);
            List<String> paramterNameList = Arrays.asList(parameterNames);
            int index = paramterNameList.indexOf(parameterName);
            // method中不存在名为 ${parameterName} 的参数
            if (index < 0) {
                log.error("method 签名列表:{} 中没有该名称:{} 的参数", paramterNameList, parameterName);
                throw new IllegalArgumentException(Strings.format("method 签名列表:{} 中没有该名称:{} 的参数", paramterNameList, parameterName));
            }
            // 方法参数名称和参数值是一一对应的，这里不需要判断索引
            value = point.getArgs()[index];
        }
        if (Objects.isNull(value)) {
            log.error("待解析的对象值为空，parameterName：{}", parameterName);
            throw new IllegalArgumentException(Strings.format("待解析的对象值为空，parameterName：{}", parameterName));
        }
        if (value instanceof Collection<?> params) {
            log.debug("批量加锁，size：{}", params.size());
            RLock[] locks = params.stream()
                    .map(k -> redissonClient.getLock(parse(parameterName, k, key)))
                    .toArray(RLock[]::new);
            return redissonClient.getMultiLock(locks);
        }
        return redissonClient.getLock(parse(parameterName, value, key));
    }

    private static String parse(String parameterName, Object value, String spEl) {
        return SpElUtil.parse(spEl, String.class, context -> context.setVariable(parameterName, value));
    }

    private static <R> R parse(ProceedingJoinPoint point, String spEl, Class<R> resultClass) {
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        String[] parameterNames = SpElUtil.getParameterNames(method);
        Object[] args = point.getArgs();
        Map<String, Object> parameterMap = HashMap.newHashMap(16);
        for (int i = 0; i < parameterNames.length; i++) {
            parameterMap.put(parameterNames[i], args[i]);
        }
        return SpElUtil.parse(spEl, resultClass, context -> context.setVariables(parameterMap));
    }
}
