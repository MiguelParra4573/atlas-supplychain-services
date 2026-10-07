package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.db.security;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.security.PasswordHistory;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.security.PasswordHistoryRepository;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.security.PasswordHistoryEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.security.QPasswordHistoryEntity;
import com.atlas.supplychain.auth.infrastructure.common.db.AbstractQueryDslRepository;
import com.atlas.supplychain.auth.infrastructure.common.db.query.predicate.PredicateBuilderRegistry;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.security.PasswordHistoryMapper;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
@Slf4j
public class PasswordHistoryRepositoryImpl extends AbstractQueryDslRepository<PasswordHistoryEntity, Long> implements PasswordHistoryRepository {

    private final PasswordHistoryMapper mapper;

    public PasswordHistoryRepositoryImpl(
            EntityManager entityManager,
            PredicateBuilderRegistry predicateRegistry,
            PasswordHistoryMapper mapper) {
        super(PasswordHistoryEntity.class, entityManager, predicateRegistry);
        this.mapper = mapper;
    }

    @Override
    public void save(PasswordHistory passwordHistory) {
        this.save(mapper.domainToEntity(passwordHistory));
    }

    @Override
    public List<String> findRecentHashes(String idUser, String idCompany, int limit) {
        QPasswordHistoryEntity ph = QPasswordHistoryEntity.passwordHistoryEntity;
        return getQueryFactory()
                .select(ph.passwordHash)
                .from(ph)
                .where(
                        ph.idUser.eq(Long.parseLong(idUser)),
                        ph.idCompany.eq(idCompany)
                )
                .orderBy(ph.createdAt.desc())
                .limit(limit)
                .fetch();
    }
}
