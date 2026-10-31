package com.atlas.supplychain.auth.infrastructure.common.db;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ConcurrencyControlSetImpl implements ConcurrencyControl {

    static final Set<Map.Entry<String, Long>> cache = new HashSet<>();

    @Override
    public Long findById(String id) {

        for (Map.Entry<String, Long> entry : cache) {
            if (entry.getKey().equals(id)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public void updateTimestamp(String id) {
        cache.add(new AbstractMap.SimpleEntry<>(id, System.currentTimeMillis()));
    }

    // Posteriormente llamamos a un redis

}
