package org.source.spring.request;

import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.source.spring.properties.SecurityProperties;
import org.source.spring.trace.TraceContext;
import org.source.utility.enums.BaseExceptionEnum;

import java.io.IOException;

public class RequestInterceptor implements Interceptor {
    private final SecurityProperties securityProperties;

    public RequestInterceptor(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
        okhttp3.Request original = chain.request();
        BaseExceptionEnum.NOT_NULL.nonNull(securityProperties.getSecretKey(),
                "org.source.common.secretKey must be configured");
        okhttp3.Request request = original.newBuilder()
                .header(TraceContext.TRACE_ID, TraceContext.getTraceIdOrDefault())
                .header(TraceContext.USER_ID, TraceContext.getUserIdOrDefault())
                .header(TraceContext.SECRET_KEY, securityProperties.getSecretKey())
                .method(original.method(), original.body())
                .build();
        return chain.proceed(request);
    }
}
