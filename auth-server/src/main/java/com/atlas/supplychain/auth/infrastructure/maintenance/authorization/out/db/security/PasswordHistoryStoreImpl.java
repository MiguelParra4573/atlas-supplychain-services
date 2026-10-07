package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.db.security;

import com.atlas.supplychain.auth.application.security.contracts.PasswordHistoryStore;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.security.PasswordHistory;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.security.PasswordHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PasswordHistoryStoreImpl implements PasswordHistoryStore {

    private final PasswordHistoryRepository repository;

    @Override
    @Transactional
    public void save(String idUser, String companyId, String hash) {
        repository.save(PasswordHistory.builder()
                .idUser(idUser)
                .idCompany(companyId)
                .passwordHash(hash)
                .createdAt(Instant.now())
                .build());
    }

    @Override
    public List<String> findRecentHashes(String idUser, String companyId, int limit) {
        return repository.findRecentHashes(idUser, companyId, limit);
    }
}
