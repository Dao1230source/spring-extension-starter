package org.source.spring.cache.strategy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class PartialCacheResult {
    private Object result;
    private List<Object> cachedKeys;
}
