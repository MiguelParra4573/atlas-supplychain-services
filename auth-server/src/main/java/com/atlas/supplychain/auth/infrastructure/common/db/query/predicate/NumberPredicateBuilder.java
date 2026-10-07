package com.atlas.supplychain.auth.infrastructure.common.db.query.predicate;


import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.PathBuilder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public class NumberPredicateBuilder implements PredicateBuilder {

    @Override
    public boolean supports(Class<?> type) {
        return Number.class.isAssignableFrom(type);
    }

    @Override
    public BooleanExpression build(PathBuilder<?> root, FilterCriterion fc) {

        Class<?> type = getFieldType(root.getType(), fc.field());
        List<String> rawValues = Arrays.asList(fc.value().split(","));

        return switch (type.getSimpleName()) {
            case "Integer" -> {
                List<Integer> values = rawValues.stream()
                        .map(Integer::parseInt)
                        .toList();
                NumberPath<Integer> path = root.getNumber(fc.field(), Integer.class);
                yield getOperatorPredicate(path, fc.operator(), values);
            }
            case "Long" -> {
                List<Long> values = rawValues.stream()
                        .map(Long::valueOf)
                        .toList();
                NumberPath<Long> path = root.getNumber(fc.field(), Long.class);
                yield getOperatorPredicate(path, fc.operator(), values);
            }
            case "BigDecimal" -> {
                List<BigDecimal> values = rawValues.stream()
                        .map(BigDecimal::new)
                        .toList();
                NumberPath<BigDecimal> path = root.getNumber(fc.field(), BigDecimal.class);
                yield getOperatorPredicate(path, fc.operator(), values);
            }
            case "Double" -> {
                List<Double> values = rawValues.stream()
                        .map(Double::valueOf)
                        .toList();
                NumberPath<Double> path = root.getNumber(fc.field(), Double.class);
                yield getOperatorPredicate(path, fc.operator(), values);
            }

            default -> throw new IllegalArgumentException(
                    "Tipo numérico no soportado: %s".formatted(type.getSimpleName())
            );
        };
    }

    private Class<?> getFieldType(Class<?> typeRoot, String field) {
        try {
            return typeRoot.getDeclaredField(field).getType();
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Campo no encontrado '" + field + "'", e);
        }
    }

    protected <N extends Number & Comparable<N>> BooleanExpression getOperatorPredicate(
            NumberPath<N> path,
            String operator,
            List<N> values
    ) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("El operador %s requiere al menos 1 valor.".formatted(operator));
        }

        N first = values.get(0);

        return switch (operator.toLowerCase()) {
            case "=" -> path.eq(first);
            case "!=" -> path.ne(first);
            case ">" -> path.gt(first);
            case "<" -> path.lt(first);
            case ">=" -> path.goe(first);
            case "<=" -> path.loe(first);

            case "in" -> path.in(values);
            case "not in" -> path.notIn(values);

            case "between" -> {
                if (values.size() < 2) {
                    throw new IllegalArgumentException("El operador BETWEEN requiere al menos 2 valores.");
                }
                yield path.between(values.get(0), values.get(1));
            }

            default -> throw new IllegalArgumentException("Operador numérico no soportado: %s".formatted(operator));
        };
    }

}
