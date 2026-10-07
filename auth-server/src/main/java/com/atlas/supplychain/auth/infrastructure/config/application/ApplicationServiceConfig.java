package com.atlas.supplychain.auth.infrastructure.config.application;

import com.atlas.supplychain.auth.application.audit.contracts.AuditTrail;
import com.atlas.supplychain.auth.application.audit.contracts.CurrentActor;
import com.atlas.supplychain.auth.application.maintenance.organization.agency.AgencyServiceImpl;
import com.atlas.supplychain.auth.application.maintenance.organization.company.CompanyServiceImpl;
import com.atlas.supplychain.auth.application.maintenance.authorization.PermisosVigentesDelUsuario;
import com.atlas.supplychain.auth.application.maintenance.authorization.PermisosVigentesDelUsuarioImpl;
import com.atlas.supplychain.auth.application.maintenance.authorization.businessprocess.BusinessProcessServiceImpl;
import com.atlas.supplychain.auth.application.maintenance.authorization.grantedpermission.GrantedPermissionServiceImpl;
import com.atlas.supplychain.auth.application.maintenance.authorization.menu.MenuServiceImpl;
import com.atlas.supplychain.auth.application.maintenance.authorization.menucategory.MenuCategoryServiceImpl;
import com.atlas.supplychain.auth.application.maintenance.authorization.module.ModuleServiceImpl;
import com.atlas.supplychain.auth.application.maintenance.authorization.module.ResourceSelfRegistrationServiceImpl;
import com.atlas.supplychain.auth.application.maintenance.authorization.operation.OperationServiceImpl;
import com.atlas.supplychain.auth.application.maintenance.authorization.processstage.ProcessStageServiceImpl;
import com.atlas.supplychain.auth.application.maintenance.authorization.screengroup.ScreenGroupServiceImpl;
import com.atlas.supplychain.auth.application.maintenance.authorization.role.RoleServiceImpl;
import com.atlas.supplychain.auth.application.maintenance.authorization.user.UserServiceImpl;
import com.atlas.supplychain.auth.application.messaging.contracts.OutboxRepository;
import com.atlas.supplychain.auth.application.security.AuthenticationService;
import com.atlas.supplychain.auth.application.security.PasswordResetServiceImpl;
import com.atlas.supplychain.auth.application.security.PasswordServiceImpl;
import com.atlas.supplychain.auth.application.security.contracts.AccessTokenGenerator;
import com.atlas.supplychain.auth.application.security.contracts.Authenticator;
import com.atlas.supplychain.auth.application.security.contracts.NotificationSender;
import com.atlas.supplychain.auth.application.security.contracts.PasswordHasher;
import com.atlas.supplychain.auth.application.security.contracts.PasswordHistoryStore;
import com.atlas.supplychain.auth.application.security.contracts.PasswordPolicy;
import com.atlas.supplychain.auth.application.security.contracts.PasswordResetTokenStore;
import com.atlas.supplychain.auth.application.security.contracts.RefreshSessionManager;
import com.atlas.supplychain.auth.application.security.contracts.UserIdentityProvider;
import com.atlas.supplychain.auth.application.security.contracts.UserStore;
import com.atlas.supplychain.auth.domain.maintenance.organization.repositories.AgencyRepository;
import com.atlas.supplychain.auth.domain.maintenance.organization.repositories.CompanyRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.GrantedPermissionRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.MenuCategoryRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ModuleOperationRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.BusinessProcessRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ModuleRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.OperationRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ProcessStageRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.RoleRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ScreenGroupRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bean-wiring de la capa application: los *ServiceImpl no pueden llevar @Service/@Component
 * (Checkstyle ImportControl, Regla A -- spec 1.9), así que el registro como bean vive aquí,
 * en infrastructure, donde sí está permitido depender de Spring.
 */
@Configuration
public class ApplicationServiceConfig {

    @Bean
    public CompanyServiceImpl companyService(CompanyRepository repository, AgencyRepository agencyRepository,
                                              UserRepository userRepository, RoleRepository roleRepository) {
        return new CompanyServiceImpl(repository, agencyRepository, userRepository, roleRepository);
    }

    @Bean
    public AgencyServiceImpl agencyService(AgencyRepository repository, CompanyRepository companyRepository) {
        return new AgencyServiceImpl(repository, companyRepository);
    }

    @Bean
    public PermisosVigentesDelUsuarioImpl permisosVigentesDelUsuario(UserRepository userRepository) {
        return new PermisosVigentesDelUsuarioImpl(userRepository);
    }

