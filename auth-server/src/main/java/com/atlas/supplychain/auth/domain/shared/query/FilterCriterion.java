package com.atlas.supplychain.auth.domain.shared.query;

import lombok.Builder;

@Builder
public record FilterCriterion(
        String field,
        String operator,
        String value,
        String type
) {}
