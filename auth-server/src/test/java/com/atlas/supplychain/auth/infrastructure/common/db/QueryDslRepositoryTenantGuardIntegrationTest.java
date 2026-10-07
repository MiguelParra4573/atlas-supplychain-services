package com.atlas.supplychain.auth.infrastructure.common.db;

import com.atlas.supplychain.auth.infrastructure.common.outbox.OutboxEventEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.ModuleEntity;
import com.atlas.supplychain.auth.infrastructure.common.db.query.predicate.PredicateBuilderRegistry;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Spec 1.1 -- forCompany() es el único punto de paso obligado del filtro de tenant: nunca
 * debe devolver una query sin filtrar. ModuleEntity tiene idCompany (Spec 1.0); OutboxEventEntity
 * no tiene idCompany, usado acá solo para probar esa rama.
 */
@SpringBootTest
class QueryDslRepositoryTenantGuardIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PredicateBuilderRegistry predicateRegistry;

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void forCompanyLanzaConIdCompanyAusenteOEnBlanco(String idCompany) {
        var repo = new AbstractQueryDslRepository<ModuleEntity, Long>(ModuleEntity.class, entityManager, predicateRegistry) {};

        assertThatThrownBy(() -> repo.forCompany(null, idCompany))
                .isInstanceOf(TenantScopeRequiredException.class);
    }

    @Test
    void forCompanyLanzaSiLaEntidadNoTieneCampoIdCompany() {
        var repo = new AbstractQueryDslRepository<OutboxEventEntity, String>(OutboxEventEntity.class, entityManager, predicateRegistry) {};

        assertThatThrownBy(() -> repo.forCompany(null, "empresaA"))
                .isInstanceOf(TenantScopeRequiredException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requireCompanyLanzaConIdCompanyAusenteOEnBlanco(String idCompany) {
        var repo = new AbstractQueryDslRepository<ModuleEntity, Long>(ModuleEntity.class, entityManager, predicateRegistry) {};

        assertThatThrownBy(() -> repo.requireCompany(idCompany))
                .isInstanceOf(TenantScopeRequiredException.class);
    }
}
