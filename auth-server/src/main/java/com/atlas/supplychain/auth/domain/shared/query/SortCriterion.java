package com.atlas.supplychain.auth.domain.shared.query;

import lombok.Builder;

@Builder
public record SortCriterion(
        String field,     // nombre del campo en la Entity
        boolean ascending // true = ASC, false = DESC
) {}
