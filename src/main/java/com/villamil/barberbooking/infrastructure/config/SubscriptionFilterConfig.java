package com.villamil.barberbooking.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import com.villamil.barberbooking.infrastructure.security.SubscriptionAccessFilter;

@Configuration
@ConditionalOnProperty(name="billing.enforce-subscription", havingValue="true")
public class SubscriptionFilterConfig {
    @Bean FilterRegistrationBean<SubscriptionAccessFilter> subscriptionFilterRegistration(SubscriptionAccessFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
