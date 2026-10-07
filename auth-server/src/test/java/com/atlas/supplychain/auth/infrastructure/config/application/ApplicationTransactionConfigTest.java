package com.atlas.supplychain.auth.infrastructure.config.application;

import com.atlas.supplychain.auth.application.maintenance.authorization.grantedpermission.GrantedPermissionServiceImpl;
import com.atlas.supplychain.auth.application.maintenance.authorization.module.ModuleServiceImpl;
import com.atlas.supplychain.auth.application.maintenance.authorization.operation.OperationServiceImpl;
import com.atlas.supplychain.auth.application.maintenance.authorization.role.RoleServiceImpl;
import com.atlas.supplychain.auth.application.maintenance.authorization.user.UserServiceImpl;
import com.atlas.supplychain.auth.application.security.AuthenticationService;
import com.atlas.supplychain.auth.application.security.PasswordResetServiceImpl;
import com.atlas.supplychain.auth.application.security.PasswordServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Blinda por reflexión el contrato implícito por convención de nombre que reemplaza a
 * {@code @Transactional} en application/maintenance/**.*ServiceImpl (hallazgo de solid-java,
 * spec 1.9): un método nuevo mal nombrado en cualquiera de estas 9 clases debe seguir cayendo en
 * la rama correcta (readOnly u RW), y las clases de application/security/** deben quedar fuera
 * del pointcut, tal como estaban antes de esta spec.
 */
class ApplicationTransactionConfigTest {

    private static final List<Class<?>> MAINTENANCE_SERVICE_IMPLS = List.of(
            GrantedPermissionServiceImpl.class,
            ModuleServiceImpl.class,
            OperationServiceImpl.class,
            RoleServiceImpl.class,
            UserServiceImpl.class
    );

    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final Advisor advisor =
            new ApplicationTransactionConfig().maintenanceServiceTransactionAdvisor(transactionManager);

    private static Stream<Class<?>> maintenanceServiceImpls() {
        return MAINTENANCE_SERVICE_IMPLS.stream();
    }

    private static Stream<Class<?>> clasesFueraDelPointcut() {
        return Stream.of(
                AuthenticationService.class,
                PasswordResetServiceImpl.class,
                PasswordServiceImpl.class
        );
    }

    @Test
    void esUnDefaultPointcutAdvisorConTransactionInterceptor() {
        assertThat(advisor).isInstanceOf(DefaultPointcutAdvisor.class);
        assertThat(advisor.getAdvice()).isInstanceOf(TransactionInterceptor.class);
    }

    @ParameterizedTest
    @MethodSource("maintenanceServiceImpls")
    void elPointcutMatcheaTodosLosServiceImplDeMaintenance(Class<?> serviceImpl) throws Exception {
        Pointcut pointcut = ((DefaultPointcutAdvisor) advisor).getPointcut();
        Method cualquierMetodo = primerMetodoPublicoDeclarado(serviceImpl);

        boolean matches = pointcut.getMethodMatcher().matches(cualquierMetodo, serviceImpl);

        assertThat(matches)
                .as("el pointcut debe matchear %s (application.maintenance.**.*ServiceImpl)", serviceImpl.getName())
                .isTrue();
    }

    @ParameterizedTest
    @MethodSource("clasesFueraDelPointcut")
    void elPointcutNoMatcheaClasesFueraDeMaintenanceServiceImpl(Class<?> clase) throws Exception {
        Pointcut pointcut = ((DefaultPointcutAdvisor) advisor).getPointcut();
        Method cualquierMetodo = primerMetodoPublicoDeclarado(clase);

        boolean matches = pointcut.getMethodMatcher().matches(cualquierMetodo, clase);

        assertThat(matches)
                .as("el pointcut NO debe matchear %s -- nunca tuvo @Transactional y no se le debe añadir aquí", clase.getName())
                .isFalse();
    }

    @ParameterizedTest
    @MethodSource("maintenanceServiceImpls")
    void cadaMetodoPublicoResuelveElAtributoDeTransaccionSegunSuPrefijo(Class<?> serviceImpl) {
        TransactionAttributeSource attributeSource = attributeSourceDelAdvisor();

        for (Method metodo : metodosPublicosDeclarados(serviceImpl)) {
            TransactionAttribute atributo = attributeSource.getTransactionAttribute(metodo, serviceImpl);
            boolean esperaReadOnly = esperaSemanticaReadOnly(metodo.getName());

            assertThat(atributo)
                    .as("%s.%s debe tener un TransactionAttribute resuelto", serviceImpl.getSimpleName(), metodo.getName())
                    .isNotNull();
            assertThat(atributo.isReadOnly())
                    .as("%s.%s (prefijo %s) esperaba readOnly=%s", serviceImpl.getSimpleName(), metodo.getName(),
                            metodo.getName(), esperaReadOnly)
                    .isEqualTo(esperaReadOnly);
        }
    }

    private static boolean esperaSemanticaReadOnly(String nombreMetodo) {
        return nombreMetodo.startsWith("obtener")
                || nombreMetodo.startsWith("listar")
                || nombreMetodo.startsWith("buscar");
    }

    private TransactionAttributeSource attributeSourceDelAdvisor() {
        TransactionInterceptor interceptor = (TransactionInterceptor) advisor.getAdvice();
        TransactionAttributeSource attributeSource =
                ((TransactionAspectSupport) interceptor).getTransactionAttributeSource();
        assertThat(attributeSource).isNotNull();
        return attributeSource;
    }

    private static Method primerMetodoPublicoDeclarado(Class<?> clase) {
        return metodosPublicosDeclarados(clase).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(clase.getName() + " no declara métodos públicos propios"));
    }

    private static List<Method> metodosPublicosDeclarados(Class<?> clase) {
        return Arrays.stream(clase.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .filter(m -> !m.isSynthetic() && !m.isBridge())
                .toList();
    }
}
