package com.atlas.supplychain.auth.application.maintenance.authorization.module;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Operation;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.OperationRepository;
import com.atlas.supplychain.auth.domain.maintenance.organization.model.Company;
import com.atlas.supplychain.auth.domain.maintenance.organization.repositories.CompanyRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Decisión de tenant (Diseño §1, "Decisión de tenant"): {@code auth-server} tiene acceso directo a
 * {@code auth.company} en su propia base -- a diferencia de {@code BackfillRunner} en
 * {@code ms-reporte} (Spec 4.4), que necesita una lista de tenants por configuración porque no
 * tiene esa tabla -- así que acá se itera {@link CompanyRepository#listarCompaniesPorState} en
 * vez de introducir un mecanismo de descubrimiento externo. Solo empresas activas ({@code
 * state=true}): una empresa dada de baja no necesita ver operaciones nuevas.
 *
 * <p>Idempotencia: por empresa, upsert de cada operación por {@code (idCompany, resource, tag)} --
 * respaldado por {@code uk_operations_company_resource_tag} (changelog 025). Correr el mismo
 * registro dos veces no duplica nada, solo refresca {@code path}/{@code httpMethod}/{@code
 * permitAll}/{@code state}.</p>
 *
 * <p>Lo que este servicio ya <b>no</b> hace (Spec 1.2): crear o actualizar un {@code Module}. La
 * operación recién registrada no cuelga de ningún módulo, así que no aparece en el menú de nadie
 * hasta que un administrador de la empresa la asigne. Es deliberado -- el sistema no adivina cómo
 * agrupa sus pantallas cada empresa -- y no le quita autoridad: una operación concedida autoriza
 * su endpoint aunque no esté en el menú.</p>
 */
@RequiredArgsConstructor
public class ResourceSelfRegistrationServiceImpl implements ResourceSelfRegistrationService {

    private final OperationRepository operationRepository;
    private final CompanyRepository companyRepository;

    @Override
    public void registrarOperacionesEnTodasLasEmpresas(String resource, List<Operation> operations) {
        if (operations == null || operations.isEmpty()) {
            return;
        }

        List<Company> empresasActivas = companyRepository.listarCompaniesPorState(true);
        for (Company empresa : empresasActivas) {
            for (Operation operacionTemplate : operations) {
                upsertOperacion(empresa.getIdCompany(), resource, operacionTemplate);
            }
        }
    }

    private void upsertOperacion(String idCompany, String resource, Operation template) {
        operationRepository.buscarPorRecursoYTag(idCompany, resource, template.getTag())
                .ifPresentOrElse(
                        existente -> actualizarOperacionExistente(existente, template),
                        () -> crearOperacionNueva(idCompany, resource, template));
    }

    private void actualizarOperacionExistente(Operation existente, Operation template) {
        existente.setPath(template.getPath());
        existente.setHttpMethod(template.getHttpMethod());
        existente.setPermitAll(template.isPermitAll());
        existente.setState(true);
        try {
            operationRepository.actualizarOperacion(existente);
        } catch (EntityNotFoundException ex) {
            // No debería pasar: buscarPorRecursoYTag acaba de encontrarla en la misma transacción.
            throw new IllegalStateException(ex);
        }
    }

    private void crearOperacionNueva(String idCompany, String resource, Operation template) {
        Operation nueva = Operation.builder()
                .idCompany(idCompany)
                .resource(resource)
                .tag(template.getTag())
                .path(template.getPath())
                .httpMethod(template.getHttpMethod())
                .permitAll(template.isPermitAll())
                .state(true)
                .build();
        operationRepository.crearOperacion(nueva);
    }
}
