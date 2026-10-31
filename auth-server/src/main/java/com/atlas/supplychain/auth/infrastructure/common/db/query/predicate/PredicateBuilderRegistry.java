package com.atlas.supplychain.auth.infrastructure.common.db.query.predicate;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PredicateBuilderRegistry {

    private final List<PredicateBuilder> builders;

    public PredicateBuilder getBuilder(Class<?> type) {
        return builders.stream()
                .filter(b -> b.supports(type))
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "No se encontró PredicateBuilder para el tipo: " + type));
    }
}
