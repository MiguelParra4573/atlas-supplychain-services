package com.atlas.supplychain.auth.domain.shared;

import com.atlas.supplychain.auth.domain.shared.query.SortCriterion;

import java.util.List;

public record PageQuery(int page, int size, List<SortCriterion> sorts) {}
