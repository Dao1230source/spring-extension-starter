package org.source.spring.expression;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * springboot EL 表达式解析
 * <pre>
 *     静态方法调用："T(class reference path).methodName(parameters)"
 *     例如："T(com.sf.opc.inventory.utils.RedisKeyUtil).getLockKeyDemandId(#operateParam.getDemandId())"
 *     #operateParam 表示方法签名列表中名为“operateParam”的参数
 * </pre>
 *
 * @author zengfugen
 */
@UtilityClass
@Slf4j
public class SpElUtil {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    /**
     * <pre>
     *  maven编译时默认 '-debug' 参数，LocalVariableTableParameterNameDiscoverer 生效，但是该类将被弃用。
     *  应使用StandardReflectionParameterNameDiscoverer，但编译时需要添加 '-parameters' 参数
     *  即maven配置
     *  {@code
     *  <plugin>
     *      <groupId>org.apache.maven.plugins</groupId>
     *      <artifactId>maven-compiler-plugin</artifactId>
     *      <configuration>
     *          <parameters>true</parameters>
     *      </configuration>
     *  </plugin>
     *  }
     *
     * </pre>
     */
    private static final DefaultParameterNameDiscoverer DISCOVERER = new DefaultParameterNameDiscoverer();

    public static <R> @Nullable R parse(String spEl, Class<R> resultClass, Consumer<StandardEvaluationContext> prepareAvailable) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        prepareAvailable.accept(context);
        Expression expression = PARSER.parseExpression(spEl);
        R r = expression.getValue(context, resultClass);
        log.debug("result:{}", r);
        return r;
    }

    public static <T> @Nullable T parseSpEl(String keySpEl, Object value, Class<T> resultClass) {
        return parse(keySpEl, resultClass, context -> context.setVariable(VariableConstants.RESULT, value));
    }

    public static String[] getParameterNames(Method method) {
        return DISCOVERER.getParameterNames(method);
    }

}