package org.source.spring.cache.pubsub;

import lombok.extern.slf4j.Slf4j;
import org.source.spring.cache.configure.ConfigureCacheProperties;
import org.source.spring.redis.pubsub.MessageDelegate;
import org.source.utility.utils.Jsons;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class ConfigureCacheMessageDelegate implements MessageDelegate {
    private final Map<String, ConfigureCacheProperties> configureCacheExpendMap;

    public ConfigureCacheMessageDelegate() {
        this.configureCacheExpendMap = new HashMap<>();
    }

    public ConfigureCacheMessageDelegate(Map<String, ConfigureCacheProperties> configureCacheExpendMap) {
        this.configureCacheExpendMap = configureCacheExpendMap;
    }

    @Override
    public String channelTopic() {
        return PublishTopic.TOPIC_CONFIGURE_CACHE;
    }

    @Override
    public void handleMessage(String message) {
        if (log.isDebugEnabled()) {
            log.debug("ConfigureCache evict jvm via pubsub message:{}", message);
        }
        ConfigureCacheMessage cacheMessage = Jsons.obj(message, ConfigureCacheMessage.class);
        if (Objects.isNull(cacheMessage)) {
            return;
        }
        String cacheName = cacheMessage.getCacheName();
        ConfigureCacheProperties cacheProperties = this.configureCacheExpendMap.get(cacheName);
        String key = cacheMessage.getMessage();
        if (Boolean.TRUE.equals(cacheMessage.getIsMulti())) {
            List<?> ks = Jsons.list(key, cacheProperties.getJvmKeyClass());
            cacheProperties.getJvmCache().invalidateAll(ks);
        } else {
            Object k = key;
            if (!cacheProperties.getJvmKeyClass().equals(String.class)) {
                k = Jsons.obj(key, cacheProperties.getJvmKeyClass());
            }
            cacheProperties.getJvmCache().invalidate(k);
        }
    }
}
