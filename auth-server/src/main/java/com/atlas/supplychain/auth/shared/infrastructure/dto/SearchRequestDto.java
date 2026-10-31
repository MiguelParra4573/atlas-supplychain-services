package com.atlas.supplychain.auth.shared.infrastructure.dto;

import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import com.atlas.supplychain.auth.domain.shared.query.SortCriterion;
import java.util.List;
import lombok.Data;

@Data
public class SearchRequestDto {
    private List<FilterCriterion> filters;
    private List<SortCriterion> sorts;
}
