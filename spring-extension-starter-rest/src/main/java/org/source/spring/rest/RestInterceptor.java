package org.source.spring.rest;

import okhttp3.Interceptor;
import okhttp3.Response;
import org.source.spring.trace.TraceContext;
import org.source.spring.uid.Uids;
import org.source.utility.enums.BaseExceptionEnum;

import java.io.IOException;
import java.util.Objects;

public class RestInterceptor implements Interceptor {
    private final String secretKey;

    public RestInterceptor(String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        okhttp3.Request original = chain.request();
        BaseExceptionEnum.NOT_NULL.nonNull(secretKey,
                "org.source.common.secretKey must be configured");
        okhttp3.Request request = original.newBuilder()
                .header(TraceContext.TRACE_ID, Objects.requireNonNullElse(TraceContext.getTraceId(), Uids.stringId()))
                .header(TraceContext.USER_ID, TraceContext.getUserIdOrDefault())
                .header(TraceContext.SECRET_KEY, secretKey)
                .method(original.method(), original.body())
                .build();
        return chain.proceed(request);
    }
}