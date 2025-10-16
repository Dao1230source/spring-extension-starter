package org.source.spring.rest;

import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.Nullable;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.exceptions.BaseException;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Objects;

@Slf4j
public class CallAdapterFactory extends CallAdapter.Factory {
    @Nullable
    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        return new UnwrapCallAdapter<>(returnType);
    }

    static final class UnwrapCallAdapter<R> implements CallAdapter<R, R> {
        private final Type returnType;

        UnwrapCallAdapter(Type returnType) {
            this.returnType = returnType;
        }

        @Override
        public Type responseType() {
            return returnType;
        }

        @Override
        public R adapt(Call<R> call) {
            try {
                return doCall(call);
            } catch (Exception e) {
                log.error("execute retrofit exception", e);
                throw BaseException.except(e, () -> BaseExceptionEnum.REQUEST_EXECUTE_EXCEPTION.except(e));
            }
        }

        public static <R> R doCall(Call<R> call) throws IOException {
            if (log.isDebugEnabled()) {
                log.debug("url:{}, ", call.request().url());
            }
            Response<R> response = call.execute();
            if (response.isSuccessful()) {
                return Objects.requireNonNullElse(response.body(), null);
            } else {
                try (ResponseBody errorBody = response.errorBody()) {
                    throw BaseExceptionEnum.REQUEST_DO_CALL_EXCEPTION.except(Objects.nonNull(errorBody) ? errorBody.string() : null);
                }
            }
        }
    }
}
