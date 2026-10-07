package com.atlas.supplychain.auth.infrastructure.common.db.query.predicate;

import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.PathBuilder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;


@Component
public class BooleanPredicateBuilder implements PredicateBuilder {

    @Override
    public boolean supports(Class<?> type) {
        return type.equals(Boolean.class) || type.equals(boolean.class);
    }

        @Override
    public BooleanExpression build(PathBuilder<?> root, FilterCriterion fc) {
        BooleanPath path = root.getBoolean(fc.field());
        String operator = normalizeOperator(fc.operator());
        List<Boolean> values = splitValues(fc.value());

        return switch (operator) {
            case "="     -> path.eq(first(values));
            case "!="    -> path.ne(first(values));
            case "in"    -> path.in(values);
            case "not in" -> path.notIn(values);
            default -> throw new IllegalArgumentException(
                    "Operador booleano no soportado: %s".formatted(fc.operator())
            );
        };
    }

    private String normalizeOperator(String op) {
        return op.trim().toLowerCase();
    }

    private List<Boolean> splitValues(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(Boolean::valueOf)
                .toList();
    }

    private Boolean first(List<Boolean> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("El filtro booleano requiere al menos un valor");
        }
        return values.get(0);
    }
}