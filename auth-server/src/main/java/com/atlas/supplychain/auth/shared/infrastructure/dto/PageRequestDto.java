package com.atlas.supplychain.auth.shared.infrastructure.dto;

import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.query.SortCriterion;
import lombok.Data;

import java.util.List;

@Data
public class PageRequestDto {
    private int page = 0;
    private int size = 10;

    public PageQuery toPageQuery(List<SortCriterion> sorts) {
        return new PageQuery(page, size, sorts);
    }
}