package com.atlas.supplychain.auth.infrastructure.common.db.query.predicate;

import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;

public interface PredicateBuilder {

    boolean supports(Class<?> type);

    BooleanExpression build(
            PathBuilder<?> root,
            FilterCriterion fc
    );
}