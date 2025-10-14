package org.source.spring.redis.config;

import org.source.utility.utils.Jsons;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author zengfugen
 */
public class TypeRedisTemplate<K, V> extends RedisTemplate<K, V> {

    private TypeRedisTemplate(RedisConnectionFactory factory) {
        this.setConnectionFactory(factory);
        setKeySerializer(RedisSerializer.string());
        setValueSerializer(RedisSerializer.string());
        setHashKeySerializer(RedisSerializer.string());
        setHashValueSerializer(RedisSerializer.string());
    }

    public static <T> Jackson2JsonRedisSerializer<T> getSerializer(Class<?>... keyType) {
        return new Jackson2JsonRedisSerializer<>(Objects.requireNonNull(Jsons.getJavaType(keyType)));
    }

    public static <K, V> Builder<K, V> builder(RedisConnectionFactory factory) {
        return new Builder<>(factory);
    }

    public static class Builder<K, V> {
        private final TypeRedisTemplate<K, V> template;

        public Builder(RedisConnectionFactory factory) {
            this.template = new TypeRedisTemplate<>(factory);
        }

        public Builder<K, V> initSerializer(Consumer<TypeRedisTemplate<K, V>> initSerializer) {
            initSerializer.accept(this.template);
            return this;
        }

        public TypeRedisTemplate<K, V> build() {
            // 使设置生效
            template.afterPropertiesSet();
            return template;
        }
    }
}
