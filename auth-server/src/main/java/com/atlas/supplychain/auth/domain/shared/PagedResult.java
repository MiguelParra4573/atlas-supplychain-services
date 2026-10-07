package com.atlas.supplychain.auth.domain.shared;

import java.util.List;

public record PagedResult<T>(List<T> content, int page, int size, long totalElements) {}
