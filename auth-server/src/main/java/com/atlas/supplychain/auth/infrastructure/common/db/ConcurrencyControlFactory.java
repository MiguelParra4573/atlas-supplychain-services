package com.atlas.supplychain.auth.infrastructure.common.db;

public class ConcurrencyControlFactory {
    ConcurrencyControl getInstance() {
        return new ConcurrencyControlSetImpl();
    }
}