    @Bean
    public GrantedPermissionServiceImpl grantedPermissionService(GrantedPermissionRepository repository,
                                                                   RoleRepository roleRepository,
                                                                   OperationRepository operationRepository,
                                                                   PermisosVigentesDelUsuario permisosVigentesDelUsuario,
                                                                   AuditTrail auditTrail,
                                                                   CurrentActor currentActor) {
        return new GrantedPermissionServiceImpl(repository, roleRepository, operationRepository,
                permisosVigentesDelUsuario, auditTrail, currentActor);
    }

    @Bean
    public MenuServiceImpl menuService(PermisosVigentesDelUsuario permisosVigentesDelUsuario,
                                        ModuleOperationRepository moduleOperationRepository,
                                        ProcessStageRepository processStageRepository,
                                        BusinessProcessRepository businessProcessRepository,
                                        MenuCategoryRepository menuCategoryRepository,
                                        ScreenGroupRepository screenGroupRepository) {
        return new MenuServiceImpl(permisosVigentesDelUsuario, moduleOperationRepository,
                processStageRepository, businessProcessRepository, menuCategoryRepository, screenGroupRepository);
    }

    @Bean
    public ModuleServiceImpl moduleService(ModuleRepository repository,
                                            ModuleOperationRepository moduleOperationRepository) {
        return new ModuleServiceImpl(repository, moduleOperationRepository);
    }

    @Bean
    public MenuCategoryServiceImpl menuCategoryService(MenuCategoryRepository repository) {
        return new MenuCategoryServiceImpl(repository);
    }

    @Bean
    public ScreenGroupServiceImpl screenGroupService(ScreenGroupRepository repository, ModuleRepository moduleRepository) {
        return new ScreenGroupServiceImpl(repository, moduleRepository);
    }

    @Bean
    public BusinessProcessServiceImpl businessProcessService(BusinessProcessRepository repository,
                                                              AuditTrail auditTrail,
                                                              CurrentActor currentActor) {
        return new BusinessProcessServiceImpl(repository, auditTrail, currentActor);
    }

    @Bean
    public ProcessStageServiceImpl processStageService(ProcessStageRepository repository) {
        return new ProcessStageServiceImpl(repository);
    }

    @Bean
    public ResourceSelfRegistrationServiceImpl resourceSelfRegistrationService(
            OperationRepository operationRepository, CompanyRepository companyRepository) {
        return new ResourceSelfRegistrationServiceImpl(operationRepository, companyRepository);
    }

    @Bean
    public OperationServiceImpl operationService(OperationRepository repository,
                                                  GrantedPermissionRepository grantedPermissionRepository,
                                                  ModuleOperationRepository moduleOperationRepository,
                                                  ModuleRepository moduleRepository) {
        return new OperationServiceImpl(repository, grantedPermissionRepository, moduleOperationRepository,
                moduleRepository);
    }

    @Bean
    public RoleServiceImpl roleService(RoleRepository repository, GrantedPermissionRepository grantedPermissionRepository,
                                        UserRepository userRepository, AuditTrail auditTrail, CurrentActor currentActor,
                                        OutboxRepository outboxRepository) {
        return new RoleServiceImpl(repository, grantedPermissionRepository, userRepository, auditTrail, currentActor,
                outboxRepository);
    }

    @Bean
    public UserServiceImpl userService(UserRepository repository, AuditTrail auditTrail, CurrentActor currentActor,
                                        OutboxRepository outboxRepository) {
        return new UserServiceImpl(repository, auditTrail, currentActor, outboxRepository);
    }

    @Bean
    public AuthenticationService authenticationService(Authenticator authenticator,
                                                         AccessTokenGenerator accessTokenGenerator,
                                                         RefreshSessionManager refreshSessionManager,
                                                         UserIdentityProvider userIdentityProvider,
                                                         AuditTrail auditTrail) {
        return new AuthenticationService(authenticator, accessTokenGenerator, refreshSessionManager, userIdentityProvider, auditTrail);
    }

    @Bean
    public PasswordServiceImpl passwordService(UserStore userStore, PasswordHasher passwordHasher,
                                                PasswordPolicy passwordPolicy, PasswordHistoryStore passwordHistoryStore,
                                                RefreshSessionManager refreshSessionManager, AuditTrail auditTrail) {
        return new PasswordServiceImpl(userStore, passwordHasher, passwordPolicy, passwordHistoryStore,
                refreshSessionManager, auditTrail);
    }

    @Bean
    public PasswordResetServiceImpl passwordResetService(UserStore userStore, PasswordResetTokenStore tokenStore,
                                                           PasswordHasher passwordHasher, PasswordPolicy passwordPolicy,
                                                           PasswordHistoryStore passwordHistoryStore,
                                                           RefreshSessionManager refreshSessionManager,
                                                           NotificationSender notificationSender) {
        return new PasswordResetServiceImpl(userStore, tokenStore, passwordHasher, passwordPolicy,
                passwordHistoryStore, refreshSessionManager, notificationSender);
    }
}
