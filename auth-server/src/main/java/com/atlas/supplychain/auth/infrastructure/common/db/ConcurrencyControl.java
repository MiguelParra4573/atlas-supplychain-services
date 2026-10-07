package com.atlas.supplychain.auth.infrastructure.common.db;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public interface ConcurrencyControl {

    static final Set<Map.Entry<String, Long>> cache = new HashSet<>();

    public Long findById(String id);

    public void updateTimestamp(String id);

}
