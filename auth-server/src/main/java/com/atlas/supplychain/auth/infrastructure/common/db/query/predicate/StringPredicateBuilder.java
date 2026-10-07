package com.atlas.supplychain.auth.infrastructure.common.db.query.predicate;

import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.StringPath;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class StringPredicateBuilder implements PredicateBuilder {

    @Override
    public boolean supports(Class<?> type) {
        return type.equals(String.class);
    }

    @Override
    public BooleanExpression build(PathBuilder<?> root, FilterCriterion fc) {
        StringPath path = root.getString(fc.field());
        String operator = normalizeOperator(fc.operator());
        List<String> values = splitValues(fc.value());

        return switch (operator) {
            case "="     -> path.eq(first(values));
            case "!="    -> path.ne(first(values));
            case "like"  -> path.containsIgnoreCase(first(values));
            case "start" -> path.startsWithIgnoreCase(first(values));
            case "end"   -> path.endsWithIgnoreCase(first(values));
            case "in"    -> path.in(values);
            case "not in" -> path.notIn(values);
            default -> throw new IllegalArgumentException(
                    "Operador no soportado para tipo String: %s".formatted(fc.operator())
            );
        };
    }

    private String normalizeOperator(String op) {
        return op.trim().toLowerCase();
    }

    private List<String> splitValues(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .toList();
    }

    private String first(List<String> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("El filtro de tipo String requiere un valor");
        }
        return values.get(0);
    }
}
