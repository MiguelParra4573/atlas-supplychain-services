package com.atlas.supplychain.auth.infrastructure.common.db.query.predicate;

import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DatePath;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.PathBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class DateTimePredicateBuilder implements PredicateBuilder {

    @Override
    public boolean supports(Class<?> type) {
        return type.equals(LocalDate.class) || type.equals(LocalDateTime.class);
    }

    @Override
    public BooleanExpression build(PathBuilder<?> root, FilterCriterion fc) {

        boolean isMultiValue = fc.value().contains(",");
        List<String> values = Arrays.asList(fc.value().split(","));

        Class<?> type = resolveTemporalType(fc.type()); // leer tipo desde frontend/API

        if (type.equals(LocalDate.class)) {
            DatePath<LocalDate> path = root.getDate(fc.field(), LocalDate.class);
            return buildLocalDatePredicate(path, fc.operator(), values, isMultiValue);
        }

        if (type.equals(LocalDateTime.class)) {
            DateTimePath<LocalDateTime> path = root.getDateTime(fc.field(), LocalDateTime.class);
            return buildLocalDateTimePredicate(path, fc.operator(), values, isMultiValue);
        }

        throw new IllegalArgumentException("Tipo temporal no soportado: " + type.getSimpleName());
    }


    private BooleanExpression buildLocalDatePredicate(
            DatePath<LocalDate> path, String operator, List<String> values, boolean multiValue) {

        LocalDate v1 = LocalDate.parse(values.get(0));

        return switch (operator.toLowerCase()) {
            case "=" -> path.eq(v1);
            case "!=" -> path.ne(v1);
            case ">" -> path.gt(v1);
            case "<" -> path.lt(v1);
            case ">=" -> path.goe(v1);
            case "<=" -> path.loe(v1);

            case "between" -> {
                if (!multiValue || values.size() < 2)
                    throw new IllegalArgumentException("BETWEEN requiere 2 valores para LocalDate");
                LocalDate v2 = LocalDate.parse(values.get(1));
                yield path.between(v1, v2);
            }

            default -> throw new IllegalArgumentException("Operador de fecha no soportado: " + operator);
        };
    }


    private BooleanExpression buildLocalDateTimePredicate(
            DateTimePath<LocalDateTime> path, String operator, List<String> values, boolean multiValue) {

        LocalDateTime v1 = LocalDateTime.parse(values.get(0));

        return switch (operator.toLowerCase()) {
            case "=" -> path.eq(v1);
            case "!=" -> path.ne(v1);
            case ">" -> path.gt(v1);
            case "<" -> path.lt(v1);
            case ">=" -> path.goe(v1);
            case "<=" -> path.loe(v1);

            case "between" -> {
                if (!multiValue || values.size() < 2)
                    throw new IllegalArgumentException("BETWEEN requiere 2 valores para LocalDateTime");
                LocalDateTime v2 = LocalDateTime.parse(values.get(1));
                yield path.between(v1, v2);
            }

            default -> throw new IllegalArgumentException("Operador de fecha y hora no soportado: " + operator);
        };
    }


    private Class<?> resolveTemporalType(String type) {
        return switch (type.toLowerCase()) {
            case "date" -> LocalDate.class;
            case "datetime", "timestamp" -> LocalDateTime.class;
            default -> throw new IllegalArgumentException("Tipo de fecha desconocido: " + type);
        };
    }
}