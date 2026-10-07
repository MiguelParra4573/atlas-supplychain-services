package com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.security;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.security.PasswordHistory;

import java.util.List;

public interface PasswordHistoryRepository {

    void save(PasswordHistory passwordHistory);

    List<String> findRecentHashes(String idUser, String idCompany, int limit);

}
