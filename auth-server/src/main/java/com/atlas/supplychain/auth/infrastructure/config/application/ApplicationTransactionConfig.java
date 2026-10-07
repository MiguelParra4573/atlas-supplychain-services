package com.atlas.supplychain.auth.infrastructure.config.application;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Los *ServiceImpl de application/maintenance/** ya no pueden llevar @Transactional (Checkstyle
 * ImportControl, Regla A -- spec 1.9). Antes tenían @Transactional a nivel de clase (RW) y
 * @Transactional(readOnly = true) en los métodos obtener*, listar*, buscar*; este advisor
 * reproduce exactamente esa semántica por convención de nombre, sin anotaciones en application.
 * Solo cubre application.maintenance.**.*ServiceImpl -- security/** nunca tuvo @Transactional
 * y no se le añade aquí para no cambiar su comportamiento.
 */
@Configuration
@EnableTransactionManagement
public class ApplicationTransactionConfig {

    @Bean
    public Advisor maintenanceServiceTransactionAdvisor(PlatformTransactionManager transactionManager) {
        NameMatchTransactionAttributeSource attributeSource = new NameMatchTransactionAttributeSource();

        RuleBasedTransactionAttribute readOnly = new RuleBasedTransactionAttribute();
        readOnly.setReadOnly(true);

        Map<String, TransactionAttribute> attributes = Map.of(
                "obtener*", readOnly,
                "listar*", readOnly,
                "buscar*", readOnly,
                "*", new DefaultTransactionAttribute()
        );
        attributeSource.setNameMap(attributes);

        TransactionInterceptor interceptor = new TransactionInterceptor(transactionManager, attributeSource);
        return new DefaultPointcutAdvisor(new MaintenanceServiceImplPointcut(), interceptor);
    }

    private static final class MaintenanceServiceImplPointcut extends StaticMethodMatcherPointcut {
        @Override
        public boolean matches(Method method, Class<?> targetClass) {
            String name = targetClass.getName();
            return name.startsWith("com.atlas.supplychain.auth.application.maintenance.") && name.endsWith("ServiceImpl");
        }
    }
}
