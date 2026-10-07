package com.atlas.supplychain.auth.application.maintenance.authorization.user;

import com.atlas.supplychain.auth.application.audit.contracts.AuditTrail;
import com.atlas.supplychain.auth.application.audit.contracts.CurrentActor;
import com.atlas.supplychain.auth.application.audit.dto.Actor;
import com.atlas.supplychain.auth.application.audit.dto.AuditEventCommand;
import com.atlas.supplychain.auth.application.messaging.contracts.OutboxRepository;
import com.atlas.supplychain.auth.application.messaging.dto.OutboxEventCommand;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Role;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.UserRepository;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import lombok.RequiredArgsConstructor;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final AuditTrail auditTrail;
    private final CurrentActor currentActor;
    private final OutboxRepository outboxRepository;

    @Override
    public void crearUsuario(User user) {
        // idUser es la PK interna (bigint IDENTITY): repository.crearUsuario la genera y la
        // escribe de vuelta sobre user -- nunca la inventa el cliente ni este código. El login
        // real usa username, generado acá, independiente del email.
        user.setUsername(generarUsername(user.getName(), user.getLastName()));
        repository.crearUsuario(user);
        registrarUsuarioCreado(user);
    }

    /**
     * username = nombre.apellido normalizado (sin tildes/espacios, minúsculas); si ya existe,
     * agrega un sufijo secuencial (2, 3, ...) -- nunca random, para que sea memorizable y no
     * genere bloqueos de cuenta por intentos con un username mal recordado (decisión del
     * usuario 2026-09-02).
     */
    private String generarUsername(String name, String lastName) {
        String base = normalizar(name) + "." + normalizar(lastName);
        String candidato = base;
        int sufijo = 2;
        while (repository.existsByUsername(candidato)) {
            candidato = base + sufijo;
            sufijo++;
        }
        return candidato;
    }

    private String normalizar(String valor) {
        String sinTildes = Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    /**
     * Spec 1.24: publica {@code USUARIO_CREADO} vía Outbox en la misma transacción que el
     * {@code INSERT} de arriba -- alimenta el read model CQRS de {@code ms-reporte} (Spec 4.4
     * revisada), que necesita enterarse de altas de usuario sin llamadas REST síncronas.
     */
    private void registrarUsuarioCreado(User user) {
        outboxRepository.append(new OutboxEventCommand(
                "USER",
                user.getIdUser(),
                "USUARIO_CREADO",
                snapshotUsuario(user)));
    }

    @Override
    public void actualizarUsuario(User user) throws EntityNotFoundException {
        User existente = repository.obtenerUsuarioPorId(user.getIdCompany(), user.getIdUser())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado ID %s ".formatted(user.getIdUser())));

        boolean cambioDeRol = !Objects.equals(idRol(existente.getRole()), idRol(user.getRole()));
        boolean cambioDeEmail = !Objects.equals(existente.getEmail(), user.getEmail());
        boolean cambioDeName = !Objects.equals(existente.getName(), user.getName());
        boolean cambioDeLastName = !Objects.equals(existente.getLastName(), user.getLastName());
        boolean cambioDeState = existente.isState() != user.isState();

        // El DTO de entrada no trae estos campos (no viajan por el borde REST) -- si no se
        // preservan del registro existente, el update de abajo los pisa a null/0 y rompe la
        // constraint NOT NULL de password_hash.
        user.setPasswordHash(existente.getPasswordHash());
        user.setPasswordChangedAt(existente.getPasswordChangedAt());
        user.setFailedAttempts(existente.getFailedAttempts());
        user.setLockUntil(existente.getLockUntil());
        user.setPasswordExpiresAt(existente.getPasswordExpiresAt());

        repository.actualizarUsuario(user);

        if (cambioDeRol) {
            registrarCambioDeRol(user);
        }
        if (cambioDeEmail) {
            registrarCambioDeEmail(user);
        }
        if (cambioDeName || cambioDeLastName || cambioDeRol || cambioDeState || cambioDeEmail) {
            registrarUsuarioActualizado(user);
        }
    }

    /**
     * Spec 1.24: publica {@code USUARIO_ACTUALIZADO} vía Outbox con el snapshot completo
     * post-update (no un delta) cuando cambia {@code name}/{@code lastName}/{@code email}/
     * {@code idRole}/{@code state} -- simplifica al consumidor de {@code ms-reporte} (upsert
     * directo, sin merge de campos parciales). No reemplaza {@code USUARIO_EMAIL_CAMBIADO}
     * (registrado aparte, arriba): cuando el email cambia se publican ambos en la misma
     * transacción.
     */
    private void registrarUsuarioActualizado(User user) {
        outboxRepository.append(new OutboxEventCommand(
                "USER",
                user.getIdUser(),
                "USUARIO_ACTUALIZADO",
                snapshotUsuario(user)));
    }

    /**
     * {@code Map.of(...)} rechaza valores {@code null} con NPE -- {@code name}/{@code lastName}/
     * {@code email} pueden no estar poblados en todo objeto de dominio (p. ej. construcciones
     * parciales en tests, o un futuro estado transitorio), así que se arma con un {@link Map}
     * mutable que sí los tolera. La serialización a JSON de infrastructure (Jackson) representa
     * un valor {@code null} como {@code null}, no como texto vacío.
     */
    private static Map<String, String> snapshotUsuario(User user) {
        Map<String, String> snapshot = new LinkedHashMap<>();
        snapshot.put("idUser", user.getIdUser());
        snapshot.put("idCompany", user.getIdCompany());
        snapshot.put("name", user.getName());
        snapshot.put("lastName", user.getLastName());
        snapshot.put("email", user.getEmail());
        snapshot.put("idRole", String.valueOf(idRol(user.getRole())));
        snapshot.put("state", String.valueOf(user.isState()));
        return snapshot;
    }

    /**
     * Spec 3.4: publica {@code USUARIO_EMAIL_CAMBIADO} vía Outbox en la misma transacción que el
     * {@code UPDATE} de arriba (este método corre dentro de la transacción RW que
     * {@code ApplicationTransactionConfig} aplica por convención de nombre a
     * {@code actualizarUsuario}) -- ms-notificacion sincroniza el email copiado en
     * {@code recipient_group_members} al consumir este evento.
     */
    private void registrarCambioDeEmail(User user) {
        outboxRepository.append(new OutboxEventCommand(
                "USER",
                user.getIdUser(),
                "USUARIO_EMAIL_CAMBIADO",
                Map.of("idUser", user.getIdUser(), "email", user.getEmail())));
    }

    private static Long idRol(Role role) {
        return role == null ? null : role.getIdRol();
    }

    private void registrarCambioDeRol(User user) {
        Actor actor = currentActor.actorActual();
        String companyId = actor != null ? actor.idCompany() : user.getIdCompany();
        String userId = actor != null ? actor.idUser() : null;
        String username = actor != null ? actor.username() : null;
        String rolPrincipal = actor != null ? actor.rolPrincipal() : null;

        auditTrail.registrar(new AuditEventCommand(
                "UPDATE",
                "CAMBIO_ROL_USUARIO",
                "USUARIO",
                user.getIdUser(),
                "ADMINISTRACION",
                "HIGH",
                companyId,
                userId,
                username,
                rolPrincipal,
                "Cambio de rol del usuario " + user.getIdUser()
        ));
    }

    @Override
    public void eliminarUsuario(String idUsuario, String idCompany) throws EntityNotFoundException {
        repository.eliminarUsuario(idCompany, idUsuario);
        registrarUsuarioEliminado(idUsuario, idCompany);
    }

    /**
     * Spec 1.24: publica {@code USUARIO_ELIMINADO} vía Outbox en la misma transacción que el
     * {@code DELETE} de arriba -- {@code UserRepositoryImpl.eliminarUsuario} hace baja física
     * ({@code this.delete(entity)}), así que el consumidor de {@code ms-reporte} necesita
     * enterarse explícitamente para borrar (no desactivar) su copia del read model.
     */
    private void registrarUsuarioEliminado(String idUsuario, String idCompany) {
        outboxRepository.append(new OutboxEventCommand(
                "USER",
                idUsuario,
                "USUARIO_ELIMINADO",
                Map.of("idUser", idUsuario, "idCompany", idCompany)));
    }

    @Override
    public User obtenerUsuarioPorId(String idUsuario, String idCompany) throws EntityNotFoundException {
        return repository.obtenerUsuarioPorId(idCompany, idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado ID %s ".formatted(idUsuario)));
    }

    @Override
    public List<User> listarUsuariosPorEmpresa(String idCompany) {
        return repository.listarUsuariosPorCompany(idCompany);
    }

    @Override
    public PagedResult<User> buscarUsuariosPaginados(List<FilterCriterion> filters, PageQuery pageQuery, String idCompany) {
        return repository.listarUsuariosPaginadosPorFiltros(idCompany, filters, pageQuery);
    }
}
