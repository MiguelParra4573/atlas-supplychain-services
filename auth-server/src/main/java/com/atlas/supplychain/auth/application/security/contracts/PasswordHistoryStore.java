package com.atlas.supplychain.auth.application.security.contracts;

import java.util.List;

public interface PasswordHistoryStore {
    void save(String idUser, String companyId, String hash);

    List<String> findRecentHashes(String idUser, String companyId, int limit);
}
