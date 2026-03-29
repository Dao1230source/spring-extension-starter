package org.source.spring.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.source.spring.cache.configure.ConfigureCacheProperties;

import java.util.List;

@AllArgsConstructor
@Data
public class ConfigureCachePropertiesCached {
    private List<ConfigureCacheProperties> configureCacheProperties;
}
